package com.company.timesheetservice.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String employeeCode;
    private String fullName;
    private String email;
    private String role;
    private String status;
    private Long managerId;
}