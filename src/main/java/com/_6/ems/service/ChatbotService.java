package com._6.ems.service;

import com._6.ems.dto.response.ChatbotResponse;
import com._6.ems.dto.response.IntentClassification;
import com._6.ems.dto.response.RoomAvailability;
import com._6.ems.entity.*;
import com._6.ems.enums.TaskStatus;
import com._6.ems.repository.*;
import com._6.ems.utils.SecurityUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatbotService {
    private final ChatClient chatClient;
    private final PersonnelRepository personnelRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final TaskRepository taskRepository;
    private final ObjectMapper objectMapper;
    private final MeetingBookingRepository meetingBookingRepository;
    private final MeetingRoomRepository meetingRoomRepository;
    private final ChatMessageService chatMessageService;
    private final JdbcChatMemoryRepository jdbcChatMemoryRepository;

    public ChatbotService(
            JdbcChatMemoryRepository jdbcChatMemoryRepository,
            ChatClient.Builder builder,
            PersonnelRepository personnelRepository,
            EmployeeRepository employeeRepository,
            AttendanceRepository attendanceRepository,
            TaskRepository taskRepository,
            ObjectMapper objectMapper,
            MeetingRoomRepository meetingRoomRepository,
            MeetingBookingRepository meetingBookingRepository,
            ChatMessageService chatMessageService
    ) {
        this.jdbcChatMemoryRepository = jdbcChatMemoryRepository;

        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(jdbcChatMemoryRepository)
                .maxMessages(10)
                .build();

        chatClient = builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        this.personnelRepository = personnelRepository;
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.taskRepository = taskRepository;
        this.objectMapper = objectMapper;
        this.meetingRoomRepository = meetingRoomRepository;
        this.meetingBookingRepository = meetingBookingRepository;
        this.chatMessageService = chatMessageService;
    }

    public ChatbotResponse processQuestion(String personnelCode, String question) {
        try {
            Personnel personnel = personnelRepository.findById(personnelCode)
                    .orElseThrow(() -> new RuntimeException("Personnel not found"));

            chatMessageService.saveUserMessage(personnelCode, question);

            Employee employee = employeeRepository.findById(personnelCode).orElse(null);

            IntentClassification intent = classifyIntent(question);

            String contextData = fetchContextData(personnelCode, intent, personnel, employee);

            String aiResponse = generateResponse(question, contextData, intent);

            chatMessageService.saveAssistantMessage(personnelCode, aiResponse);

            return ChatbotResponse.builder()
                    .answer(aiResponse)
                    .intent(intent.getIntent())
                    .timestamp(OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")))
                    .build();

        } catch (Exception e) {
            return ChatbotResponse.builder()
                    .answer("Xin lỗi, tôi không thể xử lý câu hỏi của bạn. Vui lòng thử lại.")
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * Classify user intent using AI
     */
    private IntentClassification classifyIntent(String question) {
        try {
            String systemPrompt = """
                    You are an intent classifier for an HR management system chatbot.
                    
                    Available intents:
                    - PERSONAL_INFO: Questions about name, email, phone, address, position, skills
                    - SALARY: Questions about salary, allowance, bonus, KPI penalty
                    - ATTENDANCE: Questions about attendance records, work hours, late minutes
                    - TASKS: Questions about assigned tasks, task status, deadlines
                    - DEPARTMENT: Questions about department, team, manager
                    - PROJECTS: Questions about projects they're working on
                    - MEETING: Questions about meetings, schedules
                    - LEAVE: Questions about leave/time off
                    - GENERAL: General questions, greetings, unclear intent
                    - MEETING: Questions about meetings, schedules, meeting rooms
                    - MEETING_ROOMS: Questions about available rooms, room facilities, room capacity
                    
                    Analyze the question and return ONLY valid JSON:
                    {
                      "intent": "INTENT_NAME",
                      "confidence": 0.95,
                      "parameters": {
                        "timeRange": "this_month|last_month|this_year|specific_date",
                        "specificDate": "2025-01-15",
                        "field": "salary|attendance|tasks"
                      }
                    }
                    
                    Respond in Vietnamese context but return English JSON.
                    """;

            String aiRawResponse = chatClient.prompt()
                    .options(ChatOptions.builder().temperature(0.2).build())
                    .system(systemPrompt)
                    .user(question)
                    .call()
                    .content();

            String cleaned = aiRawResponse
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            JsonNode node = objectMapper.readTree(cleaned);

            return IntentClassification.builder()
                    .intent(node.get("intent").asText())
                    .confidence(node.get("confidence").asDouble())
                    .parameters(objectMapper.convertValue(
                            node.get("parameters"),
                            new TypeReference<Map<String, String>>() {}
                    ))
                    .build();

        } catch (Exception e) {
            return IntentClassification.builder()
                    .intent("GENERAL")
                    .confidence(0.5)
                    .build();
        }
    }

    private String fetchContextData(String personnelCode, IntentClassification intent,
                                    Personnel personnel, Employee employee) {
        StringBuilder context = new StringBuilder();

        try {
            switch (intent.getIntent()) {
                case "PERSONAL_INFO":
                    context.append("# Personal Information\n");
                    context.append("Name: ").append(personnel.getFirstName()).append(" ").append(personnel.getLastName()).append("\n");
                    context.append("Email: ").append(personnel.getEmail()).append("\n");
                    context.append("Phone: ").append(personnel.getPhoneNumber()).append("\n");
                    context.append("Position: ").append(personnel.getPosition()).append("\n");
                    context.append("City: ").append(personnel.getCity()).append("\n");
                    context.append("Skills: ").append(personnel.getSkills()).append("\n");
                    if (employee != null && employee.getDepartment() != null) {
                        context.append("Department: ").append(employee.getDepartment().getName()).append("\n");
                    }
                    break;

                case "ATTENDANCE":
                    context.append("# Attendance Records\n");

                    LocalDate startDate = getStartDateFromParameters(intent.getParameters());
                    LocalDate endDate = LocalDate.now();

                    List<AttendanceRecord> records = attendanceRepository
                            .findByPersonnelCodeAndDateBetween(personnelCode, startDate, endDate);

                    if (records.isEmpty()) {
                        context.append("No attendance records found for the specified period.\n");
                    } else {
                        double totalWorkHours = records.stream()
                                .mapToDouble(AttendanceRecord::getWorkHours)
                                .sum();
                        long lateDays = records.stream()
                                .filter(r -> Boolean.TRUE.equals(r.getIsLate()))
                                .count();

                        context.append("Period: ").append(startDate).append(" to ").append(endDate).append("\n");
                        context.append("Total Work Days: ").append(records.size()).append("\n");
                        context.append("Total Work Hours: ").append(String.format("%.1f", totalWorkHours)).append("\n");
                        context.append("Late Days: ").append(lateDays).append("\n");

                        context.append("\nRecent Records:\n");
                        records.stream().limit(5).forEach(r -> context.append("- ").append(r.getDate())
                                .append(": ").append(r.getWorkHours()).append("h")
                                .append(" (").append(r.getStatus()).append(")")
                                .append(Boolean.TRUE.equals(r.getIsLate()) ? " - Late" : "")
                                .append("\n"));
                    }
                    break;

                case "TASKS":
                    context.append("# Tasks\n");

                    if (employee == null) {
                        context.append("No task information available.\n");
                    } else {
                        List<Task> tasks = taskRepository.findByAssignee_Code(personnelCode);

                        if (tasks.isEmpty()) {
                            context.append("No tasks assigned.\n");
                        } else {
                            Map<TaskStatus, Long> statusCount = tasks.stream()
                                    .collect(Collectors.groupingBy(Task::getStatus, Collectors.counting()));

                            context.append("Total Tasks: ").append(tasks.size()).append("\n");
                            statusCount.forEach((status, count) ->
                                    context.append("- ").append(status).append(": ").append(count).append("\n")
                            );

                            context.append("\nUpcoming/Active Tasks:\n");
                            tasks.stream()
                                    .filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS ||
                                            t.getStatus() == TaskStatus.PENDING)
                                    .limit(5)
                                    .forEach(t -> context.append("- ").append(t.getTitle())
                                            .append(" (").append(t.getStatus()).append(")")
                                            .append(" - Due: ").append(t.getDue())
                                            .append("\n"));
                        }
                    }
                    break;

                case "DEPARTMENT":
                    context.append("# Department Information\n");
                    if (employee != null && employee.getDepartment() != null) {
                        Department dept = employee.getDepartment();
                        context.append("Department: ").append(dept.getName()).append("\n");
                        context.append("Number of Employees: ").append(dept.getEmployeeNumber()).append("\n");
                        context.append("Established: ").append(dept.getEstablishmentDate()).append("\n");

                        if (dept.getManager() != null && dept.getManager().getInformationRecord() != null) {
                            Personnel manager = dept.getManager().getInformationRecord();
                            context.append("Manager: ").append(manager.getFirstName())
                                    .append(" ").append(manager.getLastName()).append("\n");
                        }
                    } else {
                        context.append("No department assigned.\n");
                    }
                    break;

                case "PROJECTS":
                    context.append("# Projects\n");
                    if (employee != null) {
                        List<EmployeeProject> empProjects = employee.getProjects();

                        if (empProjects.isEmpty()) {
                            context.append("No projects assigned.\n");
                        } else {
                            context.append("Total Projects: ").append(empProjects.size()).append("\n\n");
                            empProjects.forEach(ep -> {
                                Project project = ep.getProject();
                                context.append("- ").append(project.getName())
                                        .append(" (").append(project.getStatus()).append(")")
                                        .append(" - Joined: ").append(ep.getJoinedAt())
                                        .append("\n");
                            });
                        }
                    } else {
                        context.append("No project information available.\n");
                    }
                    break;

                case "MEETING":
                    context.append(fetchMeetingData(personnelCode, intent.getParameters()));
                    break;

                case "MEETING_ROOMS":
                    context.append(fetchMeetingRoomData(intent.getParameters()));
                    break;

                default:
                    context.append("# General Information\n");
                    context.append("Employee Code: ").append(personnelCode).append("\n");
                    context.append("Name: ").append(personnel.getFirstName())
                            .append(" ").append(personnel.getLastName()).append("\n");
            }

        } catch (Exception e) {
            context.append("Error fetching data: ").append(e.getMessage());
        }

        return context.toString();
    }

    private String generateResponse(String question, String contextData, IntentClassification intent) {
        try {
            String systemPrompt = """
                    You are a helpful HR assistant chatbot for employees.
                    
                    Rules:
                    1. Answer based ONLY on the provided context data
                    2. Be friendly, professional, and concise
                    3. If data is missing, politely say you don't have that information
                    4. Format numbers nicely (e.g., add commas for large numbers)
                    5. Use Vietnamese for responses
                    6. Don't make up information
                    7. If asked about other employees, politely refuse (privacy)
                    
                    Context Data:
                    %s
                    
                    Respond naturally and conversationally.
                    """.formatted(contextData);

            String personnelCode = SecurityUtil.getCurrentUserCode();

            return chatClient.prompt()
                    .options(ChatOptions.builder().temperature(0.7).build())
                    .system(systemPrompt)
                    .user(question + " " + intent)
                    .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, personnelCode))
                    .call()
                    .content();

        } catch (Exception e) {
            return "Xin lỗi, tôi gặp lỗi khi tạo câu trả lời. Vui lòng thử lại. " + e.getMessage();
        }
    }

    private LocalDate getStartDateFromParameters(Map<String, String> parameters) {
        if (parameters == null) return LocalDate.now().minusMonths(1);

        String timeRange = parameters.get("timeRange");
        if (timeRange != null) {
            return switch (timeRange) {
                case "this_month" -> LocalDate.now().withDayOfMonth(1);
                case "last_month" -> LocalDate.now().minusMonths(1).withDayOfMonth(1);
                case "this_year" -> LocalDate.now().withDayOfYear(1);
                default -> LocalDate.now().withDayOfMonth(1);
            };
        }

        String specificDate = parameters.get("specificDate");
        if (specificDate != null) {
            try {
                return LocalDate.parse(specificDate);
            } catch (Exception e) {
            }
        }

        return LocalDate.now().minusMonths(1);
    }

    private String fetchMeetingData(String personnelCode, Map<String, String> parameters) {
        StringBuilder context = new StringBuilder();
        context.append("# Meeting Information\n");

        try {
            String timeRange = parameters != null ? parameters.get("timeRange") : "upcoming";
            OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            OffsetDateTime startTime;
            OffsetDateTime endTime = switch (timeRange != null ? timeRange : "upcoming") {
                case "today" -> {
                    startTime = now.toLocalDate().atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime();
                    yield startTime.plusDays(1);
                }
                case "tomorrow" -> {
                    startTime = now.toLocalDate().plusDays(1).atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime();
                    yield startTime.plusDays(1);
                }
                case "this_week" -> {
                    startTime = now.toLocalDate().with(java.time.DayOfWeek.MONDAY).atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime();
                    yield startTime.plusWeeks(1);
                }
                case "this_month" -> {
                    startTime = now.toLocalDate().withDayOfMonth(1).atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime();
                    yield startTime.plusMonths(1);
                }
                case "past" -> {
                    startTime = now.minusMonths(1);
                    yield now;
                }
                default -> {
                    startTime = now;
                    yield now.plusMonths(1);
                }
            };

            // Determine time range

            List<MeetingBooking> organizedMeetings = meetingBookingRepository
                    .findByOrganizerCodeAndStartTimeBetween(personnelCode, startTime, endTime);

            List<MeetingBooking> attendingMeetings = meetingBookingRepository
                    .findByAttendeeCodeAndStartTimeBetween(personnelCode, startTime, endTime);

            Set<MeetingBooking> allMeetings = new HashSet<>();
            allMeetings.addAll(organizedMeetings);
            allMeetings.addAll(attendingMeetings);

            List<MeetingBooking> sortedMeetings = allMeetings.stream()
                    .sorted(Comparator.comparing(MeetingBooking::getStartTime))
                    .toList();

            if (sortedMeetings.isEmpty()) {
                context.append("Không có cuộc họp nào trong khoảng thời gian này.\n");
            } else {
                context.append("Tổng số cuộc họp: ").append(sortedMeetings.size()).append("\n");
                context.append("Số cuộc họp bạn tổ chức: ").append(organizedMeetings.size()).append("\n");
                context.append("Số cuộc họp bạn tham gia: ").append(attendingMeetings.size()).append("\n\n");

                context.append("Chi tiết các cuộc họp:\n");

                for (MeetingBooking meeting : sortedMeetings) {
                    MeetingRoom room = meetingRoomRepository.findById(meeting.getRoomId()).orElse(null);

                    context.append("\n--- Cuộc họp ---\n");
                    context.append("Tiêu đề: ").append(meeting.getTitle()).append("\n");

                    if (meeting.getDescription() != null && !meeting.getDescription().isEmpty()) {
                        context.append("Mô tả: ").append(meeting.getDescription()).append("\n");
                    }

                    context.append("Thời gian: ").append(formatDateTime(meeting.getStartTime()))
                            .append(" - ").append(formatDateTime(meeting.getEndTime())).append("\n");

                    long durationMinutes = java.time.Duration.between(
                            meeting.getStartTime(), meeting.getEndTime()
                    ).toMinutes();
                    context.append("Thời lượng: ").append(durationMinutes).append(" phút\n");

                    if (room != null) {
                        context.append("Phòng họp: ").append(room.getName())
                                .append(" (").append(room.getLocation()).append(")\n");
                        context.append("Sức chứa: ").append(room.getCapacity()).append(" người\n");
                        if (room.getEquipment() != null && !room.getEquipment().isEmpty()) {
                            context.append("Thiết bị: ").append(room.getEquipment()).append("\n");
                        }
                    }

                    boolean isOrganizer = meeting.getOrganizerCode().equals(personnelCode);
                    context.append("Vai trò: ").append(isOrganizer ? "Người tổ chức" : "Người tham dự").append("\n");

                    if (meeting.getAttendees() != null && !meeting.getAttendees().isEmpty()) {
                        context.append("Số người tham dự: ").append(meeting.getAttendees().size()).append("\n");
                    }
                }
            }

        } catch (Exception e) {
            context.append("Lỗi khi lấy thông tin cuộc họp: ").append(e.getMessage()).append("\n");
        }

        return context.toString();
    }

    private String fetchMeetingRoomData(Map<String, String> parameters) {
        StringBuilder context = new StringBuilder();
        context.append("# Meeting Room Information\n");

        try {
            String action = parameters != null ? parameters.get("meetingAction") : "available_rooms";

            if ("available_rooms".equals(action)) {
                // Find available rooms for a specific time
                OffsetDateTime checkStart;
                OffsetDateTime checkEnd;

                String timeSlot = parameters != null ? parameters.get("timeSlot") : null;
                String timeRange = parameters != null ? parameters.get("timeRange") : null;

                OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));

                // Determine check time
                if ("tomorrow".equals(timeRange)) {
                    checkStart = now.toLocalDate().plusDays(1).atTime(8, 0)
                            .atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime();
                    checkEnd = checkStart.plusHours(12);
                } else if ("today".equals(timeRange) || timeSlot != null) {
                    LocalDate targetDate = now.toLocalDate();

                    checkEnd = switch (timeSlot != null ? timeSlot : "morning") {
                        case "afternoon" -> {
                            checkStart = targetDate.atTime(13, 0).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime();
                            yield checkStart.plusHours(5);
                        }
                        case "evening" -> {
                            checkStart = targetDate.atTime(18, 0).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime();
                            yield checkStart.plusHours(3);
                        }
                        default -> {
                            checkStart = targetDate.atTime(8, 0).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime();
                            yield checkStart.plusHours(5);
                        }
                    };
                } else {
                    // Default: next 2 hours
                    checkStart = now;
                    checkEnd = now.plusHours(2);
                }

                // Get all rooms
                List<MeetingRoom> allRooms = meetingRoomRepository.findAll();

                // Filter by capacity if specified
                Integer minCapacity = null;
                if (parameters != null && parameters.get("roomCapacity") != null) {
                    try {
                        minCapacity = Integer.parseInt(parameters.get("roomCapacity"));
                    } catch (NumberFormatException e) {
                    }
                }

                final Integer finalMinCapacity = minCapacity;
                List<MeetingRoom> filteredRooms = allRooms.stream()
                        .filter(MeetingRoom::isWorking)
                        .filter(room -> finalMinCapacity == null || room.getCapacity() >= finalMinCapacity)
                        .toList();

                context.append("Kiểm tra phòng trống từ: ").append(formatDateTime(checkStart))
                        .append(" đến ").append(formatDateTime(checkEnd)).append("\n\n");

                if (minCapacity != null) {
                    context.append("Lọc phòng có sức chứa >= ").append(minCapacity).append(" người\n\n");
                }

                List<RoomAvailability> availabilities = new ArrayList<>();

                for (MeetingRoom room : filteredRooms) {
                    List<MeetingBooking> bookings = meetingBookingRepository
                            .findByRoomIdAndTimeOverlap(room.getId(), checkStart, checkEnd);

                    boolean isAvailable = bookings.isEmpty();
                    availabilities.add(new RoomAvailability(room, isAvailable, bookings));
                }

                List<RoomAvailability> available = availabilities.stream()
                        .filter(RoomAvailability::isAvailable)
                        .toList();

                List<RoomAvailability> busy = availabilities.stream()
                        .filter(ra -> !ra.isAvailable())
                        .toList();

                context.append("📊 Tóm tắt:\n");
                context.append("- Tổng số phòng: ").append(filteredRooms.size()).append("\n");
                context.append("- Phòng trống: ").append(available.size()).append("\n");
                context.append("- Phòng đang bận: ").append(busy.size()).append("\n\n");

                if (!available.isEmpty()) {
                    context.append("✅ Phòng trống:\n");
                    for (RoomAvailability ra : available) {
                        MeetingRoom room = ra.getRoom();
                        context.append("\n- ").append(room.getName()).append("\n");
                        context.append("  Vị trí: ").append(room.getLocation()).append("\n");
                        context.append("  Sức chứa: ").append(room.getCapacity()).append(" người\n");
                        if (room.getEquipment() != null && !room.getEquipment().isEmpty()) {
                            context.append("  Thiết bị: ").append(room.getEquipment()).append("\n");
                        }
                    }
                } else {
                    context.append("❌ Không có phòng trống trong khoảng thời gian này.\n");
                }

                if (!busy.isEmpty()) {
                    context.append("\n🔒 Phòng đang bận:\n");
                    for (RoomAvailability ra : busy) {
                        MeetingRoom room = ra.getRoom();
                        context.append("\n- ").append(room.getName()).append(" (")
                                .append(room.getCapacity()).append(" người)\n");

                        for (MeetingBooking booking : ra.getBookings()) {
                            context.append("  • ").append(booking.getTitle())
                                    .append(" (").append(formatDateTime(booking.getStartTime()))
                                    .append(" - ").append(formatDateTime(booking.getEndTime()))
                                    .append(")\n");
                        }
                    }
                }

            } else {
                // List all rooms
                List<MeetingRoom> rooms = meetingRoomRepository.findAll();

                context.append("Danh sách tất cả phòng họp:\n\n");

                for (MeetingRoom room : rooms) {
                    context.append("- ").append(room.getName()).append("\n");
                    context.append("  Vị trí: ").append(room.getLocation()).append("\n");
                    context.append("  Sức chứa: ").append(room.getCapacity()).append(" người\n");
                    context.append("  Trạng thái: ").append(room.isWorking() ? "Hoạt động" : "Bảo trì").append("\n");
                    if (room.getEquipment() != null && !room.getEquipment().isEmpty()) {
                        context.append("  Thiết bị: ").append(room.getEquipment()).append("\n");
                    }
                    context.append("\n");
                }
            }

        } catch (Exception e) {
            context.append("Lỗi khi lấy thông tin phòng họp: ").append(e.getMessage()).append("\n");
        }

        return context.toString();
    }

    private String formatDateTime(OffsetDateTime dateTime) {
        if (dateTime == null) return "N/A";

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter
                .ofPattern("dd/MM/yyyy HH:mm");
        return dateTime.format(formatter);
    }
}