package com._6.ems.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com._6.ems.entity.Employee;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String>{
    List<Employee> findByCodeInAndDepartment_Id(List<String> codes, int departmentId);
    Optional<Employee> findByCode(String code);

    @Query("SELECT e FROM Employee e " +
            "JOIN FETCH e.department " +
            "WHERE e.code IN :codes")
    List<Employee> findByCodeInWithDepartment(@Param("codes") Collection<String> codes);
}
