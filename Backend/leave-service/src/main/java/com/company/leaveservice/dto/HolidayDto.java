package com.company.leaveservice.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HolidayDto {
	
	@NotNull(message = "Holiday date is required")
	private LocalDate holidayDate;
	
	@NotBlank(message = "Holiday name is required")
	private String holidayName;
	
	// NATIONAL / OPTIONAL
	private String holidayType = "NATIONAL";
}
