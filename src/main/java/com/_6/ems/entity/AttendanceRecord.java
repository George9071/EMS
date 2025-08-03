package com._6.ems.entity;

import com._6.ems.enums.AttendanceType;
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
@Table(name = "attendance_record")
public class AttendanceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    String id;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "personnel_code", referencedColumnName = "code")
    Personnel personnel;

    @Enumerated(EnumType.STRING)
    AttendanceType type;

    @Column(name = "work_date")
    LocalDate date;

    @Column(name = "check_in")
    LocalDateTime checkIn;

    @Column(name = "check_out")
    LocalDateTime checkOut;
}
