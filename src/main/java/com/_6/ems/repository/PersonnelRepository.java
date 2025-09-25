package com._6.ems.repository;

import com._6.ems.entity.Personnel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonnelRepository extends JpaRepository<Personnel, String> {
    Optional<Personnel> findByAccount_Id(String accountId);
    Optional<Personnel> findByCode(String code);
}
