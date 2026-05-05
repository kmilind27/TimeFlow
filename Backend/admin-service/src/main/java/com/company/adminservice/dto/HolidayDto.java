package com.company.adminservice.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HolidayDto {
    
    @NotNull(message = "Holiday date is required")
    private LocalDate holidayDate;
    
    @NotBlank(message = "Holiday name is required")
    private String holidayName;
    
    @Builder.Default
    private String holidayType = "OPTIONAL";
}
