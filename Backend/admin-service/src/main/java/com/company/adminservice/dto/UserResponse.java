package com.company.adminservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    private Long id;
    @Builder.Default
    private String employeeCode = "Code not assigned";
    @Builder.Default
    private String fullName = "Full name not provided";
    private String email;
    @Builder.Default
    private String role = "Role information unavailable";
    @Builder.Default
    private String status = "Status unconfirmed";
    @Builder.Default
    private Long managerId = 0L;
    private LocalDateTime createdAt;
}