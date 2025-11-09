package com._6.ems.service;

import com._6.ems.dto.response.AttendanceRecordResponse;
import com._6.ems.dto.response.AttendanceStatusResponse;
import com._6.ems.entity.Account;
import com._6.ems.entity.AttendanceRecord;
import com._6.ems.entity.Personnel;
import com._6.ems.enums.AttendanceStatus;
import com._6.ems.enums.Role;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.mapper.AttendanceMapper;
import com._6.ems.repository.AttendanceRepository;
import com._6.ems.repository.PersonnelRepository;
import com._6.ems.repository.SalaryRepository;
import com._6.ems.utils.PersonnelUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private PersonnelRepository personnelRepository;

    @Mock
    private AttendanceMapper attendanceMapper;

    @Mock
    private SalaryRepository salaryRepository;

    @Mock
    private SalaryService salaryService;

    @Mock
    private PersonnelUtil personnelUtil;

    @InjectMocks
    private AttendanceService attendanceService;

    private Personnel personnel;
    private Account account;
    private AttendanceRecord attendanceRecord;
    private AttendanceRecordResponse attendanceRecordResponse;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));

        account = Account.builder()
                .id("ACC001")
                .username("testuser")
                .role(Role.EMPLOYEE)
                .build();

        personnel = Personnel.builder()
                .code("PER001")
                .firstName("John")
                .lastName("Doe")
                .account(account)
                .build();

        attendanceRecord = AttendanceRecord.builder()
                .id("ATT001")
                .personnel(personnel)
                .date(today)
                .status(AttendanceStatus.PRESENT)
                .build();

        attendanceRecordResponse = AttendanceRecordResponse.builder()
                .record_id("ATT001")
                .employee_code("PER001")
                .build();
    }

    @Test
    void checkIn_firstTimeToday_success() {
        // GIVEN
        when(personnelUtil.getCurrentPersonnel()).thenReturn(personnel);
        when(attendanceRepository.findByPersonnel_CodeAndDateForUpdate(personnel.getCode(), today))
                .thenReturn(Optional.empty());
        when(salaryRepository.existsByPersonnelAndMonthAndYear(any(), anyInt(), anyInt()))
                .thenReturn(true);
        when(attendanceRepository.save(any(AttendanceRecord.class))).thenReturn(attendanceRecord);
        when(attendanceMapper.toAttendanceRecordResponse(any(AttendanceRecord.class)))
                .thenReturn(attendanceRecordResponse);

        // WHEN
        AttendanceRecordResponse result = attendanceService.checkIn();

        // THEN
        assertThat(result).isNotNull();
        verify(personnelUtil).getCurrentPersonnel();
        verify(attendanceRepository).save(any(AttendanceRecord.class));
    }

    @Test
    void checkIn_alreadyCheckedIn_throwsException() {
        // GIVEN
        attendanceRecord.setCheckIn(OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));

        when(personnelUtil.getCurrentPersonnel()).thenReturn(personnel);
        when(attendanceRepository.findByPersonnel_CodeAndDateForUpdate(personnel.getCode(), today))
                .thenReturn(Optional.of(attendanceRecord));

        // WHEN
        AppException exception = assertThrows(AppException.class,
                () -> attendanceService.checkIn());

        // THEN
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ATTENDANCE_ALREADY_CHECKIN);
        verify(personnelUtil).getCurrentPersonnel();
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void checkOut_success() {
        // GIVEN
        attendanceRecord.setCheckIn(OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusHours(8));

        when(personnelUtil.getCurrentPersonnel()).thenReturn(personnel);
        when(attendanceRepository.findOpenForUpdate(personnel.getCode()))
                .thenReturn(Optional.of(attendanceRecord));
        when(attendanceRepository.save(any(AttendanceRecord.class))).thenReturn(attendanceRecord);
        when(attendanceMapper.toAttendanceRecordResponse(attendanceRecord))
                .thenReturn(attendanceRecordResponse);
        doNothing().when(salaryService).calculateSalary(personnel);

        // WHEN
        AttendanceRecordResponse result = attendanceService.checkOut();

        // THEN
        assertThat(result).isNotNull();
        assertThat(attendanceRecord.getCheckOut()).isNotNull();
        verify(personnelUtil).getCurrentPersonnel();
        verify(attendanceRepository).save(attendanceRecord);
        verify(salaryService).calculateSalary(personnel);
    }

    @Test
    void checkOut_notCheckedIn_throwsException() {
        // GIVEN
        when(personnelUtil.getCurrentPersonnel()).thenReturn(personnel);
        when(attendanceRepository.findOpenForUpdate(personnel.getCode()))
                .thenReturn(Optional.empty());

        // WHEN
        AppException exception = assertThrows(AppException.class,
                () -> attendanceService.checkOut());

        // THEN
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ATTENDANCE_NOT_CHECKIN);
        verify(personnelUtil).getCurrentPersonnel();
    }

    @Test
    void checkOut_alreadyCheckedOut_throwsException() {
        // GIVEN
        attendanceRecord.setCheckIn(OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusHours(8));
        attendanceRecord.setCheckOut(OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));

        when(personnelUtil.getCurrentPersonnel()).thenReturn(personnel);
        when(attendanceRepository.findOpenForUpdate(personnel.getCode()))
                .thenReturn(Optional.of(attendanceRecord));

        // WHEN
        AppException exception = assertThrows(AppException.class,
                () -> attendanceService.checkOut());

        // THEN
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ATTENDANCE_ALREADY_CHECKOUT);
        verify(personnelUtil).getCurrentPersonnel();
    }

    @Test
    void getTodayStatusByPersonnelCode_success() {
        // GIVEN
        attendanceRecord.setCheckIn(OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusHours(5));
        
        when(attendanceRepository.findTodayRecordByPersonnelCode("PER001", today))
                .thenReturn(Optional.of(attendanceRecord));

        // WHEN
        AttendanceStatusResponse result = attendanceService.getTodayStatusByPersonnelCode("PER001");

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        verify(attendanceRepository).findTodayRecordByPersonnelCode("PER001", today);
    }

    @Test
    void getTodayStatusByPersonnelCode_notYetCheckedIn_throwsException() {
        // GIVEN
        when(attendanceRepository.findTodayRecordByPersonnelCode("PER001", today))
                .thenReturn(Optional.empty());

        // WHEN
        AppException exception = assertThrows(AppException.class,
                () -> attendanceService.getTodayStatusByPersonnelCode("PER001"));

        // THEN
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ATTENDANCE_NOT_YET);
        verify(attendanceRepository).findTodayRecordByPersonnelCode("PER001", today);
    }

    @Test
    void getRecordByDate_success() {
        // GIVEN
        LocalDate date = LocalDate.of(2025, 1, 15);
        when(attendanceRepository.findByPersonnel_CodeAndDate("PER001", date))
                .thenReturn(Optional.of(attendanceRecord));
        when(attendanceMapper.toAttendanceRecordResponse(attendanceRecord))
                .thenReturn(attendanceRecordResponse);

        // WHEN
        AttendanceRecordResponse result = attendanceService.getRecordByDate("PER001", date);

        // THEN
        assertThat(result).isNotNull();
        verify(attendanceRepository).findByPersonnel_CodeAndDate("PER001", date);
    }

    @Test
    void getRecordByDate_notFound_throwsException() {
        // GIVEN
        LocalDate date = LocalDate.of(2025, 1, 15);
        when(attendanceRepository.findByPersonnel_CodeAndDate("PER001", date))
                .thenReturn(Optional.empty());

        // WHEN
        AppException exception = assertThrows(AppException.class,
                () -> attendanceService.getRecordByDate("PER001", date));

        // THEN
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ATTENDANCE_RECORD_NOT_FOUND);
        verify(attendanceRepository).findByPersonnel_CodeAndDate("PER001", date);
    }

    @Test
    void getTodayAttendanceSummary_success() {
        // GIVEN
        AttendanceRecord record2 = AttendanceRecord.builder()
                .id("ATT002")
                .personnel(Personnel.builder().code("PER002").build())
                .date(today)
                .status(AttendanceStatus.PRESENT)
                .build();

        List<AttendanceRecord> records = Arrays.asList(attendanceRecord, record2);

        when(attendanceRepository.findAllWithPersonnelByDate(today)).thenReturn(records);
        when(attendanceMapper.toAttendanceRecordResponse(any(AttendanceRecord.class)))
                .thenReturn(attendanceRecordResponse);

        // WHEN
        List<AttendanceRecordResponse> result = attendanceService.getTodayAttendanceSummary();

        // THEN
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        verify(attendanceRepository).findAllWithPersonnelByDate(today);
    }

    @Test
    void getAllRecordByEmployeeCode_success() {
        // GIVEN
        when(personnelRepository.findById("PER001")).thenReturn(Optional.of(personnel));
        when(attendanceRepository.findAllByPersonnelCode("PER001"))
                .thenReturn(Arrays.asList(attendanceRecord));
        when(attendanceMapper.toAttendanceRecordResponse(attendanceRecord))
                .thenReturn(attendanceRecordResponse);

        // WHEN
        List<AttendanceRecordResponse> result = attendanceService.getAllRecordByEmployeeCode("PER001");

        // THEN
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(personnelRepository).findById("PER001");
        verify(attendanceRepository).findAllByPersonnelCode("PER001");
    }

    @Test
    void getAllRecordByEmployeeCode_personnelNotFound_throwsException() {
        // GIVEN
        when(personnelRepository.findById("INVALID")).thenReturn(Optional.empty());

        // WHEN
        AppException exception = assertThrows(AppException.class,
                () -> attendanceService.getAllRecordByEmployeeCode("INVALID"));

        // THEN
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROFILE_NOT_EXISTED);
        verify(personnelRepository).findById("INVALID");
    }
}
