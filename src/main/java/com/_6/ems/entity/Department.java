package com._6.ems.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

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
    List<Employee> employees;

    @OneToOne(mappedBy = "department", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    Manager manager;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = true)
    List<Project> projects;
}
