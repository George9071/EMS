package com._6.ems.entity;

import com._6.ems.enums.Gender;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
@Table(name = "file")
public class File {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    String id;

    @Column(name = "file_name")
    String name;

    @Column(name = "file_type")
    String type;

    @Column(name = "file_url")
    String url;

    @Column(name = "upload_date", updatable = false)
    LocalDateTime uploadTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_code", nullable = false)
    Employee uploader;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    Task task;
}
