package com.company.adminservice.client;

import com.company.adminservice.dto.HolidayDto;
import com.company.adminservice.dto.LeaveResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.Map;

@FeignClient(
    name = "leave-service",
    fallback = LeaveServiceClientFallback.class
)
public interface LeaveServiceClient {

    @GetMapping("/leave/manager/pending")
    List<LeaveResponseDto> getPendingLeaves(
            @RequestHeader("X-User-Role") String role);

    @GetMapping("/leave/internal/consumption")
    Map<String, Long> getConsumptionStats();

    @GetMapping("/leave/internal/next-holiday")
    String getNextHoliday();

    @GetMapping("/leave/internal/count")
    long getCountByStatus(@RequestParam("status") String status);

    @PostMapping("/leave/holidays")
    Object addHoliday(
            @RequestHeader("X-User-Role") String role,
            @RequestBody HolidayDto holiday);
}