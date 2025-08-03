package com._6.ems.entity;

import com._6.ems.enums.Gender;
import com._6.ems.enums.ProjectStatus;
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
@Table(name = "project")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    String id;

    @Column(name = "name")
    String name;

    @Column(name = "description")
    String description;

    @Column(name = "max_participants")
    int maxParticipants;

    @Column(name = "participants")
    int participants;

    @Column(name = "start_date")
    LocalDate startDate;

    @Column(name = "end_date")
    LocalDate endDate;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    ProjectStatus status;

    @ManyToMany(mappedBy = "projects", fetch = FetchType.LAZY)
    List<Employee> employees;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    Department department;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    List<Task> tasks;
}
