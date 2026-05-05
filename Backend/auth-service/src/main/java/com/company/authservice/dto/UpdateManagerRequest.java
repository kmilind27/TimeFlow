package com.company.authservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateManagerRequest {

    @NotNull(message = "Manager id is required")
    private Long managerId;
}