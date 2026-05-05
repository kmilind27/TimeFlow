package com.company.leaveservice.dto;

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
public class LeaveResponseDto {

    private Long id;
    private Long userId;
    private String employeeName; 
    private String employeeEmail;
    private String leaveType;
    private LocalDate fromDate;
    private LocalDate toDate;
    @Builder.Default
    private Double totalDays = 0.0;
    @Builder.Default
    private String reason = "No reason specified";
    @Builder.Default
    private String status = "SUBMITTED";
    @Builder.Default
    private String managerComment = "No review comment yet";
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}