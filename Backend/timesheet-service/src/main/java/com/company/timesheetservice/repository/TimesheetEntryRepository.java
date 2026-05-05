package com.company.timesheetservice.repository;

import com.company.timesheetservice.entity.TimesheetEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimesheetEntryRepository extends JpaRepository<TimesheetEntry, Long> {

    // Get all entries for a timesheet
    List<TimesheetEntry> findByTimesheetId(Long timesheetId);

    // Check duplicate entry (same project + date)
    boolean existsByTimesheetIdAndProjectIdAndWorkDate(
            Long timesheetId,
            Long projectId,
            LocalDate workDate);

    // Find specific entry
    Optional<TimesheetEntry> findByTimesheetIdAndProjectIdAndWorkDate(
            Long timesheetId,
            Long projectId,
            LocalDate workDate);
}