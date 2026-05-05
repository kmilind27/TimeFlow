package com.company.timesheetservice.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TimesheetEntryResponse {

    private Long id;
    private Long projectId;
    private String projectName;
    private LocalDate workDate;
    @Builder.Default
    private Double hoursLogged = 0.0;
    @Builder.Default
    private String taskSummary = "No detailed summary provided";
    private LocalDateTime createdAt;
}