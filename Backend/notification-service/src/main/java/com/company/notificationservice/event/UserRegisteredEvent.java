package com.company.notificationservice.event;

import lombok.*;

@Getter 
@Setter 
@Builder
@NoArgsConstructor 
@AllArgsConstructor
public class UserRegisteredEvent {
    private Long userId;
    private String email;
    private String fullName;
    private String employeeCode;
    private String role;
}
