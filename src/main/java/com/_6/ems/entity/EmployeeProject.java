package com._6.ems.entity;

import com._6.ems.entity.compositeKey.EmployeeProjectId;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "employee_in_project")
@IdClass(EmployeeProjectId.class)
public class EmployeeProject {

    @Id
    @ManyToOne
    @JoinColumn(name = "employee_code")
    Employee employee;

    @Id
    @ManyToOne
    @JoinColumn(name = "project_id")
    Project project;

    LocalDate joinedAt;
}
