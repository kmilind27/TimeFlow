package com.company.leaveservice.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonInclude;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LeaveBalanceDto {

    private Long id;
    private String leaveType;
    private Double totalDays;
    private Double usedDays;
    private Double remainingDays;
    private Integer year;
}