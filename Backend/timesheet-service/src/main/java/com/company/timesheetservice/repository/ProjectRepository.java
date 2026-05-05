package com.company.timesheetservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.company.timesheetservice.entity.Project;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long>{
	
	List<Project> findByIsActiveTrue();
	
	boolean existsByProjectCode(String projectCode);
	
	Optional<Project> findByProjectCode(String projectCode);
}
