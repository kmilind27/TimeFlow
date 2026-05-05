package com.company.leaveservice.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeaveRequestDto {
	
	@NotBlank(message = "Leave type is required")
	@Pattern(regexp = "CASUAL|SICK|EARNED|COMP_OFF", message = "Leave type must be CASUAL, SICK, EARNED, or COMP_OFF")
	private String leaveType;
	
	@NotNull(message = "From date is required")
	private LocalDate fromDate;
	
	@NotNull(message = "To date is required")
	private LocalDate toDate;
	
	@NotBlank(message = "Reason is required")
	private String reason;
}
