package com._6.ems.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import com._6.ems.dto.response.*;
import com._6.ems.entity.Personnel;
import com._6.ems.entity.Salary;
import com._6.ems.enums.AttendanceStatus;
import com._6.ems.enums.AttendanceType;
import com._6.ems.enums.WorkLocation;
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
    private static final LocalTime SHIFT_END = LocalTime.of(18, 0);
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

        // If there is an open record (no checkout) — lock it
        Optional<AttendanceRecord> openOpt = attendanceRepository.findOpenForUpdate(code);
        if (openOpt.isPresent()) {
            AttendanceRecord open = openOpt.get();
            if (open.getDate().isEqual(today))
                throw new AppException(ErrorCode.ATTENDANCE_ALREADY_CHECKIN);

            open.setCheckOut(open.getCheckIn());
            open.setType(AttendanceType.ABSENCE);
            calculateWorkHours(open);

            int month = open.getDate().getMonthValue();
            int year  = open.getDate().getYear();

            Salary salary = salaryRepository.findByOwner_CodeAndMonthAndYear(code, month, year)
                    .orElseGet(() -> {
                        try {
                            return salaryService.createSalary(code, month, year);
                        } catch (org.springframework.dao.DataIntegrityViolationException e) {
                            return salaryRepository.findByOwner_CodeAndMonthAndYear(code, month, year).orElseThrow();
                        }
                    });

            salary.setAbsence(salary.getAbsence() + 1);
            attendanceRepository.save(open);
            salaryRepository.save(salary);
        }

        AttendanceRecord record = attendanceRepository
                .findByPersonnel_CodeAndDateForUpdate(code, today)
                .orElseGet(() -> attendanceRepository.save(
                        AttendanceRecord.builder()
                                .personnel(personnel)
                                .date(today)
                                .status(AttendanceStatus.PRESENT)
                                .build()
                ));

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
    public List<AttendanceRecordResponse> getAllRecordByEmployeeCodeInterval(
            String code,
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

        Metrics m = computeMetrics(records);
        List<AttendanceRecordDTO> recordDTOs = attendanceMapper.toDTOList(records);

        return AttendanceOverviewResponse.builder()
                .totalDays(m.totalDays)
                .presentDays(m.presentDays)
                .lateDays(m.lateDays)
                .absentDays(m.absentDays)
                .averageHours(m.avgHours)
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
        Map<String, List<AttendanceRecord>> data = records.stream()
                .collect(Collectors.groupingBy(record -> record.getPersonnel().getCode()));

        return data.entrySet().stream()
                .map(entry -> {
                    String code = entry.getKey();
                    List<AttendanceRecord> attendances = entry.getValue();

                    Metrics m = computeMetrics(attendances);

                    Personnel p = attendances.getFirst().getPersonnel();
                    String first = p.getFirstName() != null ? p.getFirstName() : "";
                    String last  = p.getLastName()  != null ? p.getLastName()  : "";
                    String name  = (first + " " + last).trim();

                    return AttendanceMonthlySummary.builder()
                            .code(code)
                            .name(name)
                            .presentDays(m.presentDays)
                            .lateDays(m.lateDays)
                            .absentDays(m.absentDays)
                            .avgHours(m.avgHours)
                            .build();
                })
                .toList();

    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public int syncBetween(LocalDate start, LocalDate end) {
        List<AttendanceRecord> records = attendanceRepository.findByDateBetween(start, end);
        int updated = 0;
        for (AttendanceRecord r : records) {
            sync(r);
            updated++;
        }
        return updated;
    }

    /* Helper methods */
    private long duration(LocalDateTime start, LocalDateTime end) {
        return Duration.between(start, end).toMinutes();
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

    // Classify type of record (FULLDAY | HALFDAY | ABSENCE | OVERTIME | UNKNOWN)
    private void classify(AttendanceRecord record) {
        double duration = record.getWorkHours() == null ? 0.0 : record.getWorkHours();
        if (duration >= 8.0) record.setType(AttendanceType.FULL_DAY);
        else if (duration >= 4.0) record.setType(AttendanceType.HALF_DAY);
        else record.setType(AttendanceType.ABSENCE);
    }

    // Calculate work hours from duration(check-in,check-out)
    private void calculateWorkHours(AttendanceRecord record) {
        if (record.getCheckIn() != null && record.getCheckOut() != null) {
            Duration duration = Duration.between(
                    record.getCheckIn(),
                    record.getCheckOut()
            );

            // skip lunch break
            long totalMinutes = duration.toMinutes() - LUNCH_MINUTES;
            double hours = totalMinutes / 60.0;

            record.setWorkHours(Math.round(hours * 100.0) / 100.0);
            // Check if not enough standard work hours
            record.setNotEnoughHours(hours < STANDARD_WORK_HOURS);

            if (hours < STANDARD_WORK_HOURS) {
                record.setMissingHours(STANDARD_WORK_HOURS - hours);
            }
        }
    }

    private void sync(AttendanceRecord r) {
        LocalDate date = r.getDate();
        LocalDateTime in  = r.getCheckIn();
        LocalDateTime out = r.getCheckOut();

        double workHours = 0.0;
        boolean isLate = false;
        int lateMins = 0;
        boolean notEnough = true;
        double missing = STANDARD_WORK_HOURS;
        WorkLocation loc = WorkLocation.OFFICE;
        AttendanceStatus status = AttendanceStatus.ABSENT;
        AttendanceType type = AttendanceType.ABSENCE;

        if (in != null) {
            if (out != null) {
                long totalMinutes = duration(in, out);
                if (overlapsLunch(in.toLocalTime(), out.toLocalTime())) {
                    totalMinutes = Math.max(0, totalMinutes - LUNCH_MINUTES);
                    workHours = Math.round((totalMinutes / 60.0) * 100.0) / 100.0;
                }
            }

            // Late logic
            LocalDateTime shiftStart = date.atTime(SHIFT_START);
            lateMins = (in.isAfter(shiftStart)) ? (int) duration(shiftStart, in) : 0;
            isLate = lateMins > 0;

            // Missing / not enough
            missing = Math.round(Math.max(0.0, STANDARD_WORK_HOURS - workHours) * 100.0) / 100.0;;
            notEnough = workHours < STANDARD_WORK_HOURS;

            loc = WorkLocation.OFFICE;

            if (isLate) status = AttendanceStatus.LATE_ARRIVAL;
            else status = AttendanceStatus.WORK_FROM_OFFICE;

            if (workHours >= STANDARD_WORK_HOURS) {
                type = (workHours > STANDARD_WORK_HOURS) ? AttendanceType.OVERTIME : AttendanceType.FULL_DAY;
            } else if (workHours >= 4.0) {
                type = AttendanceType.HALF_DAY;
            } else {
                status = AttendanceStatus.ABSENT;
            }

            r.setWorkHours(workHours);
            r.setIsLate(isLate);
            r.setLateMinutes(lateMins);
            r.setNotEnoughHours(notEnough);
            r.setMissingHours(missing);
            r.setWorkLocation(loc);
            r.setStatus(status);
            r.setType(type);

            attendanceRepository.save(r);
        }
    }

    private boolean overlapsLunch(LocalTime start, LocalTime end) {
        if (start == null || end == null) return false;
        if (end.isBefore(start)) return false;
        // Typical lunch window 12:00–13:00
        LocalTime LUNCH_START = LocalTime.NOON;
        LocalTime LUNCH_END   = LocalTime.NOON.plusHours(1);
        return !(end.isBefore(LUNCH_START) || start.isAfter(LUNCH_END));
    }

    private final EnumSet<AttendanceStatus> PRESENT_STATUSES = EnumSet.of(
            AttendanceStatus.PRESENT,
            AttendanceStatus.WORK_FROM_OFFICE,
            AttendanceStatus.WORK_FROM_HOME
    );

    private record Metrics(int totalDays, int presentDays, int lateDays, int absentDays, double avgHours) {
    }

    private Metrics computeMetrics(List<AttendanceRecord> records) {
        int totalDays = records.size();

        int presentDays = (int) records.stream()
                .filter(r -> PRESENT_STATUSES.contains(r.getStatus()))
                .count();

        int lateDays = (int) records.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsLate()))
                .count();

        int absentDays = (int) records.stream()
                .filter(r -> r.getStatus() == AttendanceStatus.ABSENT)
                .count();

        double avgHours = records.stream()
                .mapToDouble(r -> r.getWorkHours() != null ? r.getWorkHours() : 0.0)
                .average()
                .orElse(0.0);

        return new Metrics(totalDays, presentDays, lateDays, absentDays, avgHours);
    }

}

