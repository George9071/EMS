package com._6.ems.schedule;

import com._6.ems.entity.Personnel;
import com._6.ems.repository.PersonnelRepository;
import com._6.ems.repository.SalaryRepository;
import com._6.ems.service.SalaryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SalaryScheduler {

    private final SalaryService salaryService;
    private final PersonnelRepository personnelRepository;

    @Scheduled(cron = "0 0 0 1 * ?", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void generateMonthlySalaries() {
        List<Personnel> personnels = personnelRepository.findAll();

        for (Personnel personnel : personnels) {
            salaryService.createMonthlySalary(personnel);
        }
    }
}
