package com.company.adminservice.client;

import com.company.adminservice.dto.TimesheetResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.List;

@FeignClient(
    name = "timesheet-service",
    fallback = TimesheetServiceClientFallback.class
)
public interface TimesheetServiceClient {

    // ✅ Pass X-User-Role header so Timesheet Service
    //    allows manager/admin access
    @GetMapping("/timesheet/manager/pending")
    List<TimesheetResponse> getPendingTimesheets(
            @RequestHeader("X-User-Role") String role);

    @GetMapping("/timesheet/internal/compliance")
    long getSubmittedCount(
            @RequestParam("weekStart")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            java.time.LocalDate weekStart);

    @GetMapping("/timesheet/internal/count")
    long getCountByStatus(
            @RequestParam("status") String status);
}