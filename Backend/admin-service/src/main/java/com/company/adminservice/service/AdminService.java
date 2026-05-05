package com.company.adminservice.service;

import com.company.adminservice.client.*;
import com.company.adminservice.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AuthServiceClient authServiceClient;
    private final TimesheetServiceClient timesheetServiceClient;
    private final LeaveServiceClient leaveServiceClient;

    // ─── Dashboard ─────────────────────────────────────

    public DashboardResponse getDashboard(String role) {

        // Fetch pending items for the "Recent" section
        List<TimesheetResponse> pendingTimesheetList =
                timesheetServiceClient.getPendingTimesheets(role);

        List<LeaveResponseDto> pendingLeaveList =
                leaveServiceClient.getPendingLeaves(role);

        // Fetch all users for the "Employees" section
        List<UserResponse> allUsers = authServiceClient.getAllUsers();

        // Fetch real counts for the stats tiles
        long pendingTS = timesheetServiceClient.getCountByStatus("SUBMITTED");
        long approvedTS = timesheetServiceClient.getCountByStatus("APPROVED");
        long rejectedTS = timesheetServiceClient.getCountByStatus("REJECTED");

        long pendingLV = leaveServiceClient.getCountByStatus("SUBMITTED");
        long approvedLV = leaveServiceClient.getCountByStatus("APPROVED");
        long rejectedLV = leaveServiceClient.getCountByStatus("REJECTED");

        return DashboardResponse.builder()
                .pendingTimesheets(pendingTS)
                .approvedTimesheets(approvedTS)
                .rejectedTimesheets(rejectedTS)
                .pendingLeaves(pendingLV)
                .approvedLeaves(approvedLV)
                .rejectedLeaves(rejectedLV)
                .recentTimesheets(pendingTimesheetList)
                .recentLeaves(pendingLeaveList)
                .allEmployees(allUsers)
                .build();
    }

    public Map<String, Object> getEmployeeSummary(String email) {
        // In a real app, we'd fetch specific stats for this user
        String nextHoliday = leaveServiceClient.getNextHoliday();
        
        return Map.of(
            "pendingActions", 2, // Example: 1 missing timesheet, 1 pending review
            "nextHoliday", nextHoliday,
            "message", "Summary for " + email + " retrieved successfully"
        );
    }

    // ─── Config & Policies ─────────────────────────────

    public Map<String, Object> getPublicConfig() {
        return Map.of(
            "status", "System Online",
            "announcements", List.of(
                "System maintenance scheduled for Sunday at 2 AM.",
                "Fiscal year-end timesheets due by Friday."
            ),
            "supportContact", "support@company.com"
        );
    }

    public Map<String, Object> getPolicies() {
        return Map.of(
            "leavePolicy", "Employees accrue 1.5 days per month.",
            "timesheetPolicy", "Timesheets must be submitted by Monday 10 AM.",
            "lastUpdated", "2024-01-01"
        );
    }

    // ─── Reports ────────────────────────────────────────

    public Map<String, Object> getTimesheetCompliance() {
        LocalDate currentWeek = LocalDate.now().with(DayOfWeek.MONDAY);
        long activeEmployees = authServiceClient.getAllUsers().size();
        long submittedCount = timesheetServiceClient.getSubmittedCount(currentWeek);

        double complianceRate = activeEmployees > 0 
            ? (double) submittedCount / activeEmployees * 100 
            : 0;

        return Map.of(
            "reportName", "Timesheet Compliance",
            "weekStart", currentWeek.toString(),
            "totalEmployees", activeEmployees,
            "submittedTimesheets", submittedCount,
            "compliancePercentage", String.format("%.2f%%", complianceRate)
        );
    }

    public Map<String, Object> getLeaveConsumption() {
        Map<String, Long> consumption = leaveServiceClient.getConsumptionStats();
        
        return Map.of(
            "reportName", "Leave Consumption",
            "consumptionByType", consumption,
            "totalApprovedLeaves", consumption.values().stream().mapToLong(Long::longValue).sum()
        );
    }

    public Object addHoliday(String role, HolidayDto holiday) {
        return leaveServiceClient.addHoliday(role, holiday);
    }

    // ─── Users ─────────────────────────────────────────

    public List<UserResponse> getAllUsers() {
        return authServiceClient.getAllUsers();
    }

    public String deleteUserById(Long id) {
        return authServiceClient.deleteUserById(id);
    }

    public UserResponse getUserById(Long id) {
        return authServiceClient.getUserById(id);
    }
}