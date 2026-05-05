package com.company.leaveservice.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.company.leaveservice.client.AuthServiceClient;
import com.company.leaveservice.dto.HolidayDto;
import com.company.leaveservice.dto.LeaveBalanceDto;
import com.company.leaveservice.dto.LeaveRequestDto;
import com.company.leaveservice.dto.LeaveResponseDto;
import com.company.leaveservice.dto.LeaveReviewDto;
import com.company.leaveservice.dto.UserResponse;
import com.company.leaveservice.entity.Holiday;
import com.company.leaveservice.entity.LeaveBalance;
import com.company.leaveservice.entity.LeaveRequest;
import com.company.leaveservice.event.EventPublisher;
import com.company.leaveservice.event.LeaveStatusEvent;
import com.company.leaveservice.exception.InvalidOperationException;
import com.company.leaveservice.repository.HolidayRepository;
import com.company.leaveservice.repository.LeaveBalanceRepository;
import com.company.leaveservice.repository.LeaveRequestRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LeaveService {
	
	private final LeaveRequestRepository leaveRequestRepository;
	private final LeaveBalanceRepository leaveBalanceRepository;
	private final HolidayRepository holidayRepository;
	private final AuthServiceClient authServiceClient;
	private final EventPublisher eventPublisher;
	private static final String SUBMITTED = "SUBMITTED";
	
	//Apply for leave
	@Transactional
	public LeaveResponseDto applyLeave(Long userId, LeaveRequestDto request) {
		
		//fromDate must be before toDate
		if(request.getFromDate().isAfter(request.getToDate())) {
			
			throw new InvalidOperationException(
	                "From date cannot be after to date");
		}
		
		//cannot apply for past dates
		if(request.getFromDate().isBefore(LocalDate.now())) {
			throw new InvalidOperationException(
	                "Cannot apply leave for past dates");
		}
		
		//check for overlapping leave
		
		List<LeaveRequest> overlapping = leaveRequestRepository.findOverlappingLeave(userId,
																		request.getFromDate(), request.getToDate());
		
		if(!overlapping.isEmpty()) {
			throw new InvalidOperationException(
	                "Leave dates overlap with an existing leave request");
		}
		
		//Calculate working days (excluding weekends and holidays)
		double workingDays = calculateWorkingDays(
							request.getFromDate(),
							request.getToDate());
		
		if(workingDays == 0) {
			throw new InvalidOperationException(
	                "Selected dates have no working days " +
	                "(weekends/holidays only)");
		}
		
		//check leave balance
		int year = request.getFromDate().getYear();
		
		LeaveBalance balance = leaveBalanceRepository
							   .findByUserIdAndLeaveTypeAndYear(userId, request.getLeaveType(), year)
							   .orElseThrow(() -> new RuntimeException(
									   "No leave balance for "+request.getLeaveType()+" in year "
									   +year+". Please contact HR."));
		
		if(balance.getRemainingDays() < workingDays) {
			throw new InvalidOperationException(
	                "Insufficient leave balance. " +
	                "Available: " + balance.getRemainingDays() +
	                " days, Required: " + workingDays + " days");
		}
		
		LeaveRequest leaveRequest = LeaveRequest.builder()
									.userId(userId)
									.leaveType(request.getLeaveType())
									.fromDate(request.getFromDate())
									.toDate(request.getToDate())
									.totalDays(workingDays)
									.reason(request.getReason())
									.status(SUBMITTED)
									.build();
		LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
		
		eventPublisher.publishLeaveApplied(LeaveStatusEvent.builder()
		        .leaveId(saved.getId())
		        .userId(saved.getUserId())
		        .userEmail(authServiceClient.getUserById(userId).getEmail())
		        .leaveType(saved.getLeaveType())
		        .status("APPLIED")
		        .startDate(saved.getFromDate().toString())
		        .endDate(saved.getToDate().toString())
		        .build());
		
		return mapToResponse(saved);
	}
	
	//Get my leave history
	public List<LeaveResponseDto> getMyLeaveHistory(Long userId){
		
		return leaveRequestRepository.findByUserIdOrderByCreatedAtDesc(userId)
				.stream()
				.map(this::mapToResponse)
				.toList();
	}
	
	//Get My balance
	public List<LeaveBalanceDto> getMyBalances(Long userId){
		
		int currentYear = LocalDate.now().getYear();
		
		return leaveBalanceRepository.findByUserIdAndYear(userId, currentYear)
				.stream()
				.map(this::mapToBalanceDto)
				.toList();
	}
	
	//Cancel leave
	@Transactional
	public LeaveResponseDto cancelLeave(Long userId, Long leaveId) {
		
		LeaveRequest leave = leaveRequestRepository.findById(leaveId)
							.orElseThrow(() -> new RuntimeException(
			                        "Leave request not found"));
		
		//Only owner can cancel
		if(!leave.getUserId().equals(userId)) {
			throw new InvalidOperationException(
			                "You can only cancel your own leave");
		}
		
		//cannot cancel already processed leave
		if("REJECTED".equals(leave.getStatus())
                || "CANCELLED".equals(leave.getStatus())) {
			
			throw new InvalidOperationException(
	                "Cannot cancel a " +
	                leave.getStatus() + " leave request");
		}
		
		// Cannot cancel past approved leave
        if ("APPROVED".equals(leave.getStatus())
                && leave.getFromDate()
                        .isBefore(LocalDate.now())) {
            throw new InvalidOperationException(
                "Cannot cancel leave that has " +
                "already started");
        }
        
        leave.setStatus("CANCELLED");
        leaveRequestRepository.save(leave);
        
        return mapToResponse(leave);
		
	}
	
	//Manager review
	@Transactional
	public LeaveResponseDto reviewLeave(Long leaveId, Long managerId, LeaveReviewDto request) {
		
		LeaveRequest leave = leaveRequestRepository
                .findById(leaveId)
                .orElseThrow(() -> new RuntimeException(
                        "Leave request not found"));
		
		//Only SUBMITTED leaves can be reviewed
        if (!SUBMITTED.equals(leave.getStatus())) {
            throw new InvalidOperationException(
                "Only SUBMITTED leave can be reviewed. " +
                "Current status: " + leave.getStatus());
        }
        
     // Comment mandatory for rejection
        if ("REJECTED".equals(request.getAction())
                && (request.getComment() == null
                    || request.getComment().isBlank())) {
            throw new InvalidOperationException(
                "Comment is mandatory when rejecting");
        }
        
        leave.setStatus(request.getAction());
        leave.setManagerId(managerId);
        leave.setManagerComment(request.getComment());
        leave.setReviewedAt(LocalDateTime.now());
        leaveRequestRepository.save(leave);
        

        // ✅ Deduct balance on approval
        if ("APPROVED".equals(request.getAction())) {
            deductLeaveBalance(
                leave.getUserId(),
                leave.getLeaveType(),
                leave.getFromDate().getYear(),
                leave.getTotalDays());
        }
        
        return mapToResponse(leave);
        
	}
	
	//get pending requests
	public List<LeaveResponseDto> getPendingRequests(){
		
		return leaveRequestRepository.findByStatus(SUBMITTED)
				.stream()
				.map(this::mapToResponse)
				.toList();
	}

    // Soft Delete leave data (Used by RabbitMQ listener when user is deleted)
    @Transactional
    public void softDeleteUserData(Long userId) {
        // 1. Cancel pending requests
        leaveRequestRepository.cancelPendingLeaves(userId);
        // 2. Unassign requests (using negative ID)
        leaveRequestRepository.softDeleteUserRequests(userId);
        // 3. Unassign balances (using negative ID)
        leaveBalanceRepository.softDeleteUserBalances(userId);
    }

    // Admin reporting: Leave consumption by type
    public Map<String, Long> getConsumptionStats() {
        List<Object[]> results = leaveRequestRepository.countApprovedByType();
        Map<String, Long> stats = new HashMap<>();
        for (Object[] result : results) {
            stats.put((String) result[0], (Long) result[1]);
        }
        return stats;
    }

    // For Employee Dashboard Summary
    public String getNextHoliday() {
        return holidayRepository.findFirstByHolidayDateAfterOrderByHolidayDateAsc(LocalDate.now())
                .map(h -> h.getHolidayName() + " (" + h.getHolidayDate() + ")")
                .orElse("No upcoming holidays");
    }

    public long getCountByStatus(String status) {
        return leaveRequestRepository.countByStatus(status);
    }
	
	//Holidays
	public List<Holiday> getHolidays(int year){
		
		return holidayRepository.findByYear(year);
	}
	
	@Transactional
	public Holiday addHoliday(HolidayDto dto) {
		
		if(holidayRepository.existsByHolidayDate(dto.getHolidayDate())) {
			
			throw new InvalidOperationException(
	                "Holiday already exists for date: " +
	                        dto.getHolidayDate());
		}
		
		Holiday holiday = Holiday.builder()
							.holidayDate(dto.getHolidayDate())
							.holidayName(dto.getHolidayName())
							.holidayType(dto.getHolidayType())
							.build();
		
		return holidayRepository.save(holiday);
	}
	
	//helpers
	
	private double calculateWorkingDays(
            LocalDate from, LocalDate to) {
        double days = 0;
        LocalDate current = from;

        while (!current.isAfter(to)) {
            // Skip weekends
            if (current.getDayOfWeek() != DayOfWeek.SATURDAY
                && current.getDayOfWeek()
                          != DayOfWeek.SUNDAY && !holidayRepository
                        .existsByHolidayDate(current)) {
                    days += 1;
                
            }
            current = current.plusDays(1);
        }
	
        return days;
    }
	
    @Transactional
    public void initializeLeaveBalances(Long userId) {
        int year = LocalDate.now().getYear();

        // Check if balances already exist to ensure idempotency
        if (!leaveBalanceRepository.findByUserIdAndYear(userId, year).isEmpty()) {
            return;
        }

        List<LeaveBalance> defaultBalances = List.of(
            createDefaultBalance(userId, "CASUAL", 12.0, year),
            createDefaultBalance(userId, "SICK", 10.0, year),
            createDefaultBalance(userId, "EARNED", 15.0, year),
            createDefaultBalance(userId, "COMP_OFF", 5.0, year)
        );

        leaveBalanceRepository.saveAll(defaultBalances);
    }

    private LeaveBalance createDefaultBalance(Long userId, String type, Double days, int year) {
        return LeaveBalance.builder()
                .userId(userId)
                .leaveType(type)
                .totalDays(days)
                .usedDays(0.0)
                .remainingDays(days)
                .year(year)
                .build();
    }

	private void deductLeaveBalance(
            Long userId,
            String leaveType,
            int year,
            double days) {
        LeaveBalance balance = leaveBalanceRepository
            .findByUserIdAndLeaveTypeAndYear(
                userId, leaveType, year)
            .orElseThrow(() -> new RuntimeException(
                "Balance not found"));

        balance.setUsedDays(balance.getUsedDays() + days);
        balance.setRemainingDays(
            balance.getTotalDays() - balance.getUsedDays());
        leaveBalanceRepository.save(balance);
    }
	
	 // ─── Mappers ───────────────────────────────────────

    private LeaveResponseDto mapToResponse(
            LeaveRequest l) {
    	// ✅ Fetch user details from Auth Service
        UserResponse user = null;
        try {
            user = authServiceClient
                    .getUserById(l.getUserId());
        } catch (Exception e) {
            // Auth Service unavailable — use defaults
        }

        return LeaveResponseDto.builder()
                .id(l.getId())
                .userId(l.getUserId())
                .employeeName(user != null && user.getFullName() != null
                        ? user.getFullName()
                        : "Name information not found")
                .employeeEmail(user != null && user.getEmail() != null
                        ? user.getEmail()
                        : "Email information not found")
                .leaveType(l.getLeaveType() != null ? l.getLeaveType() : "Type not specified")
                .fromDate(l.getFromDate())
                .toDate(l.getToDate())
                .totalDays(l.getTotalDays() != null ? l.getTotalDays() : 0.0)
                .reason(l.getReason() != null ? l.getReason() : "No reason specified")
                .status(l.getStatus() != null ? l.getStatus() : SUBMITTED)
                .managerComment(l.getManagerComment() != null ? l.getManagerComment() : "No review comment yet")
                .reviewedAt(l.getReviewedAt())
                .createdAt(l.getCreatedAt())
                .build();
    }

    private LeaveBalanceDto mapToBalanceDto(
            LeaveBalance b) {
        return LeaveBalanceDto.builder()
                .id(b.getId())
                .leaveType(b.getLeaveType())
                .totalDays(b.getTotalDays())
                .usedDays(b.getUsedDays())
                .remainingDays(b.getRemainingDays())
                .year(b.getYear())
                .build();
    }
}
