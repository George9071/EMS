package com._6.ems.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com._6.ems.entity.Personnel;
import com._6.ems.entity.Salary;
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
import org.springframework.stereotype.Service;

import com._6.ems.dto.response.AttendanceRecordResponse;
import com._6.ems.entity.AttendanceRecord;
import com._6.ems.repository.AttendanceRepository;
import com._6.ems.repository.EmployeeRepository;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AttendanceService {

    AttendanceRepository attendanceRepository;
    EmployeeRepository employeeRepository;
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

        AttendanceRecord record = attendanceRepository.findByPersonnel_CodeAndDate(code, today)
                .orElseGet(() -> AttendanceRecord.builder()
                        .personnel(personnel)
                        .date(today)
                        .build());

        if (record.getCheckIn() != null) throw new AppException(ErrorCode.ATTENDANCE_ALREADY_CHECKIN);

        record.setCheckIn(LocalDateTime.now());
        return attendanceMapper.toAttendanceRecordResponse(attendanceRepository.save(record));
    }

    @Transactional
    public AttendanceRecordResponse checkOut() {
        String code = SecurityUtil.getCurrentUserCode();

        employeeRepository.findById(code)
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_FOUND));

        LocalDate today = LocalDate.now();

        AttendanceRecord record = attendanceRepository.findByPersonnel_CodeAndDate(code, today)
                .orElseThrow(() -> new AppException(ErrorCode.ATTENDANCE_NOT_CHECKIN));

        if (record.getCheckOut() != null) throw new AppException(ErrorCode.ATTENDANCE_ALREADY_CHECKOUT);

        record.setCheckOut(LocalDateTime.now());

        long worked = Duration.between(record.getCheckIn(), record.getCheckOut()).toMinutes();

        int month = today.getMonthValue();
        int year = today.getYear();

        Salary salary = salaryRepository.findByOwner_CodeAndMonthAndYear(code, month, year)
                .orElse(salaryService.createSalary(code, month, year));

        if (worked >= 3) {
            record.setType(AttendanceType.FULL_DAY);
            salary.setFullWork(salary.getFullWork() + 1);
        } else if (worked >= 1) {
            record.setType(AttendanceType.HALF_DAY);
            salary.setHalfWork(salary.getHalfWork() + 1);
        } else {
            record.setType(AttendanceType.ABSENCE);
            salary.setAbsence(salary.getAbsence() + 1);
        }

        attendanceRepository.save(record);
        salaryRepository.save(salary);

        return attendanceMapper.toAttendanceRecordResponse(record);
    }

    public AttendanceRecordResponse getRecordByDate(String employeeCode, LocalDate date) {
        AttendanceRecord record = attendanceRepository
                .findByPersonnel_CodeAndDate(employeeCode, date)
                .orElseThrow(()
                        -> new AppException(ErrorCode.ATTENDANCE_RECORD_NOT_FOUND));

        return attendanceMapper.toAttendanceRecordResponse(record);
    }

    public List<AttendanceRecordResponse> getAllRecordToday() {
        LocalDate today = LocalDate.now();

        List<AttendanceRecord> records = attendanceRepository.findByDateBetween(today, today);
        log.info("records: {}", records);

        return records.stream()
                .map(attendanceMapper::toAttendanceRecordResponse)
                .collect(Collectors.toList());
    }

    public List<AttendanceRecordResponse> getAllRecordByMonthAndYear(int month, int year) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth()); // last day of month

        List<AttendanceRecord> records = attendanceRepository.findByDateBetween(startDate, endDate);

        return records.stream()
                .map(attendanceMapper::toAttendanceRecordResponse)
                .collect(Collectors.toList());
    }

    public List<AttendanceRecordResponse> getAllRecordByEmployeeCode(String code) {
        employeeRepository.findById(code)
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_FOUND));

        List<AttendanceRecord> records = attendanceRepository.findByPersonnel_Code(code);

        return records.stream()
                .map(attendanceMapper::toAttendanceRecordResponse)
                .collect(Collectors.toList());
    }

    public List<AttendanceRecordResponse> getAllRecordByEmployeeCodeBetween(String code, LocalDate start, LocalDate end) {
        employeeRepository.findById(code)
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_FOUND));

        List<AttendanceRecord> records = attendanceRepository.findByPersonnel_CodeAndDateBetween(code, start, end);

        return records.stream()
                .map(attendanceMapper::toAttendanceRecordResponse)
                .collect(Collectors.toList());
    }
}
