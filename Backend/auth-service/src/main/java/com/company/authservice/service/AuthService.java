
package com.company.authservice.service;

import java.util.List;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import com.company.authservice.event.EventPublisher;
import com.company.authservice.event.OtpEvent;
import com.company.authservice.event.UserRegisteredEvent;
import com.company.authservice.exception.InvalidRoleChangeException;
import com.company.authservice.exception.UserAlreadyExistsException;
import com.company.authservice.exception.UserNotFoundException;
import com.company.authservice.event.UserDeletedEvent;
import com.company.authservice.model.User;
import com.company.authservice.repository.UserRepository;
import com.company.authservice.security.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
	
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;
	private final EventPublisher eventPublisher;
	private final OtpService otpService;
	
	private static final String USER_NOT_FOUND = "User not found with id: ";
	
	public String signup(SignupRequest request) {
		
		if(userRepository.existsByEmail(request.getEmail())) {
			throw new UserAlreadyExistsException(
					"Email already registered: "+request.getEmail());
		}
		
		if(userRepository.existsByEmployeeCode(request.getEmployeeCode())) {
			throw new UserAlreadyExistsException(
					"Employee code already exists: "+request.getEmployeeCode());
		}
		
		User user = User.builder()
				.employeeCode(request.getEmployeeCode())
				.fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("EMPLOYEE")
                .managerId(request.getManagerId())
                .status("ACTIVE")
                .build();
		
		userRepository.save(user);
		
		eventPublisher.publishUserRegistered(UserRegisteredEvent.builder()
		        .userId(user.getId())
		        .email(user.getEmail())
		        .fullName(user.getFullName())
		        .employeeCode(user.getEmployeeCode())
		        .role(user.getRole())
		        .build());
	
		
		return "Registration successful!\nHello "+user.getFullName();
	}
	
	public AuthResponse login(LoginRequest request) {
		
		try {
			authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(
							request.getEmail(),
							request.getPassword()));
		}
		catch (AuthenticationException e) {
			throw new BadCredentialsException(
					"Invalid email or password");
		}
		
		 User user = userRepository.findByEmail(request.getEmail())
				 .orElseThrow(() -> 
				 new UserNotFoundException("User not found"));
		 
		 String token = jwtService.generateToken(user);
		 
		 return AuthResponse.builder()
				 	.token(token)
				 	.userId(user.getId())
	                .email(user.getEmail())
	                .fullName(user.getFullName())
	                .role(user.getRole())
	                .build();
	}
	
	// ─── Forgot Password (Step 1): Request OTP ────────────────

    public void requestPasswordReset(ForgotPasswordOtpRequest request) {
    	
    	User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException(
                        "No account found with email: "
                        + request.getEmail()));

        String otp = otpService.generateOtp(request.getEmail());

        eventPublisher.publishOtpRequested(OtpEvent.builder()
                .email(user.getEmail())
                .fullName(user.getFullName())
                .otp(otp)
                .build());

        log.info("OTP requested for password reset: {}", request.getEmail());
    }

    // ─── Forgot Password (Step 2): Verify OTP ────────────────

    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {

        // Verify user exists
        userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException(
                        "No account found with email: "
                        + request.getEmail()));

        otpService.verifyOtp(request.getEmail(), request.getOtp());

        String resetToken = otpService.generateResetToken(request.getEmail());

        return VerifyOtpResponse.builder()
                .resetToken(resetToken)
                .message("OTP verified successfully")
                .build();
    }

    // ─── Forgot Password (Step 3): Reset Password ────────────

    public void resetPassword(ForgotPasswordRequest request) {

        // Check passwords match
        if (!request.getNewPassword()
                    .equals(request.getConfirmPassword())) {
            throw new BadCredentialsException(
                "Passwords do not match");
        }

        // Validate the reset token and get email
        String email = otpService.validateResetToken(
                request.getResetToken());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(
                        "User not found"));

        user.setPassword(passwordEncoder.encode(
                request.getNewPassword()));
        userRepository.save(user);

        log.info("Password reset successfully for: {}", email);
    }
    
 // -------------------Get Profile---------------------

    public UserResponse getProfile(String email) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(
                        "User not found"));
        return mapToUserResponse(user);
    }

    @Transactional
    public void deleteUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND + id));
        
        // Soft delete
        user.setStatus("DELETED");
        user.setEmail("deleted_" + id + "@company.com");
        user.setPassword(""); // wipe password
        userRepository.save(user);

        eventPublisher.publishUserDeleted(
            UserDeletedEvent.builder()
                .userId(id)
                .email(user.getEmail())
                .build()
        );
    }
    
    public UserResponse updateManager(
    		Long userId,
            String email,
            UpdateManagerRequest request) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new UserNotFoundException(
                		USER_NOT_FOUND + userId));
        
        if (user.getEmail()
                .equals(email)) {
        	throw new InvalidRoleChangeException(
          "Admin cannot set their own manager");
        }
        
        user.setManagerId(request.getManagerId());
        

        userRepository.save(user);
        return mapToUserResponse(user);
    }
    
 // ─── Change Role/Status (Admin only) ──────────────────────

    public UserResponse changeRole(
            Long userId,
            ChangeRoleRequest request,
            String requestedByEmail) {

        User requester = userRepository.findByEmail(requestedByEmail)
                .orElseThrow(() -> new UserNotFoundException("Requester not found"));
        if (!"ADMIN".equals(requester.getRole())) {
            throw new InvalidRoleChangeException("Only ADMIN can change roles");
        }

        // ✅ Find target user
        User targetUser = userRepository
                .findById(userId)
                .orElseThrow(() -> new UserNotFoundException(
                		USER_NOT_FOUND + userId));

        // Prevent admin from changing their own role
        if (targetUser.getEmail()
                      .equals(requestedByEmail)) {
            throw new InvalidRoleChangeException(
                "Admin cannot change their own role");
        }

        String oldRole = targetUser.getRole();
        targetUser.setRole(request.getRole());
        userRepository.save(targetUser);

		eventPublisher.publishUserRoleChanged(UserRegisteredEvent.builder()
		        .userId(targetUser.getId())
		        .email(targetUser.getEmail())
		        .fullName(targetUser.getFullName())
		        .employeeCode(targetUser.getEmployeeCode())
		        .role(targetUser.getRole())
		        .build());

		log.debug("Role changed: "
            + targetUser.getEmail()
            + " from " + oldRole
            + " to " + request.getRole()
            + " by " + requestedByEmail);

        return mapToUserResponse(targetUser);
    }
    
    public UserResponse changeStatus(Long userId, ChangeStatusRequest request, String requestedByEmail) {
    	
        User requester = userRepository.findByEmail(requestedByEmail)
                .orElseThrow(() -> new UserNotFoundException("Requester not found"));
        if (!"ADMIN".equals(requester.getRole())) {
            throw new InvalidRoleChangeException("Only ADMIN can change status");
        }

    	User user = userRepository.findById(userId)
    				.orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND +userId));
    	
    	// Prevent admin from changing their own status
        if (user.getEmail()
                      .equals(requestedByEmail)) {
            throw new InvalidRoleChangeException(
                "Admin cannot change their own status");
        }
        
    	user.setStatus(request.getStatus());
    	
    	userRepository.save(user);
    	
    	return mapToUserResponse(user);
    }
	
	// ─── Internal Methods ──────────────────────────────

	public UserResponse getUserById(Long id) {
	    User user = userRepository.findById(id)
	            .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND + id));
	    return mapToUserResponse(user);
	}

	public List<UserResponse> getAllUsers() {
	    return userRepository.findAll()
	            .stream()
	            .map(this::mapToUserResponse)
	            .toList();
	}

	private UserResponse mapToUserResponse(User user) {
	    return UserResponse.builder()
	            .id(user.getId())
	            .employeeCode(user.getEmployeeCode() != null ? user.getEmployeeCode() : "Code not assigned")
	            .fullName(user.getFullName() != null ? user.getFullName() : "Full name not provided")
	            .email(user.getEmail())
	            .role(user.getRole() != null ? user.getRole() : "Role information unavailable")
	            .status(user.getStatus() != null ? user.getStatus() : "Status unconfirmed")
	            .managerId(user.getManagerId() != null ? user.getManagerId() : 0L)
	            .createdAt(user.getCreatedAt())
	            .build();
	}
}
