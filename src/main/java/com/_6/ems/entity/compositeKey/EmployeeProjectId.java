package com._6.ems.entity.compositeKey;

import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeProjectId implements Serializable {
    String employee;
    String project;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmployeeProjectId that)) return false;
        return Objects.equals(employee, that.employee) &&
                Objects.equals(project, that.project);
    }

    @Override
    public int hashCode() {
        return Objects.hash(employee, project);
    }
}
