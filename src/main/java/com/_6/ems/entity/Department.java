package com._6.ems.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "department")
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    int id;

    @Column(name = "name", unique = true)
    String name;

    @Column(name = "number_employees")
    int employeeNumber;

    @Column(name = "establish_date", updatable = false)
    LocalDate establishmentDate;

    @OneToMany(mappedBy = "department")
    Set<Employee> employees = new HashSet<>();

    @OneToOne(mappedBy = "department",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    Manager manager;

    @OneToMany(mappedBy = "department",
            cascade = CascadeType.REMOVE,
            orphanRemoval = true)
    List<Project> projects;

    public void addEmployee(Employee e) {
        if (e == null) return;

        // If employee belongs to another department, unlink from there first
        Department prev = e.getDepartment();
        if (prev != null && prev != this) prev.removeEmployee(e);

        employees.add(e);

        if (e.getDepartment() != this) e.setDepartment(this);

        this.employeeNumber = employees.size();
    }

    public void removeEmployee(Employee e) {
        if (e == null) return;

        if (employees.remove(e)) {
            if (e.getDepartment() == this) e.setDepartment(null);
            this.employeeNumber = employees.size();
        }
    }

    public void assignManager(Manager m) {
        if (this.manager == m) return;

        // Unlink current manager from this department
        if (this.manager != null && this.manager.getDepartment() == this) this.manager.setDepartment(null);

        // If the incoming manager is tied to another department, detach there first
        if (m != null) {
            Department prev = m.getDepartment();
            if (prev != null && prev != this) {
                if (prev.getManager() == m) prev.manager = null;
                m.setDepartment(null);
            }
        }

        // Link new manager on both sides
        this.manager = m;
        if (m != null && m.getDepartment() != this) m.setDepartment(this);
    }

    public void unassignManager() {
        if (this.manager != null) {
            Manager old = this.manager;
            this.manager = null;
            if (old.getDepartment() == this) old.setDepartment(null);
        }
    }
}
