package com.company.authservice.controller;

import com.company.authservice.dto.*;
import com.company.authservice.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    // ── Test Data ─────────────────────────────────────────────

    private UserDetails mockUserDetails;
    private UserResponse mockUserResponse;
    private AuthResponse mockAuthResponse;
    private SignupRequest signupRequest;
    private LoginRequest loginRequest;
    private ForgotPasswordOtpRequest forgotPasswordOtpRequest;
    private VerifyOtpRequest verifyOtpRequest;
    private ForgotPasswordRequest forgotPasswordRequest;
    private ChangeRoleRequest changeRoleRequest;
    private ChangeStatusRequest changeStatusRequest;
    private UpdateManagerRequest updateManagerRequest;

    @BeforeEach
    void setUp() {

        // ✅ Spring Security UserDetails — username() returns email
        mockUserDetails = User.builder()
                .username("john@test.com")
                .password("encodedPassword")
                .authorities(Collections.emptyList())
                .build();

        // ✅ UserResponse — fields: id, employeeCode, fullName, email,
        //                          role, status, managerId, createdAt
        mockUserResponse = UserResponse.builder()
                .id(1L)
                .employeeCode("EMP001")
                .fullName("John Doe")
                .email("john@test.com")
                .role("EMPLOYEE")
                .status("ACTIVE")
                .managerId(0L)
                .build();

        // ✅ AuthResponse — fields: token, userId, email, fullName, role
        mockAuthResponse = AuthResponse.builder()
                .token("mock.jwt.token")
                .userId(1L)
                .email("john@test.com")
                .fullName("John Doe")
                .role("EMPLOYEE")
                .build();

        // ✅ SignupRequest — fields: employeeCode, fullName, email, password, managerId
        signupRequest = new SignupRequest();
        signupRequest.setEmployeeCode("EMP001");
        signupRequest.setFullName("John Doe");
        signupRequest.setEmail("john@test.com");
        signupRequest.setPassword("Test@1234");

        // ✅ LoginRequest — fields: email, password
        loginRequest = new LoginRequest();
        loginRequest.setEmail("john@test.com");
        loginRequest.setPassword("Test@1234");

        // ✅ ForgotPasswordOtpRequest — Step 1: email only
        forgotPasswordOtpRequest = new ForgotPasswordOtpRequest();
        forgotPasswordOtpRequest.setEmail("john@test.com");

        // ✅ VerifyOtpRequest — Step 2: email + otp
        verifyOtpRequest = new VerifyOtpRequest();
        verifyOtpRequest.setEmail("john@test.com");
        verifyOtpRequest.setOtp("123456");

        // ✅ ForgotPasswordRequest — Step 3: resetToken, newPassword, confirmPassword
        forgotPasswordRequest = new ForgotPasswordRequest();
        forgotPasswordRequest.setResetToken("mock-reset-token");
        forgotPasswordRequest.setNewPassword("NewPass@1234");
        forgotPasswordRequest.setConfirmPassword("NewPass@1234");

        // ✅ ChangeRoleRequest — field: role (EMPLOYEE|MANAGER|ADMIN)
        changeRoleRequest = new ChangeRoleRequest();
        changeRoleRequest.setRole("MANAGER");

        // ✅ ChangeStatusRequest — field: status (ACTIVE|INACTIVE)
        changeStatusRequest = new ChangeStatusRequest();
        changeStatusRequest.setStatus("INACTIVE");

        // ✅ UpdateManagerRequest — field: managerId
        updateManagerRequest = new UpdateManagerRequest();
        updateManagerRequest.setManagerId(2L);
    }

    // ════════════════════════════════════════════════════════
    // POST /auth/signup → 201 Created
    // ════════════════════════════════════════════════════════

    @Test
    void signup_shouldReturn201WithMessage() {
        when(authService.signup(signupRequest))
                .thenReturn("User registered successfully");

        ResponseEntity<String> response =
                authController.signup(signupRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo("User registered successfully");
        verify(authService).signup(signupRequest);
    }

    @Test
    void signup_shouldCallServiceWithCorrectRequest() {
        when(authService.signup(any(SignupRequest.class)))
                .thenReturn("User registered successfully");

        authController.signup(signupRequest);

        verify(authService, times(1)).signup(signupRequest);
    }

    @Test
    void signup_withManagerId_shouldReturn201() {
        signupRequest.setManagerId(2L);
        when(authService.signup(signupRequest))
                .thenReturn("User registered successfully");

        ResponseEntity<String> response =
                authController.signup(signupRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        verify(authService).signup(signupRequest);
    }

    // ════════════════════════════════════════════════════════
    // POST /auth/login → 200 OK
    // ════════════════════════════════════════════════════════

    @Test
    void login_shouldReturn200WithAuthResponse() {
        when(authService.login(loginRequest)).thenReturn(mockAuthResponse);

        ResponseEntity<AuthResponse> response =
                authController.login(loginRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        // ✅ Verify all AuthResponse fields
        assertThat(response.getBody().getToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getBody().getUserId()).isEqualTo(1L);
        assertThat(response.getBody().getEmail()).isEqualTo("john@test.com");
        assertThat(response.getBody().getFullName()).isEqualTo("John Doe");
        assertThat(response.getBody().getRole()).isEqualTo("EMPLOYEE");
        verify(authService).login(loginRequest);
    }

    @Test
    void login_shouldReturnTokenInResponse() {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(mockAuthResponse);

        ResponseEntity<AuthResponse> response =
                authController.login(loginRequest);

        assertThat(response.getBody().getToken()).isNotBlank();
    }

    // ════════════════════════════════════════════════════════
    // POST /auth/forgot-password → Step 1: Request OTP
    // ════════════════════════════════════════════════════════

    @Test
    void forgotPassword_shouldReturn200WithOtpSentMessage() {
        doNothing().when(authService).requestPasswordReset(forgotPasswordOtpRequest);

        ResponseEntity<String> response =
                authController.forgotPassword(forgotPasswordOtpRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("OTP sent to your registered email");
        verify(authService).requestPasswordReset(forgotPasswordOtpRequest);
    }

    @Test
    void forgotPassword_shouldCallServiceOnce() {
        doNothing().when(authService).requestPasswordReset(any(ForgotPasswordOtpRequest.class));

        authController.forgotPassword(forgotPasswordOtpRequest);

        verify(authService, times(1)).requestPasswordReset(forgotPasswordOtpRequest);
    }

    // ════════════════════════════════════════════════════════
    // POST /auth/verify-otp → Step 2: Verify OTP
    // ════════════════════════════════════════════════════════

    @Test
    void verifyOtp_shouldReturn200WithResetToken() {
        VerifyOtpResponse mockOtpResponse = VerifyOtpResponse.builder()
                .resetToken("mock-reset-token")
                .message("OTP verified successfully")
                .build();

        when(authService.verifyOtp(verifyOtpRequest)).thenReturn(mockOtpResponse);

        ResponseEntity<VerifyOtpResponse> response =
                authController.verifyOtp(verifyOtpRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getResetToken()).isEqualTo("mock-reset-token");
        assertThat(response.getBody().getMessage()).isEqualTo("OTP verified successfully");
        verify(authService).verifyOtp(verifyOtpRequest);
    }

    // ════════════════════════════════════════════════════════
    // POST /auth/reset-password → Step 3: Reset Password
    // ════════════════════════════════════════════════════════

    @Test
    void resetPassword_shouldReturn200WithSuccessMessage() {
        doNothing().when(authService).resetPassword(forgotPasswordRequest);

        ResponseEntity<String> response =
                authController.resetPassword(forgotPasswordRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("Password reset successfully");
        verify(authService).resetPassword(forgotPasswordRequest);
    }

    // ════════════════════════════════════════════════════════
    // GET /auth/profile → 200 OK
    // Uses @AuthenticationPrincipal UserDetails
    // ════════════════════════════════════════════════════════

    @Test
    void getProfile_shouldReturn200WithUserResponse() {
        // ✅ controller calls: authService.getProfile(userDetails.getUsername())
        // ✅ getUsername() returns email for our mockUserDetails
        when(authService.getProfile("john@test.com"))
                .thenReturn(mockUserResponse);

        ResponseEntity<UserResponse> response =
                authController.getProfile(mockUserDetails);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getEmail()).isEqualTo("john@test.com");
        assertThat(response.getBody().getFullName()).isEqualTo("John Doe");
        assertThat(response.getBody().getEmployeeCode()).isEqualTo("EMP001");
        assertThat(response.getBody().getRole()).isEqualTo("EMPLOYEE");
        assertThat(response.getBody().getStatus()).isEqualTo("ACTIVE");
        verify(authService).getProfile("john@test.com");
    }

    @Test
    void getProfile_shouldUseUsernameFromUserDetails() {
        when(authService.getProfile("john@test.com"))
                .thenReturn(mockUserResponse);

        authController.getProfile(mockUserDetails);

        // ✅ Must call with email extracted from UserDetails
        verify(authService).getProfile("john@test.com");
        verify(authService, never()).getProfile("wrongemail@test.com");
    }

    // ════════════════════════════════════════════════════════
    // PATCH /auth/users/{id}/manager → 200 OK
    // ════════════════════════════════════════════════════════

    @Test
    void updateManager_shouldReturn200WithUpdatedUser() {
        when(authService.updateManager(1L, "john@test.com", updateManagerRequest))
                .thenReturn(mockUserResponse);

        ResponseEntity<UserResponse> response =
                authController.updateManager(1L, mockUserDetails, updateManagerRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(1L);
        verify(authService).updateManager(1L, "john@test.com", updateManagerRequest);
    }

    @Test
    void updateManager_shouldPassCorrectManagerId() {
        updateManagerRequest.setManagerId(5L);
        when(authService.updateManager(2L, "john@test.com",
                updateManagerRequest)).thenReturn(mockUserResponse);

        authController.updateManager(2L, mockUserDetails, updateManagerRequest);

        verify(authService).updateManager(2L, "john@test.com", updateManagerRequest);
    }

    // ════════════════════════════════════════════════════════
    // PATCH /auth/users/{id}/role → 200 OK
    // ════════════════════════════════════════════════════════

    @Test
    void changeRole_shouldReturn200WithUpdatedUser() {
        UserResponse managerResponse = UserResponse.builder()
                .id(1L)
                .email("john@test.com")
                .role("MANAGER")
                .status("ACTIVE")
                .build();

        when(authService.changeRole(1L, changeRoleRequest, "john@test.com"))
                .thenReturn(managerResponse);

        ResponseEntity<UserResponse> response =
                authController.changeRole(1L, changeRoleRequest, mockUserDetails);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getRole()).isEqualTo("MANAGER");
        verify(authService).changeRole(1L, changeRoleRequest, "john@test.com");
    }

    @Test
    void changeRole_toAdmin_shouldReturn200() {
        changeRoleRequest.setRole("ADMIN");
        UserResponse adminResponse = UserResponse.builder()
                .id(2L).role("ADMIN").build();

        when(authService.changeRole(2L, changeRoleRequest, "john@test.com"))
                .thenReturn(adminResponse);

        ResponseEntity<UserResponse> response =
                authController.changeRole(2L, changeRoleRequest, mockUserDetails);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getRole()).isEqualTo("ADMIN");
    }

    @Test
    void changeRole_toEmployee_shouldReturn200() {
        changeRoleRequest.setRole("EMPLOYEE");
        when(authService.changeRole(1L, changeRoleRequest, "john@test.com"))
                .thenReturn(mockUserResponse);

        ResponseEntity<UserResponse> response =
                authController.changeRole(1L, changeRoleRequest, mockUserDetails);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(authService).changeRole(1L, changeRoleRequest, "john@test.com");
    }

    // ════════════════════════════════════════════════════════
    // PATCH /auth/users/{id}/status → 200 OK
    // ════════════════════════════════════════════════════════

    @Test
    void changeStatus_toInactive_shouldReturn200() {
        UserResponse inactiveUser = UserResponse.builder()
                .id(1L)
                .email("john@test.com")
                .status("INACTIVE")
                .build();

        when(authService.changeStatus(1L, changeStatusRequest, "john@test.com"))
                .thenReturn(inactiveUser);

        ResponseEntity<UserResponse> response =
                authController.changeStatus(1L, changeStatusRequest, mockUserDetails);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getStatus()).isEqualTo("INACTIVE");
        verify(authService).changeStatus(1L, changeStatusRequest, "john@test.com");
    }

    @Test
    void changeStatus_toActive_shouldReturn200() {
        changeStatusRequest.setStatus("ACTIVE");
        when(authService.changeStatus(1L, changeStatusRequest, "john@test.com"))
                .thenReturn(mockUserResponse);

        ResponseEntity<UserResponse> response =
                authController.changeStatus(1L, changeStatusRequest, mockUserDetails);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(authService).changeStatus(1L, changeStatusRequest, "john@test.com");
    }

    // ════════════════════════════════════════════════════════
    // GET /auth/users → 200 OK (internal endpoint)
    // ════════════════════════════════════════════════════════

    @Test
    void getAllUsers_shouldReturn200WithList() {
        UserResponse user2 = UserResponse.builder()
                .id(2L)
                .employeeCode("EMP002")
                .fullName("Jane Doe")
                .email("jane@test.com")
                .role("MANAGER")
                .status("ACTIVE")
                .build();
        when(authService.getAllUsers()).thenReturn(List.of(mockUserResponse, user2));

        ResponseEntity<List<UserResponse>> response =
                authController.getAllUsers();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).getEmployeeCode()).isEqualTo("EMP001");
        assertThat(response.getBody().get(1).getRole()).isEqualTo("MANAGER");
        verify(authService).getAllUsers();
    }

    @Test
    void getAllUsers_whenEmpty_shouldReturn200WithEmptyList() {
        when(authService.getAllUsers()).thenReturn(List.of());

        ResponseEntity<List<UserResponse>> response =
                authController.getAllUsers();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEmpty();
    }

    // ════════════════════════════════════════════════════════
    // GET /auth/users/{id} → 200 OK (internal endpoint)
    // ════════════════════════════════════════════════════════

    @Test
    void getUserById_shouldReturn200WithUser() {
        when(authService.getUserById(1L)).thenReturn(mockUserResponse);

        ResponseEntity<UserResponse> response =
                authController.getUserById(1L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(1L);
        assertThat(response.getBody().getFullName()).isEqualTo("John Doe");
        assertThat(response.getBody().getEmployeeCode()).isEqualTo("EMP001");
        verify(authService).getUserById(1L);
    }

    @Test
    void getUserById_shouldCallServiceWithCorrectId() {
        when(authService.getUserById(99L)).thenReturn(mockUserResponse);

        authController.getUserById(99L);

        verify(authService).getUserById(99L);
        verify(authService, never()).getUserById(1L);
    }

    // ════════════════════════════════════════════════════════
    // GET /auth/health-check → 200 OK
    // ════════════════════════════════════════════════════════

    @Test
    void healthCheck_shouldReturn200WithMessage() {
        ResponseEntity<String> response = authController.healthCheck();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("Auth Service is running!");
        // ✅ No service call — pure controller method
        verifyNoInteractions(authService);
    }

    // ════════════════════════════════════════════════════════
    // DELETE /auth/users/{id} → 200 OK (internal endpoint)
    // ════════════════════════════════════════════════════════

    @Test
    void deleteUserById_shouldReturn200WithMessage() {
        // ✅ authService.deleteUserById returns void
        doNothing().when(authService).deleteUserById(1L);

        ResponseEntity<String> response =
                authController.deleteUserById(1L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        // ✅ Message hardcoded: "User soft-deleted with id: " + id
        assertThat(response.getBody()).isEqualTo("User soft-deleted with id: 1");
        verify(authService).deleteUserById(1L);
    }

    @Test
    void deleteUserById_shouldIncludeIdInMessage() {
        doNothing().when(authService).deleteUserById(42L);

        ResponseEntity<String> response =
                authController.deleteUserById(42L);

        assertThat(response.getBody()).isEqualTo("User soft-deleted with id: 42");
        verify(authService).deleteUserById(42L);
    }

    @Test
    void deleteUserById_shouldCallServiceWithCorrectId() {
        doNothing().when(authService).deleteUserById(5L);

        authController.deleteUserById(5L);

        verify(authService).deleteUserById(5L);
        verify(authService, never()).deleteUserById(1L);
    }
}