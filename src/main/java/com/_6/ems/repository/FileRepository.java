package com._6.ems.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com._6.ems.entity.File;

import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<File, String>{
    List<File> findByTask_Id(String taskId);
}
