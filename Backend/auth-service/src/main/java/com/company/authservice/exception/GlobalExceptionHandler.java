package com.company.authservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 🔴 Invalid Login Credentials
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request) {

        return buildError(
                ex,
                request,
                HttpStatus.UNAUTHORIZED
        );
    }

    // 🔴 User Not Found
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UserNotFoundException ex,
            HttpServletRequest request) {

        return buildError(ex, request, HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(InvalidRoleChangeException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            InvalidRoleChangeException ex,
            HttpServletRequest request) {

        return buildError(ex, request, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UserAlreadyExistsException ex,
            HttpServletRequest request) {

        return buildError(ex, request, HttpStatus.BAD_REQUEST);
    }

    // 🔴 Invalid OTP / Reset Token
    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOtp(
            InvalidOtpException ex,
            HttpServletRequest request) {

        return buildError(ex, request, HttpStatus.BAD_REQUEST);
    }
    

    // 🔴 Access Denied
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        return buildError(ex, request, HttpStatus.FORBIDDEN);
    }

    // 🔴 JWT Expired
    @ExceptionHandler(io.jsonwebtoken.ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handleExpiredJwt(
            Exception ex,
            HttpServletRequest request) {

        return buildError(
                new Exception("JWT token has expired"),
                request,
                HttpStatus.UNAUTHORIZED             
        );
    }

    // 🔴 JWT Invalid
    @ExceptionHandler(io.jsonwebtoken.security.SecurityException.class)
    public ResponseEntity<ErrorResponse> handleInvalidJwt(
            Exception ex,
            HttpServletRequest request) {

        return buildError(
                new Exception("Invalid JWT token"),
                request,
                HttpStatus.UNAUTHORIZED
        );
    }

    // 🔴 Validation Errors (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Invalid input data")
                .path(request.getRequestURI())
                .validationErrors(errors)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // 🔴 Generic Runtime Exception
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(
            RuntimeException ex,
            HttpServletRequest request) {

        return buildError(ex, request, HttpStatus.BAD_REQUEST);
    }

    // 🔴 Catch-All Exception
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request) {

        return buildError(ex, request, HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }


    // 🔧 Common builder method (reusable)
    private ResponseEntity<ErrorResponse> buildError(
            Exception ex,
            HttpServletRequest request,
            HttpStatus status) {
        return buildError(ex, request, status, null);
    }

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