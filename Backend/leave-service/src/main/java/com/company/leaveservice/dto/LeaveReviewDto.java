package com.company.leaveservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeaveReviewDto {
	
	@NotBlank(message = "Action is required")
	@Pattern(regexp = "APPROVED|REJECTED", message = "Action must be APPROVED or REJECTED")
	private String action;
	
	private String comment;
}
