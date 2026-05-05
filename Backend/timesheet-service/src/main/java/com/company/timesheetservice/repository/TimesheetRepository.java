package com.company.timesheetservice.repository;

import com.company.timesheetservice.entity.Timesheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimesheetRepository extends JpaRepository<Timesheet, Long> {

    // Find timesheet for a specific employee + week
    Optional<Timesheet> findByUserIdAndWeekStart(
            Long userId, LocalDate weekStart);

    // Get all timesheets for an employee
    List<Timesheet> findByUserIdOrderByWeekStartDesc(
            Long userId);

    // Get all SUBMITTED timesheets for manager approval
    List<Timesheet> findByStatus(String status);
    
    // Count for compliance reports
    long countByStatusAndWeekStart(String status, LocalDate weekStart);
    long countByStatus(String status);

    // Check if timesheet already exists for this week
    boolean existsByUserIdAndWeekStart(
            Long userId, LocalDate weekStart);

    // Soft delete timesheets by unassigning user ID (using negative ID to maintain uniqueness)
    @Modifying
    @Query("UPDATE Timesheet t SET t.userId = -t.userId WHERE t.userId = :userId")
    void softDeleteUserTimesheets(@Param("userId") Long userId);
}