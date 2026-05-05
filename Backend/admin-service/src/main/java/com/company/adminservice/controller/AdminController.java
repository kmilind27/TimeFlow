package com.company.adminservice.controller;

import com.company.adminservice.dto.*;
import com.company.adminservice.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin dashboard and management APIs")
public class AdminController {

    private final AdminService adminService;

    // ─── Dashboard ─────────────────────────────────────

    @Operation(summary = "Get dashboard", description = "[Manager/Admin] Fetch aggregated dashboard data")
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DashboardResponse> getDashboard(
            @Parameter(hidden = true) @RequestHeader("X-User-Role") String role) {

        return ResponseEntity.ok(
            adminService.getDashboard(role));
    }

    @Operation(summary = "Get employee dashboard summary", description = "[Employee] Fetch aggregate dashboard data for employee")
    @GetMapping("/dashboard/employee-summary")
    public ResponseEntity<Map<String, Object>> getEmployeeSummary(
            @Parameter(hidden = true) @RequestHeader("X-User-Role") String role,
            @Parameter(hidden = true) @RequestHeader("X-User-Email") String email) {
        
        return ResponseEntity.ok(adminService.getEmployeeSummary(email));
    }

    // ─── Public Config ─────────────────────────────────

    @Operation(summary = "Get public configs", description = "[Public] Fetch static announcements and config")
    @GetMapping("/config/public")
    public ResponseEntity<Map<String, Object>> getPublicConfig() {
        return ResponseEntity.ok(adminService.getPublicConfig());
    }

    // ─── Master Data & Policies ─────────────────────────

    @Operation(summary = "Get all policies", description = "[Admin/HR] Fetch organization master data and policies")
    @GetMapping("/master/policies")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getPolicies(
            @Parameter(hidden = true) @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok(adminService.getPolicies());
    }

    @Operation(summary = "Add holiday", description = "[Admin] Add a new holiday to the calendar")
    @PostMapping("/master/holidays")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> addHoliday(
            @Parameter(hidden = true) @RequestHeader("X-User-Role") String role,
            @RequestBody HolidayDto holiday) {
        return ResponseEntity.status(201).body(adminService.addHoliday(role, holiday));
    }

    // ─── Reports ────────────────────────────────────────

    @Operation(summary = "Get Timesheet Compliance Report", description = "[Admin] Generate compliance report")
    @GetMapping("/reports/timesheet-compliance")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getTimesheetCompliance(
            @Parameter(hidden = true) @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok(adminService.getTimesheetCompliance());
    }

    @Operation(summary = "Get Leave Consumption Report", description = "[Admin] Generate leave report")
    @GetMapping("/reports/leave-consumption")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getLeaveConsumption(
            @Parameter(hidden = true) @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok(adminService.getLeaveConsumption());
    }

    // ─── Users ─────────────────────────────────────────

    @Operation(summary = "Delete user by ID", description = "[Admin] Soft delete a user account")
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.deleteUserById(id));
    }

    @Operation(summary = "Get all users", description = "[Admin] Fetch all registered users")
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers(
            @Parameter(hidden = true) @RequestHeader("X-User-Role") String role) {

        return ResponseEntity.ok(
            adminService.getAllUsers());
    }

    @Operation(summary = "Get user by ID", description = "[Manager/Admin] Fetch a specific user's details")
    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(hidden = true) @RequestHeader("X-User-Role") String role,
            @PathVariable Long id) {

        return ResponseEntity.ok(
            adminService.getUserById(id));
    }
}