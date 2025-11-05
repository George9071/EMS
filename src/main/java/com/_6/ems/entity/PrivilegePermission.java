package com._6.ems.entity;

import com._6.ems.entity.compositeKey.PrivilegePermissionId;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "privilege_permissions")
@IdClass(PrivilegePermissionId.class)
public class PrivilegePermission {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "privilege_name", referencedColumnName = "name")
    Privilege privilege;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permissions_name", referencedColumnName = "name")
    Permission permission;
}
