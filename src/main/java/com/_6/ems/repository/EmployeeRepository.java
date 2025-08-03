package com._6.ems.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com._6.ems.entity.Employee;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String>{
    List<Employee> findByCodeInAndDepartment_Id(List<String> codes, int departmentId);
}
