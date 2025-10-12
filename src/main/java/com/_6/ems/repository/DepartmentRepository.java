package com._6.ems.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com._6.ems.entity.Department;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer> {
    @Query("SELECT d FROM Department d " +
            "LEFT JOIN FETCH d.manager m " +
            "LEFT JOIN FETCH m.informationRecord " +
            "LEFT JOIN FETCH d.employees e " +
            "LEFT JOIN FETCH e.informationRecord " +
            "WHERE d.id = :id")
    Optional<Department> findByIdWithDetails(@Param("id") int id);

    @Query("""
       SELECT d
       FROM Department d
       LEFT JOIN FETCH d.manager m
       LEFT JOIN FETCH m.informationRecord
       WHERE d.id = :id
    """)
    Optional<Department> findByIdWithManagerDetails(@Param("id") int id);

    @Query("""
       SELECT DISTINCT d
       FROM Department d
       LEFT JOIN FETCH d.manager m
       LEFT JOIN FETCH m.informationRecord
    """)
    List<Department> findAllWithManagerDetails();
}
