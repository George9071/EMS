package com._6.ems.entity;

import com._6.ems.enums.Gender;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "personnel")
public class Personnel {
    @Id
    String code;

    @Column(name = "first_name")
    String firstName;

    @Column(name = "last_name")
    String lastName;

    @Column(name = "birthday")
    LocalDate dob;

    @Column(name = "email", unique = true)
    String email;

    @Column(name = "phone")
    String phoneNumber;

    @Column(name = "city")
    String city;

    @Column(name = "street")
    String street;

    @Column(name = "description")
    String description;

    @Column(name = "skills")
    String skills;

    @Column(name = "position")
    String position;

    @Column(name = "profile_image")
    @Builder.Default
    String avatar = null;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    Gender gender;

    @OneToOne
    @JoinColumn(name = "account_id", referencedColumnName = "id")
    Account account;

    @ManyToMany
    @Enumerated(EnumType.STRING)
    Set<Privilege> privileges;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<Salary> salaryRecords = new ArrayList<>();

    @OneToMany(mappedBy = "personnel", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @Builder.Default
    List<AttendanceRecord> attendanceRecords = new ArrayList<>();
}
