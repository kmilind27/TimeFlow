package com.company.adminservice.service;

import com.company.adminservice.client.*;
import com.company.adminservice.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private TimesheetServiceClient timesheetServiceClient;

    @Mock
    private LeaveServiceClient leaveServiceClient;

    @InjectMocks
    private AdminService adminService;

    private UserResponse mockUser;

    @BeforeEach
    void setUp() {
        mockUser = UserResponse.builder()
                .id(1L)
                .fullName("Test User")
                .email("test@example.com")
                .build();
    }

    @Test
    void testGetDashboard_Success() {
        // Arrange
        when(authServiceClient.getAllUsers()).thenReturn(List.of(mockUser));
        when(timesheetServiceClient.getPendingTimesheets(anyString())).thenReturn(List.of());
        when(leaveServiceClient.getPendingLeaves(anyString())).thenReturn(List.of());
        
        when(timesheetServiceClient.getCountByStatus("SUBMITTED")).thenReturn(5L);
        when(timesheetServiceClient.getCountByStatus("APPROVED")).thenReturn(10L);
        when(timesheetServiceClient.getCountByStatus("REJECTED")).thenReturn(2L);
        
        when(leaveServiceClient.getCountByStatus("SUBMITTED")).thenReturn(3L);
        when(leaveServiceClient.getCountByStatus("APPROVED")).thenReturn(8L);
        when(leaveServiceClient.getCountByStatus("REJECTED")).thenReturn(1L);

        // Act
        DashboardResponse response = adminService.getDashboard("ADMIN");

        // Assert
        assertNotNull(response);
        assertEquals(5L, response.getPendingTimesheets());
        assertEquals(10L, response.getApprovedTimesheets());
        assertEquals(3L, response.getPendingLeaves());
        assertEquals(1, response.getAllEmployees().size());
        verify(authServiceClient).getAllUsers();
    }

    @Test
    void testGetEmployeeSummary_Success() {
        // Arrange
        when(leaveServiceClient.getNextHoliday()).thenReturn("Holi (2024-03-25)");

        // Act
        Map<String, Object> summary = adminService.getEmployeeSummary("test@example.com");

        // Assert
        assertEquals("Holi (2024-03-25)", summary.get("nextHoliday"));
        assertTrue(summary.get("message").toString().contains("test@example.com"));
    }

    @Test
    void testGetTimesheetCompliance_Success() {
        // Arrange
        when(authServiceClient.getAllUsers()).thenReturn(List.of(mockUser, mockUser)); // 2 employees
        when(timesheetServiceClient.getSubmittedCount(any())).thenReturn(1L); // 1 submitted

        // Act
        Map<String, Object> compliance = adminService.getTimesheetCompliance();

        // Assert
        assertEquals(2L, compliance.get("totalEmployees"));
        assertEquals(1L, compliance.get("submittedTimesheets"));
        assertEquals("50.00%", compliance.get("compliancePercentage"));
    }

    @Test
    void testGetTimesheetCompliance_ZeroEmployees() {
        // Arrange
        when(authServiceClient.getAllUsers()).thenReturn(List.of()); // 0 employees

        // Act
        Map<String, Object> compliance = adminService.getTimesheetCompliance();

        // Assert
        assertEquals(0L, compliance.get("totalEmployees"));
        assertEquals("0.00%", compliance.get("compliancePercentage"));
    }

    @Test
    void testGetLeaveConsumption_Success() {
        // Arrange
        Map<String, Long> mockConsumption = Map.of("SICK", 5L, "ANNUAL", 10L);
        when(leaveServiceClient.getConsumptionStats()).thenReturn(mockConsumption);

        // Act
        Map<String, Object> result = adminService.getLeaveConsumption();

        // Assert
        assertEquals(15L, result.get("totalApprovedLeaves"));
        Map<String, Long> consumption = (Map<String, Long>) result.get("consumptionByType");
        assertEquals(5L, consumption.get("SICK"));
    }

    @Test
    void testGetLeaveConsumption_NoData() {
        // Arrange
        when(leaveServiceClient.getConsumptionStats()).thenReturn(Map.of());

        // Act
        Map<String, Object> result = adminService.getLeaveConsumption();

        // Assert
        assertEquals(0L, result.get("totalApprovedLeaves"));
        assertTrue(((Map<?, ?>) result.get("consumptionByType")).isEmpty());
    }

    @Test
    void testGetDashboard_EmptyData() {
        // Arrange
        when(authServiceClient.getAllUsers()).thenReturn(List.of());
        when(timesheetServiceClient.getPendingTimesheets(anyString())).thenReturn(List.of());
        when(leaveServiceClient.getPendingLeaves(anyString())).thenReturn(List.of());

        // Act
        DashboardResponse response = adminService.getDashboard("ADMIN");

        // Assert
        assertNotNull(response);
        assertEquals(0L, response.getPendingTimesheets());
        assertEquals(0, response.getAllEmployees().size());
    }

    @Test
    void testGetPublicConfig_Success() {
        // Act
        Map<String, Object> config = adminService.getPublicConfig();

        // Assert
        assertEquals("System Online", config.get("status"));
        assertNotNull(config.get("announcements"));
    }

    @Test
    void testGetPolicies_Success() {
        // Act
        Map<String, Object> policies = adminService.getPolicies();

        // Assert
        assertTrue(policies.containsKey("leavePolicy"));
        assertTrue(policies.containsKey("timesheetPolicy"));
    }

    @Test
    void testGetAllUsers_Proxy() {
        // Arrange
        when(authServiceClient.getAllUsers()).thenReturn(List.of(mockUser));

        // Act
        List<UserResponse> result = adminService.getAllUsers();

        // Assert
        assertEquals(1, result.size());
        verify(authServiceClient).getAllUsers();
    }

    @Test
    void testGetUserById_Proxy() {
        // Arrange
        when(authServiceClient.getUserById(1L)).thenReturn(mockUser);

        // Act
        UserResponse result = adminService.getUserById(1L);

        // Assert
        assertEquals("Test User", result.getFullName());
        verify(authServiceClient).getUserById(1L);
    }

    @Test
    void testDeleteUserById_Proxy() {
        // Arrange
        when(authServiceClient.deleteUserById(1L)).thenReturn("User deleted");

        // Act
        String result = adminService.deleteUserById(1L);

        // Assert
        assertEquals("User deleted", result);
        verify(authServiceClient).deleteUserById(1L);
    }

    @Test
    void testAddHoliday_Proxy() {
        // Arrange
        HolidayDto holiday = HolidayDto.builder()
                .holidayName("New Year")
                .holidayDate(LocalDate.of(2025, 1, 1))
                .build();
        when(leaveServiceClient.addHoliday(anyString(), any(HolidayDto.class))).thenReturn("Holiday Added");

        // Act
        Object result = adminService.addHoliday("ADMIN", holiday);

        // Assert
        assertEquals("Holiday Added", result);
        verify(leaveServiceClient).addHoliday("ADMIN", holiday);
    }
}
