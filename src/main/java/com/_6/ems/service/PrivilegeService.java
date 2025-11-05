package com._6.ems.service;

import com._6.ems.dto.request.PrivilegeRequest;
import com._6.ems.entity.Permission;
import com._6.ems.entity.Personnel;
import com._6.ems.entity.Privilege;
import com._6.ems.entity.PrivilegePermission;
import com._6.ems.exception.AppException;
import com._6.ems.exception.ErrorCode;
import com._6.ems.mapper.PrivilegeMapper;
import com._6.ems.repository.PermissionRepository;
import com._6.ems.repository.PersonnelRepository;
import com._6.ems.repository.PrivilegeRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PrivilegeService {
    PrivilegeRepository privilegeRepository;
    PermissionRepository permissionRepository;
    PersonnelRepository personnelRepository;
    PrivilegeMapper privilegeMapper;

    public Privilege create(PrivilegeRequest request){
        var privilege = privilegeMapper.toPrivilege(request);

        var permissions = permissionRepository.findAllById(request.getPermissions());

        for (Permission permission : permissions) {
            PrivilegePermission item = PrivilegePermission.builder()
                    .privilege(privilege)
                    .permission(permission)
                    .build();

            privilege.getPermissions().add(item);
        }

        return privilegeRepository.save(privilege);
    }

    public List<Privilege> getAll(){
        return privilegeRepository.findAll();
    }

    public void delete(String name){
        Privilege privilege = privilegeRepository.findById(name)
                .orElseThrow(() -> new AppException(ErrorCode.PRIVILEGE_NOT_FOUND));

        List<Personnel> personnels = personnelRepository.findAllByPrivilegesContains(privilege);

        for (Personnel personnel : personnels) personnel.getPrivileges().remove(privilege);
        privilege.getPermissions().clear();
        privilegeRepository.deleteById(name);
    }
}
