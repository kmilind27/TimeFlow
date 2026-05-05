package com.company.timesheetservice.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TimesheetEntryRequest {
	
	@NotNull(message = "Project Id is required")
	private Long projectId;
	
	@NotNull(message = "Work Date is required")
	private LocalDate workDate;
	
	@NotNull(message = "Hours Logged is required")
	@DecimalMin(value = "0.5", message = "Minimum 0.5 hours per entry")
	@DecimalMax(value = "24.0",  message = "Cannot exceed 24 hours per day")
	private Double hoursLogged;
	
	private String taskSummary;
}
