package com._6.ems.repository;

import com._6.ems.entity.Privilege;
import com._6.ems.enums.PrivilegeName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrivilegeRepository extends JpaRepository<Privilege, String> {
    Optional<Privilege> findByName(PrivilegeName name);
}
