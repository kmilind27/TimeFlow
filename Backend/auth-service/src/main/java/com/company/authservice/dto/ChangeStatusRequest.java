package com.company.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeStatusRequest {
	
	@NotBlank(message = "Status is required")
	@Pattern(regexp = "ACTIVE|INACTIVE", message = "Status can be ACTIVE or INACTIVE only")
	private String status;
}
