package com.company.leaveservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.company.leaveservice.entity.LeaveBalance;
import java.util.List;


@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
	
	// Get balance for specific type and year
	Optional<LeaveBalance> findByUserIdAndLeaveTypeAndYear(Long userId, String leaveType, Integer year);
	
	// Get all balances for an employee for a year
    List<LeaveBalance> findByUserIdAndYear(Long userId, int year);

    // Soft delete balances by unassigning user ID
    @Modifying
    @Query("UPDATE LeaveBalance lb SET lb.userId = -lb.userId WHERE lb.userId = :userId")
    void softDeleteUserBalances(@Param("userId") Long userId);
}
