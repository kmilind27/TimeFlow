package com.company.adminservice.controller;

import com.company.adminservice.dto.*;
import com.company.adminservice.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminController adminController;

    private UserResponse mockUser;
    private DashboardResponse mockDashboard;
    private HolidayDto mockHoliday;

    @BeforeEach
    void setUp() {

        // ✅ UserResponse fields: id, employeeCode, fullName, email,
        //                         role, status, managerId, createdAt
        // ❌ NO totalUsers field in UserResponse
        mockUser = UserResponse.builder()
                .id(1L)
                .employeeCode("EMP001")
                .fullName("John Doe")
                .email("john@test.com")
                .role("EMPLOYEE")
                .status("ACTIVE")
                .managerId(0L)
                .build();

        // ✅ DashboardResponse fields: pendingTimesheets, approvedTimesheets,
        //    rejectedTimesheets, pendingLeaves, approvedLeaves, rejectedLeaves,
        //    recentTimesheets, recentLeaves, allEmployees
        // ❌ NO totalUsers field in DashboardResponse
        mockDashboard = DashboardResponse.builder()
                .pendingTimesheets(3)
                .approvedTimesheets(5)
                .rejectedTimesheets(1)
                .pendingLeaves(2)
                .approvedLeaves(4)
                .rejectedLeaves(0)
                .build();

        TimesheetResponse.builder()
                .id(1L)
                .userId(1L)
                .employeeName("John Doe")
                .employeeEmail("john@test.com")
                .weekStart(LocalDate.of(2026, 3, 23))
                .weekEnd(LocalDate.of(2026, 3, 29))
                .status("SUBMITTED")
                .totalHours(40.0)
                .build();

        LeaveResponseDto.builder()
                .id(1L)
                .userId(1L)
                .employeeName("John Doe")
                .employeeEmail("john@test.com")
                .leaveType("CASUAL")
                .fromDate(LocalDate.of(2026, 4, 1))
                .toDate(LocalDate.of(2026, 4, 3))
                .totalDays(3.0)
                .reason("Personal work")
                .status("SUBMITTED")
                .build();

        // ✅ HolidayDto: holidayDate is LocalDate NOT String
        // ✅ Field is holidayName NOT description
        // ✅ Has holidayType field (default = "OPTIONAL")
        mockHoliday = HolidayDto.builder()
                .holidayDate(LocalDate.of(2026, 8, 15))
                .holidayName("Independence Day")
                .holidayType("NATIONAL")
                .build();
    }

    // ════════════════════════════════════════════════════════
    // GET /admin/dashboard
    // ════════════════════════════════════════════════════════

    @Test
    void getDashboard_whenAdmin_shouldReturn200() {
        when(adminService.getDashboard("ADMIN")).thenReturn(mockDashboard);

        ResponseEntity<DashboardResponse> response =
                adminController.getDashboard("ADMIN");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        // ✅ Correct fields — no totalUsers
        assertThat(response.getBody().getPendingTimesheets()).isEqualTo(3);
        assertThat(response.getBody().getApprovedTimesheets()).isEqualTo(5);
        assertThat(response.getBody().getPendingLeaves()).isEqualTo(2);
        assertThat(response.getBody().getApprovedLeaves()).isEqualTo(4);
        assertThat(response.getBody().getRejectedTimesheets()).isEqualTo(1);
        assertThat(response.getBody().getRejectedLeaves()).isZero();
        verify(adminService).getDashboard("ADMIN");
    }

    @Test
    void getDashboard_whenManager_shouldReturn200() {
        when(adminService.getDashboard("MANAGER")).thenReturn(mockDashboard);

        ResponseEntity<DashboardResponse> response =
                adminController.getDashboard("MANAGER");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(adminService).getDashboard("MANAGER");
    }

    @Test
    void getDashboard_defaultLists_shouldBeEmptyNotNull() {
        // ✅ @Builder.Default — lists initialize to empty ArrayList
        DashboardResponse emptyDashboard = DashboardResponse.builder().build();
        when(adminService.getDashboard("ADMIN")).thenReturn(emptyDashboard);

        ResponseEntity<DashboardResponse> response =
                adminController.getDashboard("ADMIN");

        assertThat(response.getBody().getRecentTimesheets()).isNotNull().isEmpty();
        assertThat(response.getBody().getRecentLeaves()).isNotNull().isEmpty();
        assertThat(response.getBody().getAllEmployees()).isNotNull().isEmpty();
    }

    // ════════════════════════════════════════════════════════
    // GET /admin/dashboard/employee-summary
    // ════════════════════════════════════════════════════════

    @Test
    void getEmployeeSummary_shouldReturn200() {
        Map<String, Object> summary = Map.of(
                "pendingLeaves", 2,
                "submittedTimesheets", 1
        );
        when(adminService.getEmployeeSummary("john@test.com")).thenReturn(summary);

        ResponseEntity<Map<String, Object>> response =
                adminController.getEmployeeSummary("EMPLOYEE", "john@test.com");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsKey("pendingLeaves");
        verify(adminService).getEmployeeSummary("john@test.com");
    }

    @Test
    void getEmployeeSummary_whenEmptySummary_shouldReturn200WithEmptyMap() {
        when(adminService.getEmployeeSummary("john@test.com")).thenReturn(Map.of());

        ResponseEntity<Map<String, Object>> response =
                adminController.getEmployeeSummary("EMPLOYEE", "john@test.com");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEmpty();
    }

    // ════════════════════════════════════════════════════════
    // GET /admin/config/public
    // ════════════════════════════════════════════════════════

    @Test
    void getPublicConfig_shouldReturn200() {
        Map<String, Object> config = Map.of(
                "announcement", "System maintenance on Sunday",
                "version", "1.0"
        );
        when(adminService.getPublicConfig()).thenReturn(config);

        ResponseEntity<Map<String, Object>> response =
                adminController.getPublicConfig();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsKey("announcement");
        verify(adminService).getPublicConfig();
    }

    @Test
    void getPublicConfig_whenEmpty_shouldReturn200() {
        when(adminService.getPublicConfig()).thenReturn(Map.of());

        ResponseEntity<Map<String, Object>> response =
                adminController.getPublicConfig();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEmpty();
    }

    // ════════════════════════════════════════════════════════
    // GET /admin/master/policies
    // ════════════════════════════════════════════════════════

    @Test
    void getPolicies_whenAdmin_shouldReturn200() {
        Map<String, Object> policies = Map.of(
                "leavePolicy", "12 casual days per year",
                "timesheetPolicy", "Submit by Friday"
        );
        when(adminService.getPolicies()).thenReturn(policies);

        ResponseEntity<Map<String, Object>> response =
                adminController.getPolicies("ADMIN");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsKey("leavePolicy");
        verify(adminService).getPolicies();
    }

    // ════════════════════════════════════════════════════════
    // POST /admin/master/holidays
    // ════════════════════════════════════════════════════════

    @Test
    void addHoliday_whenAdmin_shouldReturn201() {
        when(adminService.addHoliday("ADMIN", mockHoliday))
                .thenReturn(mockHoliday);

        ResponseEntity<Object> response =
                adminController.addHoliday("ADMIN", mockHoliday);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        verify(adminService).addHoliday("ADMIN", mockHoliday);
    }

    @Test
    void addHoliday_shouldPassCorrectHolidayToService() {
        // ✅ holidayDate = LocalDate, holidayName NOT description
        HolidayDto newHoliday = HolidayDto.builder()
                .holidayDate(LocalDate.of(2026, 10, 2))
                .holidayName("Gandhi Jayanti")
                .holidayType("NATIONAL")
                .build();
        when(adminService.addHoliday("ADMIN", newHoliday)).thenReturn(newHoliday);

        ResponseEntity<Object> response =
                adminController.addHoliday("ADMIN", newHoliday);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        verify(adminService).addHoliday("ADMIN", newHoliday);
    }

    @Test
    void holidayDto_defaultHolidayType_shouldBeOptional() {
        // ✅ @Builder.Default holidayType = "OPTIONAL"
        HolidayDto holiday = HolidayDto.builder()
                .holidayDate(LocalDate.of(2026, 12, 25))
                .holidayName("Christmas")
                .build();

        assertThat(holiday.getHolidayType()).isEqualTo("OPTIONAL");
    }

    // ════════════════════════════════════════════════════════
    // GET /admin/reports/timesheet-compliance
    // ════════════════════════════════════════════════════════

    @Test
    void getTimesheetCompliance_whenAdmin_shouldReturn200() {
        Map<String, Object> report = Map.of(
                "totalEmployees", 20,
                "submittedCount", 15,
                "complianceRate", "75%"
        );
        when(adminService.getTimesheetCompliance()).thenReturn(report);

        ResponseEntity<Map<String, Object>> response =
                adminController.getTimesheetCompliance("ADMIN");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsKey("complianceRate");
        verify(adminService).getTimesheetCompliance();
    }

    @Test
    void getTimesheetCompliance_whenEmptyReport_shouldReturn200() {
        when(adminService.getTimesheetCompliance()).thenReturn(Map.of());

        ResponseEntity<Map<String, Object>> response =
                adminController.getTimesheetCompliance("ADMIN");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEmpty();
    }

    // ════════════════════════════════════════════════════════
    // GET /admin/reports/leave-consumption
    // ════════════════════════════════════════════════════════

    @Test
    void getLeaveConsumption_whenAdmin_shouldReturn200() {
        Map<String, Object> report = Map.of(
                "totalLeavesTaken", 45,
                "casualLeaves", 20,
                "sickLeaves", 15
        );
        when(adminService.getLeaveConsumption()).thenReturn(report);

        ResponseEntity<Map<String, Object>> response =
                adminController.getLeaveConsumption("ADMIN");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsKey("totalLeavesTaken");
        verify(adminService).getLeaveConsumption();
    }

    @Test
    void getLeaveConsumption_whenEmptyReport_shouldReturn200() {
        when(adminService.getLeaveConsumption()).thenReturn(Map.of());

        ResponseEntity<Map<String, Object>> response =
                adminController.getLeaveConsumption("ADMIN");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    
    @Test
    void deleteUserById_shouldReturn200() {
        when(adminService.deleteUserById(1L))
                .thenReturn("User deleted successfully");

        ResponseEntity<String> response =
                adminController.deleteUserById(1L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("User deleted successfully");
        verify(adminService).deleteUserById(1L);
    }

    @Test
    void deleteUserById_shouldCallServiceWithCorrectId() {
        when(adminService.deleteUserById(99L)).thenReturn("User deleted successfully");

        adminController.deleteUserById(99L);

        verify(adminService).deleteUserById(99L);
        verify(adminService, never()).deleteUserById(1L);
    }

    // ════════════════════════════════════════════════════════
    // GET /admin/users
    // ════════════════════════════════════════════════════════

    @Test
    void getAllUsers_shouldReturn200WithList() {
        when(adminService.getAllUsers()).thenReturn(List.of(mockUser));

        ResponseEntity<List<UserResponse>> response =
                adminController.getAllUsers("ADMIN");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(1);
        // ✅ Verify actual UserResponse fields
        assertThat(response.getBody().get(0).getEmail()).isEqualTo("john@test.com");
        assertThat(response.getBody().get(0).getEmployeeCode()).isEqualTo("EMP001");
        assertThat(response.getBody().get(0).getFullName()).isEqualTo("John Doe");
        assertThat(response.getBody().get(0).getRole()).isEqualTo("EMPLOYEE");
        verify(adminService).getAllUsers();
    }

    @Test
    void getAllUsers_whenNoUsers_shouldReturn200WithEmptyList() {
        when(adminService.getAllUsers()).thenReturn(List.of());

        ResponseEntity<List<UserResponse>> response =
                adminController.getAllUsers("ADMIN");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getAllUsers_whenMultipleUsers_shouldReturnAll() {
        // ✅ Include employeeCode field
        UserResponse user2 = UserResponse.builder()
                .id(2L)
                .employeeCode("EMP002")
                .fullName("Jane Doe")
                .email("jane@test.com")
                .role("MANAGER")
                .status("ACTIVE")
                .managerId(0L)
                .build();
        when(adminService.getAllUsers()).thenReturn(List.of(mockUser, user2));

        ResponseEntity<List<UserResponse>> response =
                adminController.getAllUsers("ADMIN");

        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(1).getEmployeeCode()).isEqualTo("EMP002");
        assertThat(response.getBody().get(1).getRole()).isEqualTo("MANAGER");
    }

    @Test
    void userResponse_defaultValues_shouldBeSet() {
        // ✅ @Builder.Default values from UserResponse
        UserResponse defaultUser = UserResponse.builder().build();

        assertThat(defaultUser.getEmployeeCode()).isEqualTo("Code not assigned");
        assertThat(defaultUser.getFullName()).isEqualTo("Full name not provided");
        assertThat(defaultUser.getRole()).isEqualTo("Role information unavailable");
        assertThat(defaultUser.getStatus()).isEqualTo("Status unconfirmed");
        assertThat(defaultUser.getManagerId()).isZero();
    }

   

    @Test
    void getUserById_shouldReturn200() {
        when(adminService.getUserById(1L)).thenReturn(mockUser);

        ResponseEntity<UserResponse> response =
                adminController.getUserById("ADMIN", 1L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(1L);
        assertThat(response.getBody().getFullName()).isEqualTo("John Doe");
        assertThat(response.getBody().getEmployeeCode()).isEqualTo("EMP001");
        assertThat(response.getBody().getStatus()).isEqualTo("ACTIVE");
        verify(adminService).getUserById(1L);
    }

    @Test
    void getUserById_shouldCallServiceWithCorrectId() {
        when(adminService.getUserById(5L)).thenReturn(mockUser);

        adminController.getUserById("ADMIN", 5L);

        verify(adminService).getUserById(5L);
        verify(adminService, never()).getUserById(1L);
    }

    // ════════════════════════════════════════════════════════
    // DTO Default Value Tests — covers @Builder.Default branches
    // ════════════════════════════════════════════════════════

    @Test
    void timesheetResponse_defaultValues_shouldBeSet() {
        // ✅ @Builder.Default values from TimesheetResponse
        TimesheetResponse ts = TimesheetResponse.builder().build();

        assertThat(ts.getEmployeeName()).isEqualTo("Name information not found");
        assertThat(ts.getEmployeeEmail()).isEqualTo("Email information not found");
        assertThat(ts.getStatus()).isEqualTo("DRAFT");
        assertThat(ts.getTotalHours()).isEqualTo(0.0);
        assertThat(ts.getEntries()).isNotNull().isEmpty();
    }

    @Test
    void leaveResponseDto_defaultValues_shouldBeSet() {
        // ✅ @Builder.Default values from LeaveResponseDto
        LeaveResponseDto leave = LeaveResponseDto.builder().build();

        assertThat(leave.getEmployeeName()).isEqualTo("Name information not found");
        assertThat(leave.getEmployeeEmail()).isEqualTo("Email information not found");
        assertThat(leave.getTotalDays()).isEqualTo(0.0);
        assertThat(leave.getReason()).isEqualTo("No reason specified");
        assertThat(leave.getStatus()).isEqualTo("SUBMITTED");
        assertThat(leave.getManagerComment()).isEqualTo("No review comment yet");
    }

    @Test
    void timesheetEntryResponse_defaultValues_shouldBeSet() {
        // ✅ @Builder.Default values from TimesheetEntryResponse
        TimesheetEntryResponse entry = TimesheetEntryResponse.builder().build();

        assertThat(entry.getHoursLogged()).isEqualTo(0.0);
        assertThat(entry.getTaskSummary()).isEqualTo("No detailed summary provided");
    }
}