package com._6.ems.service;

import com._6.ems.dto.request.PrivilegeRequest;
import com._6.ems.entity.Privilege;
import com._6.ems.mapper.PrivilegeMapper;
import com._6.ems.repository.PermissionRepository;
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
    PrivilegeMapper privilegeMapper;

    public Privilege create(PrivilegeRequest request){
        var privilege = privilegeMapper.toPrivilege(request);
        var permissions = permissionRepository.findAllById(request.getPermissions());
        privilege.setPermissions(new HashSet<>(permissions));;

        return privilegeRepository.save(privilege);
    }

    public List<Privilege> getAll(){
        return privilegeRepository.findAll();
    }

    public void delete(String role){
        privilegeRepository.deleteById(role);
    }
}
