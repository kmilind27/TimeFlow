package com.company.leaveservice.dto;

import lombok.*;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@ToString
public class UserRegisteredEvent {
    private Long userId;
    private String email;
    private String fullName;
    private String employeeCode;
    private String role;
}
