package com.company.leaveservice.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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

import com.company.leaveservice.dto.HolidayDto;
import com.company.leaveservice.dto.LeaveBalanceDto;
import com.company.leaveservice.dto.LeaveRequestDto;
import com.company.leaveservice.dto.LeaveResponseDto;
import com.company.leaveservice.dto.LeaveReviewDto;
import com.company.leaveservice.entity.Holiday;
import com.company.leaveservice.service.LeaveService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/leave")
@RequiredArgsConstructor
@Tag(name = "Leave", description = "Leave management APIs")
public class LeaveController {
	
	private final LeaveService leaveService;
	
	//Employee APIs
	
	@Operation(summary = "Apply for leave", description = "Submit a new leave request")
	@PostMapping("/apply")
	public  ResponseEntity<LeaveResponseDto> applyLeave(
			@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
			@Valid @RequestBody LeaveRequestDto request){
		
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(leaveService.applyLeave(userId, request));
	}
	
	@Operation(summary = "Get my leave requests", description = "Fetch leave history of the logged-in user")
	@GetMapping("/my-requests")
	public ResponseEntity<List<LeaveResponseDto>> getMyRequests(
			@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId){
		
		return ResponseEntity.ok(leaveService.getMyLeaveHistory(userId));
	}
	
	@Operation(summary = "Get my leave balance", description = "Fetch remaining leave balance for current year")
	@GetMapping("/my-balance")
	public ResponseEntity<List<LeaveBalanceDto>> getMyBalance(
			@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId){
		
		return ResponseEntity.ok(leaveService.getMyBalances(userId));
	}
	
	@Operation(summary = "Cancel leave request", description = "Cancel a submitted or approved leave request")
	@PutMapping("/cancel/{leaveId}")
	public ResponseEntity<LeaveResponseDto> cancelLeave(
			@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
			@PathVariable Long leaveId){
		
		return ResponseEntity.ok(leaveService.cancelLeave(userId, leaveId));
	}
	
	//Manager APIs
	
	@Operation(summary = "Get team calendar", description = "[Manager] View aggregated team leave plans and holidays")
	@GetMapping("/team-calendar")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
	public ResponseEntity<Map<String, Object>> getTeamCalendar(
			@Parameter(hidden = true) @RequestHeader("X-User-Role") String role,
			@RequestParam(required = false) String teamId,
			@RequestParam(required = false) String month,
			@RequestParam(required = false) String year) {
		
		return ResponseEntity.ok(Map.of(
				"teamId", teamId != null ? teamId : "DEFAULT",
				"holidays", List.of(),
				"approvedLeaves", List.of(),
				"message", "Team calendar data aggregated successfully"
		));
	}
	
	@Operation(summary = "Get pending leave requests", description = "[Manager/Admin] Fetch all leaves pending review")
	@GetMapping("/manager/pending")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
	public ResponseEntity<List<LeaveResponseDto>> getPendingRequests(
			@Parameter(hidden = true) @RequestHeader("X-User-Role") String role){
		
		
		
		return ResponseEntity.ok(leaveService.getPendingRequests());
	}
	
	@Operation(summary = "Review leave request", description = "[Manager/Admin] Approve or reject a leave request")
	@PutMapping("/manager/review/{leaveId}")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
	public ResponseEntity<LeaveResponseDto> reviewLeave(
			@Parameter(hidden = true) @RequestHeader("X-User-Id") Long managerId,
			@Parameter(hidden = true) @RequestHeader("X-User-Role") String role,
			@PathVariable Long leaveId,
			@Valid @RequestBody LeaveReviewDto request){
		
		
		return ResponseEntity.ok(leaveService.reviewLeave(leaveId, managerId, request));
		
	}
	
	//Holiday APIs
	
	@Operation(summary = "Get holidays", description = "Fetch list of holidays for a given year")
	@GetMapping("/holidays")
	public ResponseEntity<List<Holiday>> getHolidays(@RequestParam(defaultValue = "0") int year){
		
		if(year == 0) {
			year = LocalDate.now().getYear();
		}
		
		return ResponseEntity.ok(leaveService.getHolidays(year));
	}
	
    @Operation(summary = "Add holiday", description = "[Internal] Add a new holiday (Called by admin-service)", hidden = true)
    @PostMapping("/holidays")
    public ResponseEntity<Holiday> addHoliday(
            @Parameter(hidden = true) @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody HolidayDto request){
    	
        
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveService.addHoliday(request));
    }
	
    @Operation(summary = "Get approved counts by type", description = "[Internal] Aggregated data for consumption report", hidden = true)
    @GetMapping("/internal/consumption")
    public ResponseEntity<Map<String, Long>> getConsumptionStats() {
        return ResponseEntity.ok(leaveService.getConsumptionStats());
    }

    @Operation(summary = "Get next holiday", description = "[Internal] Aggregated data for dashboard summary", hidden = true)
    @GetMapping("/internal/next-holiday")
    public ResponseEntity<String> getNextHoliday() {
        return ResponseEntity.ok(leaveService.getNextHoliday());
    }

    @Operation(summary = "Get leave count by status", description = "[Internal] Fetch count for dashboard", hidden = true)
    @GetMapping("/internal/count")
    public ResponseEntity<Long> getCountByStatus(@RequestParam String status) {
        return ResponseEntity.ok(leaveService.getCountByStatus(status));
    }
}
