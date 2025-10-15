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
import com._6.ems.enums.AttendanceStatus;
import com._6.ems.enums.AttendanceType;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.mapper.AttendanceMapper;
import com._6.ems.repository.PersonnelRepository;
import com._6.ems.repository.SalaryRepository;
import com._6.ems.utils.PersonnelUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${salary.standard-work-hours}")
    private static double standardWorkHours;

    private static final LocalTime SHIFT_START = LocalTime.of(9, 0);

    AttendanceRepository attendanceRepository;
    PersonnelRepository personnelRepository;
    AttendanceMapper attendanceMapper;
    SalaryRepository salaryRepository;
    SalaryService salaryService;
    PersonnelUtil personnelUtil;

    @Transactional
    public AttendanceRecordResponse checkIn() {
        Personnel personnel = personnelUtil.getCurrentPersonnel();

        LocalDate today = LocalDate.now();

        AttendanceRecord attendanceRecord = attendanceRepository
                .findByPersonnel_CodeAndDateForUpdate(personnel.getCode(), today)
                .orElseGet(() -> AttendanceRecord.builder()
                        .personnel(personnel)
                        .date(today)
                        .status(AttendanceStatus.PRESENT)
                        .build());

        if (attendanceRecord.getCheckIn() != null) throw new AppException(ErrorCode.ATTENDANCE_ALREADY_CHECKIN);

        attendanceRecord.setCheckIn(LocalDateTime.now());

        if(LocalTime.now().isAfter(SHIFT_START)) {
            attendanceRecord.setStatus(AttendanceStatus.LATE_ARRIVAL);
        }

        checkLate(attendanceRecord);

        boolean exists = salaryRepository
                .existsByPersonnelAndMonthAndYear(personnel, today.getMonthValue(), today.getYear());
        if (!exists) {
            salaryService.createMonthlySalary(personnel);
        }

        return attendanceMapper.toAttendanceRecordResponse(attendanceRepository.save(attendanceRecord));
    }

    @Transactional
    public AttendanceRecordResponse checkOut() {
        Personnel personnel = personnelUtil.getCurrentPersonnel();

        AttendanceRecord attendanceRecord = attendanceRepository.findOpenForUpdate(personnel.getCode())
                .orElseThrow(() -> new AppException(ErrorCode.ATTENDANCE_NOT_CHECKIN));

        if (attendanceRecord.getCheckOut() != null) throw new AppException(ErrorCode.ATTENDANCE_ALREADY_CHECKOUT);

        attendanceRecord.setCheckOut(LocalDateTime.now());
        calculateWorkHours(attendanceRecord);
        classify(attendanceRecord);

        salaryService.calculateSalary(personnel);

        attendanceRepository.save(attendanceRecord);

        return attendanceMapper.toAttendanceRecordResponse(attendanceRecord);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or #employeeCode == authentication.name")
    public AttendanceRecordResponse getRecordByDate(String employeeCode, LocalDate date) {
        AttendanceRecord attendanceRecord = attendanceRepository
                .findByPersonnel_CodeAndDate(employeeCode, date)
                .orElseThrow(() -> new AppException(ErrorCode.ATTENDANCE_RECORD_NOT_FOUND));

        return attendanceMapper.toAttendanceRecordResponse(attendanceRecord);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<AttendanceRecordResponse> getTodayAttendanceSummary() {
        LocalDate today = LocalDate.now();
        List<AttendanceRecord> records = attendanceRepository.findAllWithPersonnelByDate(today);
        return records.stream()
                .map(attendanceMapper::toAttendanceRecordResponse)
                .sorted(Comparator.comparing(AttendanceRecordResponse::getEmployee_code))
                .toList();
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
                .filter(r -> r.getStatus() == AttendanceStatus.PRESENT)
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
        if (hrs > 9.0) record.setType(AttendanceType.OVERTIME);
        else if (hrs >= 8.0) record.setType(AttendanceType.FULL_DAY);
        else if (hrs >= 4.0) record.setType(AttendanceType.HALF_DAY);
        else record.setType(AttendanceType.NOT_ENOUGH_HOURS);
    }

    // Tính work hours từ check-in và check-out
    private void calculateWorkHours(AttendanceRecord record) {
        if (record.getCheckIn() != null && record.getCheckOut() != null) {
            Duration duration = Duration.between(
                    record.getCheckIn(),
                    record.getCheckOut()
            );

            double hours = duration.toMinutes() / 60.0;

            record.setWorkHours(Math.round(hours * 100.0) / 100.0);

            // Kiểm tra không đủ giờ
            record.setNotEnoughHours(hours < standardWorkHours);

            if (hours < standardWorkHours) {
                record.setMissingHours(standardWorkHours - hours);
            } else {
                record.setMissingHours(0.0);
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
                .toList();
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
                record.getWorkHours() < standardWorkHours;

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

