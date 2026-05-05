package com.company.timesheetservice.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.company.timesheetservice.dto.ProjectResponse;
import com.company.timesheetservice.dto.ReviewRequest;
import com.company.timesheetservice.dto.TimesheetEntryRequest;
import com.company.timesheetservice.dto.TimesheetEntryResponse;
import com.company.timesheetservice.dto.TimesheetResponse;
import com.company.timesheetservice.service.TimesheetService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/timesheet")
@RequiredArgsConstructor
@Tag(name = "Timesheet", description = "Timesheet management APIs")
public class TimesheetController {
	
	private final TimesheetService timesheetService;
	
	//Projects
	
	@Operation(summary = "Get active projects", description = "Fetch all active projects available for time logging")
	@GetMapping("/projects")
	public ResponseEntity<List<ProjectResponse>> getActiveProjects(){
		
		return ResponseEntity.ok(timesheetService.getAllActiveProjects());
	}
	
	//LogEntry
	
	@Operation(summary = "Log time entry", description = "Log hours for a project on a specific date")
	@PostMapping("/entries")
	public ResponseEntity<TimesheetEntryResponse> logEntry(
			@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
			@Valid @RequestBody TimesheetEntryRequest request){
		TimesheetEntryResponse response = timesheetService.logEntry(userId, request);
		
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
	
	//Get Weekly timesheet
	
	@Operation(summary = "Get weekly timesheet", description = "Fetch timesheet for a specific week by start date")
	@GetMapping("/weeks/{weekStart}")
	public ResponseEntity<TimesheetResponse> getWeeklyTimesheet(
			@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
			@PathVariable @DateTimeFormat(iso = ISO.DATE) LocalDate weekStart){
		return ResponseEntity.ok(timesheetService.getWeeklyTimesheet(userId, weekStart));
	}
	
	//Get All My Timesheets
	
	@Operation(summary = "Get my timesheets", description = "Fetch all timesheets of the logged-in user")
	@GetMapping("/my-timesheets")
	public ResponseEntity<List<TimesheetResponse>> getMyTimesheets(
			@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId){
		
		return ResponseEntity.ok(timesheetService.getAllTimesheet(userId));
	}
	
	//Submit Timesheet
	
	@Operation(summary = "Submit timesheet", description = "Submit a draft timesheet for manager approval")
	@PostMapping("/weeks/{weekStart}/submit")
	public ResponseEntity<TimesheetResponse> submitTimesheet(
			@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId, 
			@PathVariable @DateTimeFormat(iso = ISO.DATE) LocalDate weekStart){
		
	    
		return ResponseEntity.ok(timesheetService.submitTimesheet(userId, weekStart));
	}
	
	//Manager APIs
	
	@Operation(summary = "Get pending timesheets", description = "[Manager/Admin] Fetch all timesheets pending review")
	@GetMapping("/manager/pending")
	@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
	public ResponseEntity<List<TimesheetResponse>> getPendingTimesheets(
			@Parameter(hidden = true) @RequestHeader("X-User-Role") String role){
		
		
		return ResponseEntity.ok(timesheetService.getPendingTimesheets());
	}
	
	@Operation(summary = "Review timesheet", description = "[Manager/Admin] Approve or reject a submitted timesheet")
	@PutMapping("/manager/review/{timesheetId}")
	@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
	public ResponseEntity<TimesheetResponse> reviewTimesheet(
			@Parameter(hidden = true) @RequestHeader("X-User-Id") Long managerId,
			@Parameter(hidden = true) @RequestHeader("X-User-Role") String role,
			@PathVariable Long timesheetId,
			@Valid @RequestBody ReviewRequest request){
		
		
		return ResponseEntity.ok(timesheetService.reviewTimesheet(timesheetId, managerId, request));
	}

    @Operation(summary = "Get submitted count", description = "[Internal] Fetch submitted count for compliance", hidden = true)
    @GetMapping("/internal/compliance")
    public ResponseEntity<Long> getSubmittedCount(
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate weekStart) {
        return ResponseEntity.ok(timesheetService.getSubmittedCount(weekStart));
    }

    @Operation(summary = "Get timesheet count by status", description = "[Internal] Fetch count for dashboard", hidden = true)
    @GetMapping("/internal/count")
    public ResponseEntity<Long> getCountByStatus(@RequestParam String status) {
        return ResponseEntity.ok(timesheetService.getCountByStatus(status));
    }
}
