package com._6.ems.schedule;

import com._6.ems.entity.AttendanceRecord;
import com._6.ems.entity.Personnel;
import com._6.ems.enums.AttendanceStatus;
import com._6.ems.repository.AttendanceRepository;
import com._6.ems.repository.PersonnelRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AttendanceScheduler {
    private final PersonnelRepository personnelRepository;
    private final AttendanceRepository attendanceRepository;

    @Scheduled(cron = "0 0 2 * * 1-5", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void createAttendanceRecordForToday() {
        LocalDate today = LocalDate.now();
        List<Personnel> personnelList = personnelRepository.findAll();

        for (Personnel p : personnelList) {
            boolean hasRecord = attendanceRepository.existsByPersonnelAndDate(p, today);
            if (!hasRecord) {
                AttendanceRecord attendance = AttendanceRecord.builder()
                        .personnel(p)
                        .date(today)
                        .status(AttendanceStatus.ABSENT)
                        .build();
                attendanceRepository.save(attendance);
            }
        }
    }

    @Scheduled(cron = "0 0 5 * * TUE-SAT", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void markIncompleteCheckoutsAsAbsent() {
        LocalDate yesterday = LocalDate.now().minusDays(1);


        List<AttendanceRecord> incompleteRecords =
                attendanceRepository.findByDateAndCheckOutIsNull(yesterday);

        for (AttendanceRecord attendanceRecord : incompleteRecords) {
            attendanceRecord.setStatus(AttendanceStatus.ABSENT);
            attendanceRecord.setNotEnoughHours(true);
            attendanceRecord.setMissingHours(8.0);
            attendanceRecord.setNotes("Auto-marked absent due to missing check-out");
        }

        if (!incompleteRecords.isEmpty()) {
            attendanceRepository.saveAll(incompleteRecords);
        }
    }
}
