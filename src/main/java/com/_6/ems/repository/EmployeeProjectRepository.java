package com._6.ems.repository;

import com._6.ems.entity.EmployeeProject;
import com._6.ems.entity.compositeKey.EmployeeProjectId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeProjectRepository extends JpaRepository<EmployeeProject, EmployeeProjectId> {
    boolean existsByEmployee_CodeAndProject_Id(String employeeCode, String projectId);
}
