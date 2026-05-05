package com.company.authservice.exception;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SecurityException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        lenient().when(request.getRequestURI()).thenReturn("/auth/login");
    }

    // ════════════════════════════════════════════════════════
    // BadCredentialsException → 401
    // ════════════════════════════════════════════════════════

    @Test
    void handleBadCredentials_shouldReturn401() {
        BadCredentialsException ex =
                new BadCredentialsException("Invalid email or password");

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleBadCredentials(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody().getStatus()).isEqualTo(401);
        assertThat(response.getBody().getError()).isEqualTo("Unauthorized");
        // ✅ buildError(ex, request, status) — no override → uses ex.getMessage()
        assertThat(response.getBody().getMessage())
                .isEqualTo("Invalid email or password");
        assertThat(response.getBody().getPath()).isEqualTo("/auth/login");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    // ════════════════════════════════════════════════════════
    // UserNotFoundException → 404
    // ════════════════════════════════════════════════════════

    @Test
    void handleUserNotFound_shouldReturn404() {
        UserNotFoundException ex =
                new UserNotFoundException("User not found with id: 5");

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleUserNotFound(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
        assertThat(response.getBody().getMessage())
                .isEqualTo("User not found with id: 5");
    }

    @Test
    void handleUserNotFound_withEmailMessage_shouldPreserveIt() {
        UserNotFoundException ex =
                new UserNotFoundException("User not found with email: test@test.com");

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleUserNotFound(ex, request);

        assertThat(response.getBody().getMessage())
                .isEqualTo("User not found with email: test@test.com");
    }

    // ════════════════════════════════════════════════════════
    // InvalidRoleChangeException → 400
    // ════════════════════════════════════════════════════════

    @Test
    void handleInvalidRoleChange_shouldReturn400() {
        InvalidRoleChangeException ex =
                new InvalidRoleChangeException("Admin cannot change their own role");

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleUserNotFound(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage())
                .isEqualTo("Admin cannot change their own role");
    }

    @Test
    void handleInvalidRoleChange_differentMessage_shouldBePropagated() {
        InvalidRoleChangeException ex =
                new InvalidRoleChangeException("Cannot assign ADMIN role directly");

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleUserNotFound(ex, request);

        assertThat(response.getBody().getMessage())
                .isEqualTo("Cannot assign ADMIN role directly");
    }

    // ════════════════════════════════════════════════════════
    // UserAlreadyExistsException → 400
    // ════════════════════════════════════════════════════════

    @Test
    void handleUserAlreadyExists_shouldReturn400() {
        UserAlreadyExistsException ex =
                new UserAlreadyExistsException(
                        "Email already registered: john@test.com");

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleUserNotFound(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage())
                .isEqualTo("Email already registered: john@test.com");
    }

    // ════════════════════════════════════════════════════════
    // AccessDeniedException → 403
    // ════════════════════════════════════════════════════════

    @Test
    void handleAccessDenied_shouldReturn403() {
        when(request.getRequestURI()).thenReturn("/auth/users/1/role");
        AccessDeniedException ex = new AccessDeniedException("Access is denied");

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleAccessDenied(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody().getError()).isEqualTo("Forbidden");
        assertThat(response.getBody().getMessage()).isEqualTo("Access is denied");
        assertThat(response.getBody().getPath()).isEqualTo("/auth/users/1/role");
    }

    // ════════════════════════════════════════════════════════
    // ExpiredJwtException → 401
    // ✅ Handler wraps with hardcoded "JWT token has expired"
    // ════════════════════════════════════════════════════════

    @Test
    void handleExpiredJwt_shouldReturn401WithHardcodedMessage() {
        // ✅ Mock — ExpiredJwtException needs Claims + header to construct
        ExpiredJwtException ex = mock(ExpiredJwtException.class);

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleExpiredJwt(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody().getError()).isEqualTo("Unauthorized");
        // ✅ Your handler creates new Exception("JWT token has expired")
        assertThat(response.getBody().getMessage())
                .isEqualTo("JWT token has expired");
    }

    @Test
    void handleExpiredJwt_messageShouldBeHardcoded_notFromException() {
        ExpiredJwtException ex = mock(ExpiredJwtException.class);
        lenient().when(ex.getMessage()).thenReturn("JWT expired at 2026-01-01");

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleExpiredJwt(ex, request);

        // ✅ Hardcoded — original exception message not exposed
        assertThat(response.getBody().getMessage())
                .isEqualTo("JWT token has expired");
        assertThat(response.getBody().getMessage())
                .doesNotContain("2026-01-01");
    }

    // ════════════════════════════════════════════════════════
    // io.jsonwebtoken.security.SecurityException → 401
    // ✅ Handler wraps with hardcoded "Invalid JWT token"
    // ════════════════════════════════════════════════════════

    @Test
    void handleInvalidJwt_shouldReturn401WithHardcodedMessage() {
        SecurityException ex = mock(SecurityException.class);

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleInvalidJwt(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody().getError()).isEqualTo("Unauthorized");
        // ✅ Your handler creates new Exception("Invalid JWT token")
        assertThat(response.getBody().getMessage())
                .isEqualTo("Invalid JWT token");
    }

    @Test
    void handleInvalidJwt_messageShouldBeHardcoded_notFromException() {
        SecurityException ex = mock(SecurityException.class);
        lenient().when(ex.getMessage()).thenReturn("JWT signature does not match");

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleInvalidJwt(ex, request);

        assertThat(response.getBody().getMessage()).isEqualTo("Invalid JWT token");
        assertThat(response.getBody().getMessage())
                .doesNotContain("JWT signature does not match");
    }

    // ════════════════════════════════════════════════════════
    // MethodArgumentNotValidException → 400
    // Covers: SignupRequest, LoginRequest, ForgotPasswordRequest,
    //         ChangeRoleRequest, ChangeStatusRequest, UpdateManagerRequest
    // ════════════════════════════════════════════════════════

    @Test
    void handleValidation_shouldReturn400WithValidationErrors() {
        MethodArgumentNotValidException ex =
                mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        // ✅ LoginRequest: email @Email
        FieldError fieldError = new FieldError(
                "loginRequest", "email", "Invalid email format");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleValidation(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Validation Failed");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid input data");
        assertThat(response.getBody().getValidationErrors())
                .containsKey("email");
        assertThat(response.getBody().getValidationErrors())
        .containsEntry("email", "Invalid email format");
    }

    @Test
    void handleValidation_signupRequest_allFieldsFailing_shouldReturnAll() {
        MethodArgumentNotValidException ex =
                mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        // ✅ SignupRequest: employeeCode, fullName, email, password
        List<FieldError> errors = List.of(
                new FieldError("signupRequest", "employeeCode",
                        "Employee code is required"),
                new FieldError("signupRequest", "fullName",
                        "Full name is required"),
                new FieldError("signupRequest", "email",
                        "Email is required"),
                new FieldError("signupRequest", "password",
                        "Password is required")
        );

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(errors);

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleValidation(ex, request);

        assertThat(response.getBody().getValidationErrors()).hasSize(4);
        assertThat(response.getBody().getValidationErrors())
                .containsKeys("employeeCode", "fullName", "email", "password");
    }

    @Test
    void handleValidation_changeRoleRequest_invalidRole_shouldWork() {
        MethodArgumentNotValidException ex =
                mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        // ✅ ChangeRoleRequest: role @Pattern(EMPLOYEE|MANAGER|ADMIN)
        FieldError fieldError = new FieldError("changeRoleRequest", "role",
                "Role must be EMPLOYEE, MANAGER or ADMIN");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleValidation(ex, request);

        assertThat(response.getBody().getValidationErrors())
        .containsEntry("role", "Role must be EMPLOYEE, MANAGER or ADMIN");
    }

    @Test
    void handleValidation_changeStatusRequest_invalidStatus_shouldWork() {
        MethodArgumentNotValidException ex =
                mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        // ✅ ChangeStatusRequest: status @Pattern(ACTIVE|INACTIVE)
        FieldError fieldError = new FieldError("changeStatusRequest", "status",
                "Status can be ACTIVE or INACTIVE only");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleValidation(ex, request);

        assertThat(response.getBody().getValidationErrors())
        .containsEntry("status", "Status can be ACTIVE or INACTIVE only");
    }

    @Test
    void handleValidation_forgotPassword_shouldWork() {
        MethodArgumentNotValidException ex =
                mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        // ✅ ForgotPasswordRequest: email, newPassword, confirmPassword
        List<FieldError> errors = List.of(
                new FieldError("forgotPasswordRequest", "newPassword",
                        "Password must be at least 8 characters"),
                new FieldError("forgotPasswordRequest", "confirmPassword",
                        "Confirm password is required")
        );

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(errors);

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleValidation(ex, request);

        assertThat(response.getBody().getValidationErrors())
                .containsKeys("newPassword", "confirmPassword");
    }

    @Test
    void handleValidation_updateManagerRequest_nullManagerId_shouldWork() {
        MethodArgumentNotValidException ex =
                mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        // ✅ UpdateManagerRequest: managerId @NotNull
        FieldError fieldError = new FieldError(
                "updateManagerRequest", "managerId", "Manager id is required");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleValidation(ex, request);

        assertThat(response.getBody().getValidationErrors())
        .containsEntry("managerId", "Manager id is required");
    }

    // ════════════════════════════════════════════════════════
    // RuntimeException → 400
    // ════════════════════════════════════════════════════════

    @Test
    void handleRuntime_shouldReturn400() {
        RuntimeException ex = new RuntimeException("Unexpected runtime error");

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleRuntime(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage())
                .isEqualTo("Unexpected runtime error");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    // ════════════════════════════════════════════════════════
    // Generic Exception → 500
    // ════════════════════════════════════════════════════════

    @Test
    void handleGeneric_shouldReturn500(){
        Exception ex = new Exception("Database connection failed");

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleGeneric(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        // ✅ Hardcoded in your handler
        assertThat(response.getBody().getMessage())
                .isEqualTo("An unexpected error occurred");
    }

    @Test
    void handleGeneric_shouldNotExposeInternalDetails() {
        Exception ex = new Exception("Sensitive DB creds exposed");

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleGeneric(ex, request);

        assertThat(response.getBody().getMessage())
                .isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().getMessage())
                .doesNotContain("Sensitive DB creds exposed");
    }

    // ════════════════════════════════════════════════════════
    // Custom Exception class tests — covers model package too
    // ════════════════════════════════════════════════════════

    @Test
    void userNotFoundException_shouldExtendRuntimeException() {
        UserNotFoundException ex =
                new UserNotFoundException("User not found");

        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("User not found");
    }

    @Test
    void userAlreadyExistsException_shouldExtendRuntimeException() {
        UserAlreadyExistsException ex =
                new UserAlreadyExistsException("Email already in use");

        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("Email already in use");
    }

    @Test
    void invalidRoleChangeException_shouldExtendRuntimeException() {
        InvalidRoleChangeException ex =
                new InvalidRoleChangeException("Cannot change own role");

        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("Cannot change own role");
    }
}