package com._6.ems.repository;

import com._6.ems.entity.Employee;
import com._6.ems.entity.Manager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com._6.ems.entity.Department;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer> {

    @Query("SELECT d.id FROM Department d")
    Page<Integer> findAllDepartmentIds(Pageable pageable);

    @Query("""
       SELECT DISTINCT d
       FROM Department d
       LEFT JOIN FETCH d.manager m
       LEFT JOIN FETCH m.informationRecord
       WHERE d.id IN :ids
    """)
    List<Department> findAllByIdsWithManagerDetails(@Param("ids") List<Integer> ids);



    /**
     * Fetches a single {@link Department} by its ID along with all its related entities:
     * - The {@link Manager} assigned to this department (if any)
     * - The manager’s {@code informationRecord}
     * - All {@link Employee} entities that belong to this department
     * - Each employee’s {@code informationRecord}
     *
     * @param id The unique identifier of the department
     * @return An {@link Optional} containing the department and its related data if found, otherwise an empty Optional
     */
    @Query("SELECT d FROM Department d " +
            "LEFT JOIN FETCH d.manager m " +
            "LEFT JOIN FETCH m.informationRecord " +
            "LEFT JOIN FETCH d.employees e " +
            "LEFT JOIN FETCH e.informationRecord " +
            "WHERE d.id = :id")
    Optional<Department> findByIdWithDetails(@Param("id") int id);

    /**
     * Fetches a {@link Department} by its ID along with its {@link Manager} and the manager’s {@code informationRecord}
     *
     * @param id The unique identifier of the department
     * @return An {@link Optional} containing the department with manager details if found, otherwise an empty Optional
     */
    @Query("""
       SELECT d
       FROM Department d
       LEFT JOIN FETCH d.manager m
       LEFT JOIN FETCH m.informationRecord
       WHERE d.id = :id
    """)
    Optional<Department> findByIdWithManagerDetails(@Param("id") int id);

    /**
     * Fetches all {@link Department} entities along with their managers and each manager’s {@code informationRecord}.*
     * @return A {@link List} of all departments with their manager details
     */
    @Query("""
       SELECT DISTINCT d
       FROM Department d
       LEFT JOIN FETCH d.manager m
       LEFT JOIN FETCH m.informationRecord
    """)
    List<Department> findAllWithManagerDetails();
}
