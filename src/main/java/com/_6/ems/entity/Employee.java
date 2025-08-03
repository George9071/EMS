package com._6.ems.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class Employee {
    @Id
    @Column(name = "code")
    String code;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "code")
    Personnel informationRecord;

    @Column(name = "task_completed")
    @Builder.Default
    int taskCompleted = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", referencedColumnName = "id" )
    Department department;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "employee_in_project",
            joinColumns = @JoinColumn(name = "employee_code"),
            inverseJoinColumns = @JoinColumn(name = "project_id")
    )
    @Builder.Default
    List<Project> projects = new ArrayList<>();

    @OneToMany(mappedBy = "assignee", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Builder.Default
    List<Task> tasks = new ArrayList<>();

    @OneToMany(mappedBy = "uploader", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Builder.Default
    List<File> uploadedFiles = new ArrayList<>();
}
