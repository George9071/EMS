package com._6.ems.entity;

import com._6.ems.enums.PrivilegeName;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "privilege")
public class Privilege {
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "name")
    PrivilegeName name;

    @Column(name = "description")
    String description;

    @ManyToMany
    Set<Permission> permissions;
}
