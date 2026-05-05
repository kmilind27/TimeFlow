package com.company.leaveservice.exception;

import com.company.leaveservice.controller.LeaveController;
import com.company.leaveservice.service.LeaveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests GlobalExceptionHandler by wiring it with a real MockMvc
 * and stubbing LeaveService to throw each exception type.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @Mock private LeaveService leaveService;
    @InjectMocks private LeaveController leaveController;

    @BeforeEach
    void setUp() {
        // Wire the exception handler into standalone MockMvc
        mockMvc = MockMvcBuilders
                .standaloneSetup(leaveController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // 400 — InvalidOperationException
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("InvalidOperationException → 400")
    class InvalidOperationTests {

        @Test
        @DisplayName("Returns 400 with exception message")
        void handleInvalidOperation_Returns400() throws Exception {
            when(leaveService.getMyLeaveHistory(anyLong()))
                    .thenThrow(new InvalidOperationException("Leave dates overlap"));

            mockMvc.perform(get("/leave/my-requests")
                            .header("X-User-Id", 1L))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Leave dates overlap"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 400 — RuntimeException (generic)
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("RuntimeException → 400")
    class RuntimeExceptionTests {

        @Test
        @DisplayName("Returns 400 with runtime error message")
        void handleRuntimeException_Returns400() throws Exception {
            when(leaveService.getMyLeaveHistory(anyLong()))
                    .thenThrow(new RuntimeException("Unexpected DB error"));

            mockMvc.perform(get("/leave/my-requests")
                            .header("X-User-Id", 1L))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Unexpected DB error"));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 401 — BadCredentialsException
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("BadCredentialsException → 401")
    class BadCredentialsTests {

        @Test
        @DisplayName("Returns 401 with fixed message")
        void handleBadCredentials_Returns401() throws Exception {
            when(leaveService.getMyLeaveHistory(anyLong()))
                    .thenThrow(new BadCredentialsException("bad creds"));

            mockMvc.perform(get("/leave/my-requests")
                            .header("X-User-Id", 1L))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.message").value("Invalid username or password"));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 404 — UsernameNotFoundException
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("UsernameNotFoundException → 404")
    class UserNotFoundTests {

        @Test
        @DisplayName("Returns 404 with exception message")
        void handleUsernameNotFound_Returns404() throws Exception {
            when(leaveService.getMyLeaveHistory(anyLong()))
                    .thenThrow(new UsernameNotFoundException("User not found"));

            mockMvc.perform(get("/leave/my-requests")
                            .header("X-User-Id", 1L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("User not found"));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 403 — AccessDeniedException
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("AccessDeniedException → 403")
    class AccessDeniedTests {

        @Test
        @DisplayName("Returns 403 with Access Denied message")
        void handleAccessDenied_Returns403() throws Exception {
            when(leaveService.getMyLeaveHistory(anyLong()))
                    .thenThrow(new AccessDeniedException("forbidden"));

            mockMvc.perform(get("/leave/my-requests")
                            .header("X-User-Id", 1L))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.message").value("Access Denied"));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 400 — MethodArgumentNotValidException (@Valid)
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("MethodArgumentNotValidException → 400 with field errors")
    class ValidationExceptionTests {

        @Test
        @DisplayName("Returns 400 with validationErrors map when body is invalid")
        void handleValidation_Returns400WithErrors() throws Exception {
            // POST with a body missing required fields triggers @Valid
            String invalidBody = """
                    {
                      "fromDate": "2026-04-01",
                      "toDate":   "2026-04-02"
                    }
                    """;
            // leaveType and reason are @NotBlank — will fail @Valid

            mockMvc.perform(post("/leave/apply")
                            .header("X-User-Id", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Validation Failed"))
                    .andExpect(jsonPath("$.message").value("Invalid input data"))
                    .andExpect(jsonPath("$.validationErrors").exists())
                    .andExpect(jsonPath("$.validationErrors.leaveType").exists())
                    .andExpect(jsonPath("$.validationErrors.reason").exists());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // ErrorResponse structure
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ErrorResponse structure")
    class ErrorResponseStructure {

        @Test
        @DisplayName("Error response always includes timestamp, status, error, path fields")
        void errorResponse_ContainsAllBaseFields() throws Exception {
            when(leaveService.getMyLeaveHistory(anyLong()))
                    .thenThrow(new InvalidOperationException("some error"));

            mockMvc.perform(get("/leave/my-requests")
                            .header("X-User-Id", 1L))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").exists())
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.path").value("/leave/my-requests"));
        }
    }
}