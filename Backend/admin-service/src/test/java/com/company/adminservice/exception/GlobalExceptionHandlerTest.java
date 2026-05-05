package com.company.adminservice.exception;

import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
        when(request.getRequestURI()).thenReturn("/admin/test");
    }

    // ════════════════════════════════════════════════════════
    // AccessDeniedException → 403
    // ════════════════════════════════════════════════════════

    @Test
    void handleAccessDenied_shouldReturn403() {
        AccessDeniedException ex = new AccessDeniedException("Access Denied");

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleAccessDenied(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(403);
        assertThat(response.getBody().getError()).isEqualTo("Forbidden");
        assertThat(response.getBody().getMessage()).isEqualTo("Access Denied");
        assertThat(response.getBody().getPath()).isEqualTo("/admin/test");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void handleAccessDenied_shouldIncludeCorrectPath() {
        when(request.getRequestURI()).thenReturn("/admin/users");
        AccessDeniedException ex = new AccessDeniedException("Access Denied");

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleAccessDenied(ex, request);

        assertThat(response.getBody().getPath()).isEqualTo("/admin/users");
    }

    // ════════════════════════════════════════════════════════
    // MethodArgumentNotValidException → 400
    // Triggered by @NotNull on holidayDate, @NotBlank on holidayName
    // ════════════════════════════════════════════════════════

    @Test
    void handleValidation_shouldReturn400WithFieldErrors() {
        MethodArgumentNotValidException ex =
                mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        // ✅ HolidayDto fields: holidayDate (@NotNull), holidayName (@NotBlank)
        FieldError fieldError = new FieldError(
                "holidayDto", "holidayDate", "Holiday date is required");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleValidation(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Validation Failed");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid input data");
        // ✅ validationErrors map contains field → message
        assertThat(response.getBody().getValidationErrors())
                .containsKey("holidayDate");
        assertThat(response.getBody().getValidationErrors())
        .containsEntry("holidayName", "Holiday name is required");
    }

    @Test
    void handleValidation_withHolidayNameError_shouldReturnCorrectField() {
        MethodArgumentNotValidException ex =
                mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        // ✅ holidayName @NotBlank validation
        FieldError fieldError = new FieldError(
                "holidayDto", "holidayName", "Holiday name is required");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleValidation(ex, request);

        assertThat(response.getBody().getValidationErrors())
                .containsKey("holidayName");
        assertThat(response.getBody().getValidationErrors().get("holidayName"))
                .isEqualTo("Holiday name is required");
    }

    @Test
    void handleValidation_withMultipleFieldErrors_shouldReturnAllErrors() {
        MethodArgumentNotValidException ex =
                mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        // ✅ Both HolidayDto validation fields failing together
        List<FieldError> errors = List.of(
                new FieldError("holidayDto", "holidayDate", "Holiday date is required"),
                new FieldError("holidayDto", "holidayName", "Holiday name is required")
        );

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(errors);

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleValidation(ex, request);

        assertThat(response.getBody().getValidationErrors()).hasSize(2);
        assertThat(response.getBody().getValidationErrors())
                .containsKeys("holidayDate", "holidayName");
    }

    @Test
    void handleValidation_withNoErrors_shouldReturnEmptyValidationMap() {
        MethodArgumentNotValidException ex =
                mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleValidation(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getValidationErrors()).isEmpty();
    }

    // ════════════════════════════════════════════════════════
    // FeignException — inter-service call failures
    // ════════════════════════════════════════════════════════

    @Test
    void handleFeignException_when404_shouldReturn404() {
        FeignException ex = mock(FeignException.class);
        when(ex.status()).thenReturn(404);
        when(ex.contentUTF8()).thenReturn("User not found");

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleFeignException(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
        // ✅ Your handler prepends "Downstream Service Error: "
        assertThat(response.getBody().getMessage())
                .contains("Downstream Service Error");
        assertThat(response.getBody().getMessage())
                .contains("User not found");
    }

    @Test
    void handleFeignException_when503_shouldReturn503() {
        FeignException ex = mock(FeignException.class);
        when(ex.status()).thenReturn(503);
        when(ex.contentUTF8()).thenReturn("Service unavailable");

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleFeignException(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody().getMessage()).contains("Service unavailable");
    }

    @Test
    void handleFeignException_whenInvalidStatus_shouldFallbackTo500() {
        FeignException ex = mock(FeignException.class);
        // ✅ HttpStatus.resolve(-1) returns null → falls back to INTERNAL_SERVER_ERROR
        when(ex.status()).thenReturn(-1);
        when(ex.contentUTF8()).thenReturn("Unknown error");

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleFeignException(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }

    @Test
    void handleFeignException_when400_shouldReturn400() {
        FeignException ex = mock(FeignException.class);
        when(ex.status()).thenReturn(400);
        when(ex.contentUTF8()).thenReturn("Bad request sent to auth service");

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleFeignException(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getMessage())
                .contains("Bad request sent to auth service");
    }

    // ════════════════════════════════════════════════════════
    // RuntimeException → 400
    // ════════════════════════════════════════════════════════

    @Test
    void handleRuntime_shouldReturn400() {
        RuntimeException ex = new RuntimeException("Something went wrong");

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleRuntime(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        // ✅ Your handler passes ex.getMessage() directly
        assertThat(response.getBody().getMessage()).isEqualTo("Something went wrong");
        assertThat(response.getBody().getPath()).isEqualTo("/admin/test");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void handleRuntime_shouldIncludeExactExceptionMessage() {
        RuntimeException ex =
                new RuntimeException("User not authorized to perform this action");

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleRuntime(ex, request);

        assertThat(response.getBody().getMessage())
                .isEqualTo("User not authorized to perform this action");
    }

    // ════════════════════════════════════════════════════════
    // Generic Exception → 500
    // ════════════════════════════════════════════════════════

    @Test
    void handleGeneric_shouldReturn500(){
        Exception ex = new Exception("Unexpected database error");

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleGeneric(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        // ✅ Your handler hardcodes this message — not from ex.getMessage()
        assertThat(response.getBody().getMessage())
                .isEqualTo("An unexpected error occurred");
    }

    @Test
    void handleGeneric_messageShouldBeHardcoded_notFromException(){
        Exception ex = new Exception("Sensitive internal DB error details");

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleGeneric(ex, request);

        // ✅ Hardcoded — sensitive details not exposed to client
        assertThat(response.getBody().getMessage())
                .isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().getMessage())
                .doesNotContain("Sensitive internal DB error details");
    }

    @Test
    void handleGeneric_shouldIncludeTimestampAndPath(){
        Exception ex = new Exception("Error");

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleGeneric(ex, request);

        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().getPath()).isEqualTo("/admin/test");
        assertThat(response.getBody().getStatus()).isEqualTo(500);
    }
}