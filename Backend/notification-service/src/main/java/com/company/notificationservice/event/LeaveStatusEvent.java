package com.company.notificationservice.event;

import lombok.*;

@Getter 
@Setter 
@Builder
@NoArgsConstructor 
@AllArgsConstructor
public class LeaveStatusEvent {
    private Long leaveId;
    private Long userId;
    private String userEmail;
    private String leaveType;
    private String status;        // APPLIED, APPROVED, REJECTED, CANCELLED
    private String startDate;
    private String endDate;
    private String reviewComment;
}
