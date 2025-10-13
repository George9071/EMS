package com._6.ems.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com._6.ems.dto.response.*;
import com._6.ems.entity.Personnel;
import com._6.ems.entity.Salary;
import com._6.ems.enums.AttendanceStatus;
import com._6.ems.enums.AttendanceType;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.mapper.AttendanceMapper;
import com._6.ems.repository.PersonnelRepository;
import com._6.ems.repository.SalaryRepository;
import com._6.ems.utils.SecurityUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com._6.ems.entity.AttendanceRecord;
import com._6.ems.repository.AttendanceRepository;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AttendanceService {

    private static final double STANDARD_WORK_HOURS = 8.0;
    private static final LocalTime SHIFT_START = LocalTime.of(9, 0);
    private static final int LUNCH_MINUTES = 60;

    AttendanceRepository attendanceRepository;
    PersonnelRepository personnelRepository;
    AttendanceMapper attendanceMapper;
    SalaryRepository salaryRepository;
    SalaryService salaryService;

    @Transactional
    public AttendanceRecordResponse checkIn() {
        String code = SecurityUtil.getCurrentUserCode();

        Personnel personnel = personnelRepository.findById(code)
                .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_EXISTED));

        LocalDate today = LocalDate.now();

        AttendanceRecord record = attendanceRepository.findByPersonnel_CodeAndDateForUpdate(code, today)
                .orElseGet(() -> AttendanceRecord.builder()
                        .personnel(personnel)
                        .date(today)
                        .status(AttendanceStatus.PRESENT)
                        .build());

        if (record.getCheckIn() != null) throw new AppException(ErrorCode.ATTENDANCE_ALREADY_CHECKIN);

        record.setCheckIn(LocalDateTime.now());
        checkLate(record);

        return attendanceMapper.toAttendanceRecordResponse(attendanceRepository.save(record));
    }

    @Transactional
    public AttendanceRecordResponse checkOut() {
        String code = SecurityUtil.getCurrentUserCode();

        personnelRepository.findById(code)
                .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_EXISTED));

        LocalDate today = LocalDate.now();

        AttendanceRecord record = attendanceRepository.findOpenForUpdate(code)
                .orElseThrow(() -> new AppException(ErrorCode.ATTENDANCE_NOT_CHECKIN));

        if (record.getCheckOut() != null) throw new AppException(ErrorCode.ATTENDANCE_ALREADY_CHECKOUT);

        record.setCheckOut(LocalDateTime.now());
        calculateWorkHours(record);
        classify(record);

        int month = today.getMonthValue();
        int year = today.getYear();

        Salary salary = salaryRepository
                .findByOwner_CodeAndMonthAndYear(code, month, year)
                .orElseGet(() -> salaryService.createSalary(code, month, year));

        switch (record.getType()) {
            case FULL_DAY -> salary.setFullWork(salary.getFullWork() + 1);
            case HALF_DAY -> salary.setHalfWork(salary.getHalfWork() + 1);
            default       -> salary.setAbsence(salary.getAbsence() + 1);
        }

        attendanceRepository.save(record);
        salaryRepository.save(salary);

        return attendanceMapper.toAttendanceRecordResponse(record);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or #employeeCode == authentication.name")
    public AttendanceRecordResponse getRecordByDate(String employeeCode, LocalDate date) {
        AttendanceRecord record = attendanceRepository
                .findByPersonnel_CodeAndDate(employeeCode, date)
                .orElseThrow(() -> new AppException(ErrorCode.ATTENDANCE_RECORD_NOT_FOUND));

        return attendanceMapper.toAttendanceRecordResponse(record);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<AttendanceRecordResponse> getTodayAttendanceSummary() {
        LocalDate today = LocalDate.now();
        List<AttendanceRecord> records = attendanceRepository.findAllWithPersonnelByDate(today);
        return records.stream()
                .map(attendanceMapper::toAttendanceRecordResponse)
                .sorted(Comparator.comparing(AttendanceRecordResponse::getEmployee_code))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<AttendanceRecordResponse> getAllRecordByMonthAndYear(int month, int year) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        List<AttendanceRecord> records = attendanceRepository.findByDateBetween(start, end);

        return records.stream()
                .map(attendanceMapper::toAttendanceRecordResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or #code == authentication.name")
    public List<AttendanceRecordResponse> getAllRecordByEmployeeCode(String code) {
        personnelRepository.findById(code)
                .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_EXISTED));

        return attendanceRepository.findAllByPersonnelCode(code)
                .stream()
                .map(attendanceMapper::toAttendanceRecordResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or #code == authentication.name")
    public List<AttendanceRecordResponse> getAllRecordByEmployeeCodeInterval(String code,
                                                                            LocalDate start,
                                                                            LocalDate end) {
        personnelRepository.findById(code)
                .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_EXISTED));

        return attendanceRepository.findAllByPersonnelCodeAndDateBetween(code, start, end)
                .stream()
                .map(attendanceMapper::toAttendanceRecordResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<AttendanceRecordResponse> getAllRecordsByDateOrInterval(LocalDate start, LocalDate end) {
        // default to today if no dates provided
        LocalDate effectiveStart = (start != null) ? start : LocalDate.now();
        LocalDate effectiveEnd = (end != null) ? end : effectiveStart;

        List<AttendanceRecord> records = attendanceRepository.findAllWithPersonnelByDateBetween(effectiveStart, effectiveEnd);

        return records.stream()
                .map(attendanceMapper::toAttendanceRecordResponse)
                .sorted(Comparator.comparing(AttendanceRecordResponse::getEmployee_code)
                        .thenComparing(AttendanceRecordResponse::getCheckIn))
                .toList();
    }

    public AttendanceOverviewResponse getAttendanceOverview(
            String personnelCode,
            Integer month,
            Integer year) {

        List<AttendanceRecord> records = attendanceRepository
                .findByPersonnelCodeAndMonthAndYear(personnelCode, month, year);

        // Tính toán các chỉ số
        int totalDays = records.size();
        int presentDays = (int) records.stream()
                .filter(r -> r.getStatus() == AttendanceStatus.PRESENT ||
                        r.getStatus() == AttendanceStatus.WORK_FROM_OFFICE ||
                        r.getStatus() == AttendanceStatus.WORK_FROM_HOME)
                .count();
        int lateDays = (int) records.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsLate()))
                .count();
        int absentDays = (int) records.stream()
                .filter(r -> r.getStatus() == AttendanceStatus.ABSENT)
                .count();
        double averageHours = records.stream()
                .mapToDouble(r -> r.getWorkHours() != null ? r.getWorkHours() : 0)
                .average()
                .orElse(0.0);

        List<AttendanceRecordDTO> recordDTOs = mapToDTO(records);

        return AttendanceOverviewResponse.builder()
                .totalDays(totalDays)
                .presentDays(presentDays)
                .lateDays(lateDays)
                .absentDays(absentDays)
                .averageHours(averageHours)
                .records(recordDTOs)
                .build();
    }



    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<AttendanceMonthlySummary> getMonthlySummary(int month, int year) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        List<AttendanceRecord> records =
                attendanceRepository.findAllWithPersonnelByDateBetween(start, end);

        // group by employee code
        Map<String, List<AttendanceRecord>> data =
                records.stream().collect(Collectors.groupingBy(record -> record.getPersonnel().getCode()));

        return data.entrySet().stream().map(employee -> {
                    String code = employee.getKey();
                    List<AttendanceRecord> attendances = employee.getValue();

                    int presentDays = (int) attendances.stream().filter(r ->
                            r.getStatus() == AttendanceStatus.PRESENT
                                    || r.getStatus() == AttendanceStatus.WORK_FROM_OFFICE
                                    || r.getStatus() == AttendanceStatus.WORK_FROM_HOME
                    ).count();

                    int lateDays = (int) attendances.stream().filter(r -> Boolean.TRUE.equals(r.getIsLate())).count();

                    int absentDays = (int) attendances.stream().filter(r ->
                            r.getStatus() == AttendanceStatus.ABSENT
                    ).count();

                    double avgHours = attendances.stream()
                            .mapToDouble(r -> r.getWorkHours() != null ? r.getWorkHours() : 0.0)
                            .average().orElse(0.0);

                    Personnel p = attendances.get(0).getPersonnel();
                    String name = (p.getFirstName() == null ? "" : p.getFirstName()) +
                            " " +
                            (p.getLastName() == null ? "" : p.getLastName());

                    return AttendanceMonthlySummary.builder()
                            .code(code)
                            .name(name.trim())
                            .presentDays(presentDays)
                            .lateDays(lateDays)
                            .absentDays(absentDays)
                            .avgHours(avgHours)
                            .build();
                })
                .toList();

    }


    /* Helper methods */
    private int duration(LocalDateTime start, LocalDateTime end) {
        return (int) Duration.between(start, end).toMinutes();
    }

    private void checkLate(AttendanceRecord record) {
        if (record.getCheckIn() == null) return;
        int late = Math.max(0, (int) Duration.between(
                record.getDate().atTime(SHIFT_START),
                record.getCheckIn()).toMinutes());
        boolean isLate = late > 0;
        record.setIsLate(isLate);
        record.setLateMinutes(isLate ? late : 0);
    }

    private void classify(AttendanceRecord record) {
        double hrs = record.getWorkHours() == null ? 0.0 : record.getWorkHours();
        if (hrs >= 8.0) record.setType(AttendanceType.FULL_DAY);
        else if (hrs >= 4.0) record.setType(AttendanceType.HALF_DAY);
        else record.setType(AttendanceType.ABSENCE);
    }

    // Tính work hours từ check-in và check-out
    private void calculateWorkHours(AttendanceRecord record) {
        if (record.getCheckIn() != null && record.getCheckOut() != null) {
            Duration duration = Duration.between(
                    record.getCheckIn(),
                    record.getCheckOut()
            );

            // Trừ 1 giờ nghỉ trưa
            long totalMinutes = duration.toMinutes() - 60;
            double hours = totalMinutes / 60.0;

            record.setWorkHours(Math.round(hours * 100.0) / 100.0);

            // Kiểm tra không đủ giờ
            record.setNotEnoughHours(hours < STANDARD_WORK_HOURS);

            if (hours < STANDARD_WORK_HOURS) {
                record.setMissingHours(STANDARD_WORK_HOURS - hours);
            }
        }
    }

    // Lấy tên ngày trong tuần bằng tiếng Anh
    private String getDayOfWeekInEnglish(LocalDate date) {
        if (date == null) return "";

        return date.getDayOfWeek().getDisplayName(
                java.time.format.TextStyle.FULL,
                java.util.Locale.ENGLISH
        );
    }

    // Format số giờ thành "10h 2m"
    private String formatWorkHours(Double hours) {
        if (hours == null || hours == 0) return "0m";

        int hourPart = hours.intValue();
        int minutePart = (int) ((hours - hourPart) * 60);

        if (minutePart == 0) return hourPart + "h";

        return hourPart + "h " + minutePart + "m";
    }

    private List<AttendanceRecordDTO> mapToDTO(List<AttendanceRecord> records) {
        return records.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private AttendanceRecordDTO mapToDTO(AttendanceRecord record) {
        LocalTime checkIn = record.getCheckIn() != null ?
                record.getCheckIn().toLocalTime() : null;
        LocalTime checkOut = record.getCheckOut() != null ?
                record.getCheckOut().toLocalTime() : null;

        String workHoursFormatted = formatWorkHours(record.getWorkHours());

        String dayOfWeek = getDayOfWeekInEnglish(record.getDate());

        // Kiểm tra có đủ giờ không
        boolean notEnoughHour = record.getWorkHours() != null &&
                record.getWorkHours() < STANDARD_WORK_HOURS;

        return AttendanceRecordDTO.builder()
                .date(record.getDate())
                .day(dayOfWeek)
                .checkIn(checkIn)
                .checkOut(checkOut)
                .workHours(workHoursFormatted)
                .status(record.getStatus())
                .notEnoughHour(notEnoughHour)
                .build();
    }



//
//    // History of all employees by arbitrary period
//    @Transactional(readOnly = true)
//    @PreAuthorize("hasRole('ADMIN')")
//    public AttendanceHistoryResponse getAllEmployeesHistoryBetween(LocalDate start, LocalDate end) {
//        List<AttendanceRecord> records =
//                attendanceRepository.findAllWithPersonnelByDateBetween(start, end);
//
//        // Optionally: sort by (date asc, code asc)
//        records.sort(Comparator
//                .comparing(AttendanceRecord::getDate)
//                .thenComparing(r -> r.getPersonnel().getCode()));
//
//        List<AttendanceRecordResponse> payload = records.stream()
//                .map(attendanceMapper::toAttendanceRecordResponse)
//                .toList();
//
//        return AttendanceHistoryResponse.builder()
//                .start(start)
//                .end(end)
//                .records(payload)
//                .build();
//    }
}

