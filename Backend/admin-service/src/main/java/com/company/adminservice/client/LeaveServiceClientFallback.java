package com.company.adminservice.client;

import com.company.adminservice.dto.HolidayDto;
import com.company.adminservice.dto.LeaveResponseDto;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class LeaveServiceClientFallback
        implements LeaveServiceClient {

    @Override
    public List<LeaveResponseDto> getPendingLeaves(
            String role) {
        return List.of();
    }

    @Override
    public Map<String, Long> getConsumptionStats() {
        return Map.of();
    }

    @Override
    public String getNextHoliday() {
        return "N/A";
    }

    @Override
    public long getCountByStatus(String status) {
        return 0;
    }

    @Override
    public Object addHoliday(String role, HolidayDto holiday) {
        return null;
    }
}