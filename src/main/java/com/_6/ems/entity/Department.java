package com._6.ems.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
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

    @OneToMany(mappedBy = "department", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    Set<Employee> employees;

    @OneToOne(mappedBy = "department", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    Manager manager;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<Project> projects;

    public void addEmployee(Employee e) {
        employees.add(e);
        e.setDepartment(this);
        this.employeeNumber = employees.size();
    }

    public void removeEmployee(Employee e) {
        if (employees.remove(e)) {
            e.setDepartment(null);
            this.employeeNumber = employees.size();
        }
    }

    public void assignManager(Manager m) {
        if (this.manager == m) return;
        if (this.manager != null) this.manager.setDepartment(null);
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
