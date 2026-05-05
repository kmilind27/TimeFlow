package com.company.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeRoleRequest {

    @NotBlank(message = "Role is required")
    @Pattern(
        regexp = "EMPLOYEE|MANAGER|ADMIN",
        message = "Role must be EMPLOYEE, MANAGER or ADMIN"
    )
    private String role;
}