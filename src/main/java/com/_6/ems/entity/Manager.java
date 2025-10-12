package com._6.ems.entity;

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
@Table(name = "manager")
public class Manager {
    @Id
    @Column(name = "code", unique = true)
    String code;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "code")
    Personnel informationRecord;

    @Column(name = "manage_date")
    LocalDate manageDate;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departmentID", referencedColumnName = "id")
    Department department;

    public void moveToDepartment(Department d) {
        if (this.department == d) return;
        if (this.department != null && this.department.getManager() == this) this.department.setManager(null);
        this.department = d;
        if (d != null && d.getManager() != this) d.setManager(this);
    }
}
