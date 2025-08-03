package com._6.ems.entity;

import com._6.ems.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "task")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    String id;

    @Column(name = "title")
    String title;

    @Column(name = "description")
    String description;

    @Column(name = "due")
    LocalDateTime due;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    TaskStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_code")
    Employee assignee;

    @Column(name = "assigned_at")
    LocalDateTime assignedAt;

    @Column(name = "last_status_change")
    LocalDateTime lastStatusChange;

    @OneToMany(mappedBy = "task", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @Builder.Default
    List<File> files = new ArrayList<>();;
}
