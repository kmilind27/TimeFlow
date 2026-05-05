package com.company.leaveservice.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.company.leaveservice.entity.LeaveRequest;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
	
	List<LeaveRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
	
	//All pending requests for manager approval
	List<LeaveRequest> findByStatus(String status);
	long countByStatus(String status);
	
	//list of pending requests for a specific manager
	List<LeaveRequest> findByManagerIdAndStatus(Long managerId, String status);
	
	// Check if employee already has approved/submitted
    // leave that overlaps with requested dates
	@Query("SELECT l FROM LeaveRequest l "
			+ "WHERE l.userId = :userId "
			+ "AND l.status IN ('SUBMITTED', 'APPROVED') "
			+ "AND l.fromDate <= :toDate "
			+ "AND l.toDate >= :fromDate ")
	List<LeaveRequest> findOverlappingLeave(@Param("userId") Long userId,
											@Param("fromDate") LocalDate fromDate,
											@Param("toDate") LocalDate toDate);
	
	// Leave history by type
    List<LeaveRequest> findByUserIdAndLeaveType(Long userId, String leaveType);

    // Cancel all PENDING requests for a deleted user
    @Modifying
    @Query("UPDATE LeaveRequest l SET l.status = 'CANCELLED' WHERE l.userId = :userId AND l.status = 'SUBMITTED'")
    void cancelPendingLeaves(@Param("userId") Long userId);

    // Soft delete requests by unassigning user ID
    @Modifying
    @Query("UPDATE LeaveRequest l SET l.userId = -l.userId WHERE l.userId = :userId")
    void softDeleteUserRequests(@Param("userId") Long userId);

    // Grouping for reports
    @Query("SELECT l.leaveType, COUNT(l) FROM LeaveRequest l WHERE l.status = 'APPROVED' GROUP BY l.leaveType")
    List<Object[]> countApprovedByType();
}
