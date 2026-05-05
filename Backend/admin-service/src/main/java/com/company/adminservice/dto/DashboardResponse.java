package com.company.adminservice.dto;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

@Getter 
@Setter 
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardResponse {

    // Timesheet stats
    private long pendingTimesheets;
    private long approvedTimesheets;
    private long rejectedTimesheets;

    // Leave stats
    private long pendingLeaves;
    private long approvedLeaves;
    private long rejectedLeaves;

    // Lists
    @Builder.Default
    private List<TimesheetResponse> recentTimesheets = new ArrayList<>();
    @Builder.Default
    private List<LeaveResponseDto> recentLeaves = new ArrayList<>();
    @Builder.Default
    private List<UserResponse> allEmployees = new ArrayList<>();
}