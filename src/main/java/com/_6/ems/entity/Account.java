package com._6.ems.entity;

import com._6.ems.enums.Role;
import jakarta.persistence.*;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "account")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "username")
    String username;

    @Column(name = "password")
    String password;

    @Column(name = "role")
    @Enumerated(value = EnumType.STRING)
    Role role;
}