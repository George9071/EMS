package com._6.ems.entity;

import com._6.ems.entity.compositeKey.EmployeeProjectId;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmployeeProject)) {
            return false;
        }

        EmployeeProject that = (EmployeeProject) o;

        if (employee == null || that.employee == null ||
                project == null || that.project == null) {
            return false; // Không nên xảy ra với các trường ID
        }

        return Objects.equals(employee.getCode(), that.employee.getCode()) &&
                Objects.equals(project.getId(), that.project.getId());
    }

    @Override
    public int hashCode() {
        if (employee == null || project == null) {
            return 0;
        }

        return Objects.hash(employee.getCode(), project.getId());
    }
}
