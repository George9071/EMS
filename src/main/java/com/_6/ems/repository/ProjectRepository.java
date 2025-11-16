package com._6.ems.repository;

import java.time.LocalDate;
import java.util.List;

import com._6.ems.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com._6.ems.entity.Department;
import com._6.ems.entity.Project;

@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {
    List<Project> findByDepartment(Department department);
    List<Project> findByStatus(ProjectStatus status);
    List<Project> findByStartDateBetween(LocalDate from, LocalDate to);

    List<Project> findByStatusAndDepartment_Id(ProjectStatus status, Integer departmentId);
    List<Project> findByStartDateBetweenAndDepartment_Id(LocalDate from, LocalDate to, Integer departmentId);
}
