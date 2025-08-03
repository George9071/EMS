package com._6.ems.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com._6.ems.entity.Department;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer> {

}
