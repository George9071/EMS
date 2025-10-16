package com._6.ems.entity;

import com._6.ems.enums.AttendanceStatus;
import com._6.ems.enums.AttendanceType;
import com._6.ems.enums.WorkLocation;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(
        name = "attendance_record",
        uniqueConstraints = @UniqueConstraint(
                name = "attendance_date_personnel_persistent",
                columnNames = {"personnel_code", "work_date"}
        ))
public class AttendanceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    String id;

    @ManyToOne(cascade = {})
    @JoinColumn(name = "personnel_code", referencedColumnName = "code")
    Personnel personnel;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_type")
    AttendanceType type;

    @Column(name = "work_date")
    LocalDate date;

    @Column(name = "check_in")
    OffsetDateTime checkIn;

    @Column(name = "check_out")
    OffsetDateTime checkOut;

    @Builder.Default
    @Column(name = "work_hours")
    Double workHours = 0.0;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    AttendanceStatus status;

    @Column(name = "is_late")
    Boolean isLate;

    @Builder.Default
    @Column(name = "late_minutes")
    Integer lateMinutes = 0;

    @Column(name = "not_enough_hours")
    Boolean notEnoughHours;

    @Builder.Default
    @Column(name = "missing_hours")
    Double missingHours = 0.0;

    @Builder.Default
    @Column(name = "work_location")
    @Enumerated(EnumType.STRING)
    WorkLocation workLocation = WorkLocation.OFFICE;

    @Column(name = "notes")
    String notes;

    @Override
    public String toString() {
        return "AttendanceRecord{" +
                "id=" + id +
                ", date=" + date +
                ", type=" + type +
                ", status=" + status +
                ", workLocation=" + workLocation +
                ", personnelCode=" + (personnel != null ? personnel.getCode() : null) +
                '}';
    }
}
