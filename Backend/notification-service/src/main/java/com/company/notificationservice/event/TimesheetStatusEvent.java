package com.company.notificationservice.event;

import lombok.*;

@Getter 
@Setter 
@Builder
@NoArgsConstructor 
@AllArgsConstructor
public class TimesheetStatusEvent {
    private Long timesheetId;
    private Long userId;
    private String userEmail;
    private String status;        // SUBMITTED, APPROVED, REJECTED
    private String weekStart;
    private String reviewComment;
}
