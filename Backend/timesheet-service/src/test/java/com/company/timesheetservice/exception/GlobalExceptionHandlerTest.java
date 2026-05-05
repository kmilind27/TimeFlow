package com.company.timesheetservice.exception;

import com.company.timesheetservice.controller.TimesheetController;
import com.company.timesheetservice.service.TimesheetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock private TimesheetService timesheetService;
    @InjectMocks private TimesheetController timesheetController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders
                .standaloneSetup(timesheetController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("InvalidOperationException → 400")
    class InvalidOperationTests {

        @Test
        @DisplayName("Returns 400 with exception message")
        void handleInvalidOperation_Returns400() throws Exception {
            when(timesheetService.getAllTimesheet(anyLong()))
                    .thenThrow(new InvalidOperationException("Cannot modify a SUBMITTED timesheet"));

            mockMvc.perform(get("/timesheet/my-timesheets").header("X-User-Id", 1L))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Cannot modify a SUBMITTED timesheet"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Nested
    @DisplayName("ResourceNotFoundException → 404")
    class ResourceNotFoundTests {

        @Test
        @DisplayName("Returns 404 with exception message")
        void handleResourceNotFound_Returns404() throws Exception {
            when(timesheetService.getAllTimesheet(anyLong()))
                    .thenThrow(new ResourceNotFoundException("Timesheet not found"));

            mockMvc.perform(get("/timesheet/my-timesheets").header("X-User-Id", 1L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Timesheet not found"));
        }
    }

    @Nested
    @DisplayName("UsernameNotFoundException → 404")
    class UsernameNotFoundTests {

        @Test
        @DisplayName("Returns 404 with exception message")
        void handleUsernameNotFound_Returns404() throws Exception {
            when(timesheetService.getAllTimesheet(anyLong()))
                    .thenThrow(new UsernameNotFoundException("User not found"));

            mockMvc.perform(get("/timesheet/my-timesheets").header("X-User-Id", 1L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("User not found"));
        }
    }

    @Nested
    @DisplayName("AccessDeniedException → 403")
    class AccessDeniedTests {

        @Test
        @DisplayName("Returns 403 with fixed 'Access Denied' message")
        void handleAccessDenied_Returns403() throws Exception {
            when(timesheetService.getAllTimesheet(anyLong()))
                    .thenThrow(new AccessDeniedException("forbidden"));

            mockMvc.perform(get("/timesheet/my-timesheets").header("X-User-Id", 1L))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.message").value("Access Denied"));
        }
    }

    @Nested
    @DisplayName("RuntimeException → 400")
    class RuntimeExceptionTests {

        @Test
        @DisplayName("Returns 400 with exception message")
        void handleRuntimeException_Returns400() throws Exception {
            when(timesheetService.getAllTimesheet(anyLong()))
                    .thenThrow(new RuntimeException("Unexpected error"));

            mockMvc.perform(get("/timesheet/my-timesheets").header("X-User-Id", 1L))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Unexpected error"));
        }
    }

    @Nested
    @DisplayName("MethodArgumentNotValidException → 400 with field errors")
    class ValidationExceptionTests {

        @Test
        @DisplayName("Returns 400 with validationErrors map for invalid POST body")
        void handleValidation_Returns400WithFieldErrors() throws Exception {
            mockMvc.perform(post("/timesheet/entries")
                            .header("X-User-Id", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Validation Failed"))
                    .andExpect(jsonPath("$.message").value("Invalid input data"))
                    .andExpect(jsonPath("$.validationErrors").exists());
        }
    }

    @Nested
    @DisplayName("ErrorResponse structure")
    class ErrorResponseStructureTests {

        @Test
        @DisplayName("All base fields present: timestamp, status, error, message, path")
        void errorResponseHasAllBaseFields() throws Exception {
            when(timesheetService.getAllTimesheet(anyLong()))
                    .thenThrow(new InvalidOperationException("some error"));

            mockMvc.perform(get("/timesheet/my-timesheets").header("X-User-Id", 1L))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").exists())
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.path").value("/timesheet/my-timesheets"));
        }
    }
}