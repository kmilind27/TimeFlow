package com.company.authservice.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.company.authservice.dto.AuthResponse;
import com.company.authservice.dto.ChangeRoleRequest;
import com.company.authservice.dto.ChangeStatusRequest;
import com.company.authservice.dto.ForgotPasswordOtpRequest;
import com.company.authservice.dto.ForgotPasswordRequest;
import com.company.authservice.dto.LoginRequest;
import com.company.authservice.dto.SignupRequest;
import com.company.authservice.dto.UpdateManagerRequest;
import com.company.authservice.dto.UserResponse;
import com.company.authservice.dto.VerifyOtpRequest;
import com.company.authservice.dto.VerifyOtpResponse;
import com.company.authservice.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication and management APIs")
public class AuthController {
	
	private final AuthService authService;
	
	@Operation(summary = "Register user", description = "Create a new employee account")
	@PostMapping("/signup")
	public ResponseEntity<String> signup(@Valid @RequestBody SignupRequest request){
		
		
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
	}
	
	@Operation(summary = "Login", description = "Authenticate and receive a JWT token")
	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request){
		
		AuthResponse authResponse = authService.login(request);
		
		return ResponseEntity.ok(authResponse);
	}
	
	@Operation(summary = "Forgot password - Step 1", description = "Request an OTP to be sent to the registered email")
	@PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @Valid @RequestBody
                ForgotPasswordOtpRequest request) {
        authService.requestPasswordReset(request);
        return ResponseEntity.ok(
            "OTP sent to your registered email");
    }

    @Operation(summary = "Verify OTP - Step 2", description = "Verify the OTP received via email and get a reset token")
    @PostMapping("/verify-otp")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(
            @Valid @RequestBody
                VerifyOtpRequest request) {
        return ResponseEntity.ok(
            authService.verifyOtp(request));
    }

    @Operation(summary = "Reset password - Step 3", description = "Reset password using the verified reset token")
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody
                ForgotPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(
            "Password reset successfully");
    }
	
	
	@Operation(summary = "Get my profile", description = "Fetch profile of the logged-in user")
	@GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile( @AuthenticationPrincipal UserDetails userDetails) {
        
		return ResponseEntity.ok(
            authService.getProfile(
                userDetails.getUsername()));
    }
	
	@Operation(summary = "Assign manager to employee", description = "[Admin] Assign a manager to an employee")
	@PatchMapping("/users/{id}/manager")
	@PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateManager(@PathVariable Long id,
            @AuthenticationPrincipal
                UserDetails userDetails,
            @Valid @RequestBody
                UpdateManagerRequest request) {
        return ResponseEntity.ok(
            authService.updateManager(
                id, userDetails.getUsername(), request));
    }
	
	@Operation(summary = "Change user role", description = "[Admin] Change a user's role (EMPLOYEE/MANAGER/ADMIN)")
	@PatchMapping("/users/{id}/role")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<UserResponse> changeRole(
	        @PathVariable Long id, @Valid @RequestBody ChangeRoleRequest request,
	        @AuthenticationPrincipal UserDetails userDetails) {

	    return ResponseEntity.ok(
	        authService.changeRole(
	            id,
	            request,
	            userDetails.getUsername()));
	}

	@Operation(summary = "Change user status", description = "[Admin] Activate or deactivate a user account")
	@PatchMapping("/users/{id}/status")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<UserResponse> changeStatus(
			@PathVariable Long id, @Valid @RequestBody ChangeStatusRequest request,
			@AuthenticationPrincipal UserDetails userDetails){
		
		return ResponseEntity.ok(authService.changeStatus(id, request, userDetails.getUsername()));
	}
	
	
	@Operation(summary = "Get all users", description = "[Admin] Internal endpoint for all users", hidden = true)
	@GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {

        return ResponseEntity.ok(
            authService.getAllUsers());
    }

	@Operation(summary = "Get user by ID", description = "[Admin] Internal endpoint for specific user", hidden = true)
	@GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long id) {
    	
        
        return ResponseEntity.ok(
            authService.getUserById(id));
    }
	
	@Operation(summary = "Health check", description = "[Admin] Verify auth service is running")
	@GetMapping("/health-check")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<String> healthCheck(){
		return ResponseEntity.ok("Auth Service is running!");
	}

    @Operation(summary = "Delete user", description = "[Admin] Internal endpoint for soft delete", hidden = true)
    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUserById(@PathVariable Long id) {
        authService.deleteUserById(id);
        return ResponseEntity.ok("User soft-deleted with id: " + id);
    }
}
