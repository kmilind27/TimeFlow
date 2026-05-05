package com.company.timesheetservice.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ProjectResponse {

    private Long id;
    private String projectCode;
    private String projectName;
    private Boolean isActive;
}