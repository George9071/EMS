package com._6.ems.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com._6.ems.entity.Task;

@Repository
public interface TaskRepository extends JpaRepository<Task, String>{

    // 1. Find tasks by project code
    List<Task> findByProject_Id(String projectCode);

    // 2. Find tasks by assignee (employee) code
    List<Task> findByAssignee_Code(String employeeCode);
}
