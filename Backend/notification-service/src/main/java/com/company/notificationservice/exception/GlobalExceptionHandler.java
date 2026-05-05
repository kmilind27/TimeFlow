package com.company.notificationservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 🔴 Generic Runtime Exception
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(
            RuntimeException ex,
            HttpServletRequest request) {

        return buildError(ex, request, HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // 🔴 Catch-All Exception
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request) {

        return buildError(ex, request, HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    // 🔧 Common builder method
    private ResponseEntity<ErrorResponse> buildError(
            Exception ex,
            HttpServletRequest request,
            HttpStatus status,
            String message) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message != null ? message : ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(error, status);
    }
}
