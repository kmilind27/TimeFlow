package com.company.timesheetservice.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TimesheetResponse {

    private Long id;
    private Long userId;
    @Builder.Default
    private String employeeName = "Name information not found";
    @Builder.Default
    private String employeeEmail = "Email information not found";
    private LocalDate weekStart;
    private LocalDate weekEnd;
    @Builder.Default
    private String status = "DRAFT";
    @Builder.Default
    private Double totalHours = 0.0;
    private LocalDateTime submittedAt;
    private String reviewComment;
    @Builder.Default
    private List<TimesheetEntryResponse> entries = new ArrayList<>();
}