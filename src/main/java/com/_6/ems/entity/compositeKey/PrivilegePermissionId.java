package com._6.ems.entity.compositeKey;

import lombok.*;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PrivilegePermissionId implements Serializable {
    private String privilege;
    private String permission;
}
