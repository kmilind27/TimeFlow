package com.company.timesheetservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewRequest {

    @NotBlank(message = "Action is required")
    @Pattern(
        regexp = "APPROVED|REJECTED",
        message = "Action must be APPROVED or REJECTED"
    )
    private String action;

    // Mandatory when rejecting
    private String comment;
}