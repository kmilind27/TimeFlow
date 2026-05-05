package com.company.authservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.company.authservice.model.User;
import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Long>{
	
	Optional<User> findByEmail(String email);
	
	void deleteByEmail(String email);
	
	boolean existsByEmail(String email);
	
	boolean existsByEmployeeCode(String employeeCode);
	
	Optional<User> findByEmployeeCode(String employeeCode);
	
}
