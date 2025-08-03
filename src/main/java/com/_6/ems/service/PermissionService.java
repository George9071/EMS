package com._6.ems.service;

import com._6.ems.dto.request.PermissionRequest;
import com._6.ems.entity.Permission;
import com._6.ems.mapper.PermissionMapper;
import com._6.ems.repository.PermissionRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PermissionService {
    PermissionRepository permissionRepository;
    PermissionMapper permissionMapper;

    public Permission create(PermissionRequest request){
        return permissionRepository.save(permissionMapper.toPermission(request));
    }

    public List<Permission> getAll(){
        return permissionRepository.findAll();
    }

    public void delete(String permission){
        permissionRepository.deleteById(permission);
    }
}
