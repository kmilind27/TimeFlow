package com.company.timesheetservice.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.company.timesheetservice.client.AuthServiceClient;
import com.company.timesheetservice.dto.ProjectResponse;
import com.company.timesheetservice.dto.ReviewRequest;
import com.company.timesheetservice.dto.TimesheetEntryRequest;
import com.company.timesheetservice.dto.TimesheetEntryResponse;
import com.company.timesheetservice.dto.TimesheetResponse;
import com.company.timesheetservice.dto.UserResponse;
import com.company.timesheetservice.entity.Project;
import com.company.timesheetservice.entity.Timesheet;
import com.company.timesheetservice.entity.TimesheetEntry;
import com.company.timesheetservice.event.EventPublisher;
import com.company.timesheetservice.event.TimesheetStatusEvent;
import com.company.timesheetservice.exception.InvalidOperationException;
import com.company.timesheetservice.exception.ResourceNotFoundException;
import com.company.timesheetservice.repository.ProjectRepository;
import com.company.timesheetservice.repository.TimesheetEntryRepository;
import com.company.timesheetservice.repository.TimesheetRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TimesheetService {
	
	private final TimesheetRepository timesheetRepository;
	private final TimesheetEntryRepository entryRepository;
	private final ProjectRepository projectRepository;
    private final AuthServiceClient authServiceClient;
    private final EventPublisher eventPublisher;
    
    private static final String SUBMITTED = "SUBMITTED";
    private static final String DRAFT = "DRAFT";
    
	// Maximum hours allowed per day
    private static final double MAX_DAILY_HOURS = 12.0;
    
    public List<ProjectResponse> getAllActiveProjects(){
    	
    	return projectRepository.findByIsActiveTrue().stream()
    			.map(this::mapToProjectResponse)
    			.toList();
    }
    
    public TimesheetEntryResponse logEntry(Long userId, TimesheetEntryRequest request) {
    	
    	//Cannot log entry for future dates
    	if(request.getWorkDate().isAfter(LocalDate.now())) {
    		throw new InvalidOperationException("Cannot log hours for future dates");
    	}
    	
    	//validate project exists
    	Project project = projectRepository.findById(request.getProjectId())
    			.orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    	
    	
    	if(Boolean.FALSE.equals(project.getIsActive())){
    		throw new InvalidOperationException("Project is inactive");
    	}
    	
    	//get or create timesheet for this week
    	LocalDate weekStart = getWeekStart(request.getWorkDate());
    	LocalDate weekEnd = weekStart.plusDays(6);
    	
    	Timesheet timesheet = timesheetRepository.findByUserIdAndWeekStart(userId, weekStart)
    			.orElseGet(() -> createNewTimesheet(
                        userId, weekStart, weekEnd));
    	
    	// RULE 4: Cannot add entry to submitted/approved timesheet
        if (SUBMITTED.equals(timesheet.getStatus())
                || "APPROVED".equals(timesheet.getStatus())) {
            throw new InvalidOperationException(
                "Cannot modify a " + timesheet.getStatus()
                + " timesheet");
        }
        
        //check duplicate entries
        
        if(entryRepository.existsByTimesheetIdAndProjectIdAndWorkDate(timesheet.getId(), project.getId(), request.getWorkDate())) {
        	
        	throw new InvalidOperationException("Entry already exists for this project and date");
        }
        
        //check daily hours limit
        Double existingHours = entryRepository.findByTimesheetId(timesheet.getId())
        		.stream()
        		.filter(e -> e.getWorkDate().equals(request.getWorkDate()))
        		.mapToDouble(TimesheetEntry::getHoursLogged)
        		.sum();
        
        if (existingHours + request.getHoursLogged() > MAX_DAILY_HOURS) {
        	throw new InvalidOperationException(
        			"Total hours for " + request.getWorkDate() + " would exceed " + MAX_DAILY_HOURS + " hours");
        }
        
        //save entry
        TimesheetEntry entry = TimesheetEntry.builder()
        						.timesheet(timesheet)
        						.project(project)
        						.workDate(request.getWorkDate())
        						.hoursLogged(request.getHoursLogged())
        						.taskSummary(request.getTaskSummary())
        						.build();
        
        TimesheetEntry savedEntry = entryRepository.save(entry);
        
        updateTotalHours(timesheet);
        
        return mapToEntryResponse(savedEntry);
        
    }
    
    //get weekly timesheet
    public TimesheetResponse getWeeklyTimesheet(Long userId, LocalDate weekStart) {
    	
    	Timesheet timesheet = timesheetRepository.findByUserIdAndWeekStart(userId, weekStart)
    							.orElseThrow(() -> new  ResourceNotFoundException("No timesheet found for week: "
    																	+ weekStart));
    	
    	return mapToTimesheetResponse(timesheet);
    }
    
    //get all timesheets
    public List<TimesheetResponse> getAllTimesheet(Long userId){
    	
    	return timesheetRepository.findByUserIdOrderByWeekStartDesc(userId)
    			.stream()
    			.map(this::mapToTimesheetResponse)
    			.toList();
    }
    
    //submit timesheet
    @Transactional
    public TimesheetResponse submitTimesheet(Long userId, LocalDate weekStart) {
    	
    	Timesheet timesheet = timesheetRepository.findByUserIdAndWeekStart(userId, weekStart)
    							.orElseThrow(() -> new ResourceNotFoundException("No timesheet found for week: "
    																		+weekStart));
    	
    	//Only DRAFT timesheets can be submitted
    	if(!DRAFT.equals(timesheet.getStatus())) {
    		throw new InvalidOperationException(
    				"Only DRAFT timesheets can be submitted. Current status: "+timesheet.getStatus());
    	}
    	
    	//Must have at least one entry
    	List<TimesheetEntry> entries = entryRepository.findByTimesheetId(timesheet.getId());
    	
    	if(entries.isEmpty()) {
    		throw new InvalidOperationException("Cannot submit empty timesheet");
    	}
    	
    	timesheet.setStatus(SUBMITTED);
    	timesheet.setSubmittedAt(LocalDateTime.now());
    	timesheetRepository.save(timesheet);
    	
    	eventPublisher.publishTimesheetSubmitted(TimesheetStatusEvent.builder()
    	        .timesheetId(timesheet.getId())
    	        .userId(timesheet.getUserId())
    	        .userEmail(authServiceClient.getUserById(userId).getEmail())
    	        .status(SUBMITTED)
    	        .weekStart(timesheet.getWeekStart().toString())
    	        .build());
    	
    	return mapToTimesheetResponse(timesheet);
    }
    
    //Manager review
    @Transactional
    public TimesheetResponse reviewTimesheet(Long timesheetId, Long managerId, ReviewRequest request) {
    	
    	Timesheet timesheet = timesheetRepository.findById(timesheetId)
    							.orElseThrow(() -> 
    							new ResourceNotFoundException("Timesheet not found"));
    	
    	//Only SUBMITTED timesheets can be reviewed
    	if(!SUBMITTED.equals(timesheet.getStatus())) {
    		throw new InvalidOperationException("Only SUBMITTED timesheets can be reviewed");
    	}
    	
    	// ✅ Comment mandatory for rejection
        if ("REJECTED".equals(request.getAction())
                && (request.getComment() == null
                    || request.getComment().isBlank())) {
            throw new InvalidOperationException(
                "Comment is mandatory when rejecting");
        }
        
        timesheet.setStatus(request.getAction());
        timesheet.setReviewedBy(managerId);
        timesheet.setReviewComment(request.getComment());
        timesheet.setReviewedAt(LocalDateTime.now());
        
        if("REJECTED".equals(request.getAction())) {
        	timesheet.setStatus(DRAFT);
        }
        
        timesheetRepository.save(timesheet);
        
        return mapToTimesheetResponse(timesheet);
    }
    
    //Get pending timsheets(manager)
    public List<TimesheetResponse> getPendingTimesheets(){
    	
    	return timesheetRepository.findByStatus(SUBMITTED)
    			.stream()
    			.map(this::mapToTimesheetResponse)
    			.toList();
    }

    // Get count of submitted/approved timesheets for a week (Admin Compliance)
    public long getSubmittedCount(LocalDate weekStart) {
        long submitted = timesheetRepository.countByStatusAndWeekStart(SUBMITTED, weekStart);
        long approved = timesheetRepository.countByStatusAndWeekStart("APPROVED", weekStart);
        return submitted + approved;
    }

    public long getCountByStatus(String status) {
        return timesheetRepository.countByStatus(status);
    }

    // Soft Delete timesheets (Used by RabbitMQ listener when user is deleted)
    @Transactional
    public void softDeleteUserData(Long userId) {
        timesheetRepository.softDeleteUserTimesheets(userId);
    }

    // ─── Private Helpers ───────────────────────────────

    private Timesheet createNewTimesheet(Long userId, LocalDate weekStart, LocalDate weekEnd) {
        Timesheet timesheet = Timesheet.builder()
                .userId(userId)
                .weekStart(weekStart)
                .weekEnd(weekEnd)
                .status(DRAFT)
                .totalHours(0.0)
                .build();
        return timesheetRepository.save(timesheet);
    }

    private void updateTotalHours(Timesheet timesheet) {
        double total = entryRepository
                .findByTimesheetId(timesheet.getId())
                .stream()
                .mapToDouble(TimesheetEntry::getHoursLogged)
                .sum();
        timesheet.setTotalHours(total);
        timesheetRepository.save(timesheet);
    }
    
    private LocalDate getWeekStart(LocalDate date) {
        return date.with(DayOfWeek.MONDAY);
    }
    
 // ─── Mappers ───────────────────────────────────────

    private TimesheetResponse mapToTimesheetResponse(Timesheet t) {
    	
        List<TimesheetEntryResponse> entries =
                entryRepository
                        .findByTimesheetId(t.getId())
                        .stream()
                        .map(this::mapToEntryResponse)
                        .toList();

     // ✅ Fetch user details from Auth Service
        UserResponse user = null;
        try {
            user = authServiceClient
                    .getUserById(t.getUserId());
        } catch (Exception e) {
            // Auth Service unavailable — use defaults
        }

        return TimesheetResponse.builder()
                .id(t.getId())
                .userId(t.getUserId())
                // ✅ Use name from Auth Service with defaults
                .employeeName(user != null && user.getFullName() != null
                        ? user.getFullName()
                        : "Name information not found")
                .employeeEmail(user != null && user.getEmail() != null
                        ? user.getEmail()
                        : "Email information not found")
                .weekStart(t.getWeekStart())
                .weekEnd(t.getWeekEnd())
                .status(t.getStatus() != null ? t.getStatus() : DRAFT)
                .totalHours(t.getTotalHours() != null ? t.getTotalHours() : 0.0)
                .submittedAt(t.getSubmittedAt())
                .reviewComment(t.getReviewComment())
                .entries(entries)
                .build();
    
    }

    private TimesheetEntryResponse mapToEntryResponse(
            TimesheetEntry e) {
        return TimesheetEntryResponse.builder()
                .id(e.getId())
                .projectId(e.getProject().getId())
                .projectName(e.getProject().getProjectName())
                .workDate(e.getWorkDate())
                .hoursLogged(e.getHoursLogged() != null ? e.getHoursLogged() : 0.0)
                .taskSummary(e.getTaskSummary() != null ? e.getTaskSummary() : "No detailed summary provided")
                .createdAt(e.getCreatedAt())
                .build();
    }

    private ProjectResponse mapToProjectResponse(
            Project p) {
        return ProjectResponse.builder()
                .id(p.getId())
                .projectCode(p.getProjectCode())
                .projectName(p.getProjectName())
                .isActive(p.getIsActive())
                .build();
    }
}
