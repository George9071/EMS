package com._6.ems.entity;

import com._6.ems.enums.AttendanceStatus;
import com._6.ems.enums.AttendanceType;
import com._6.ems.enums.WorkLocation;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "personnel_code", referencedColumnName = "code")
    Personnel personnel;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_type")
    AttendanceType type;

    @Column(name = "work_date")
    LocalDate date;

    @Column(name = "check_in")
    LocalDateTime checkIn;

    @Column(name = "check_out")
    LocalDateTime checkOut;

    @Column(name = "work_hours")
    Double workHours;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    AttendanceStatus status;

    @Column(name = "is_late")
    Boolean isLate;

    @Column(name = "late_minutes")
    Integer lateMinutes;

    @Column(name = "not_enough_hours")
    Boolean notEnoughHours;

    @Column(name = "missing_hours")
    Double missingHours;

    @Column(name = "work_location")
    @Enumerated(EnumType.STRING)
    WorkLocation workLocation;

    @Column(name = "notes")
    String notes;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
