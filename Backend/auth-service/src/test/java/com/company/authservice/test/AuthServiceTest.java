package com.company.authservice.test;

import com.company.authservice.dto.*;
import com.company.authservice.model.User;
import com.company.authservice.repository.UserRepository;
import com.company.authservice.security.JwtService;
import com.company.authservice.service.AuthService;
import com.company.authservice.service.OtpService;
import com.company.authservice.event.EventPublisher;
import com.company.authservice.event.OtpEvent;
import com.company.authservice.event.UserRegisteredEvent;
import com.company.authservice.exception.InvalidOtpException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// ✅ @ExtendWith(MockitoExtension.class) — tells JUnit
// to use Mockito for creating mocks automatically
@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Service Tests")
class AuthServiceTest {

    // ✅ @Mock creates a fake/mock version of the class
    // No real database calls — all controlled by us
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private OtpService otpService;

    // ✅ @InjectMocks creates real AuthService
    // and injects all @Mock objects into it
    @InjectMocks
    private AuthService authService;

    // ✅ Test data — reused across tests
    private User mockUser;
    private SignupRequest signupRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        // Build a mock user for testing
        mockUser = User.builder()
                .id(1L)
                .employeeCode("EMP001")
                .fullName("John Doe")
                .email("john@example.com")
                .password("$2a$10$encodedPassword")
                .role("EMPLOYEE")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

        // Build signup request
        signupRequest = new SignupRequest();
        signupRequest.setEmployeeCode("EMP001");
        signupRequest.setFullName("John Doe");
        signupRequest.setEmail("john@example.com");
        signupRequest.setPassword("Password@123");

        // Build login request
        loginRequest = new LoginRequest();
        loginRequest.setEmail("john@example.com");
        loginRequest.setPassword("Password@123");
    }

    // ═══════════════════════════════════════════════
    // SIGNUP TESTS
    // ═══════════════════════════════════════════════

    @Nested
    @DisplayName("Signup Tests")
    class SignupTests {

        @Test
        @DisplayName("Should signup successfully")
        void signup_Success() {
            // ✅ ARRANGE — set up mock behavior
            // When userRepository.existsByEmail is called
            // with any string, return false (email not taken)
            when(userRepository.existsByEmail(anyString()))
                    .thenReturn(false);
            when(userRepository.existsByEmployeeCode(
                    anyString()))
                    .thenReturn(false);
            when(passwordEncoder.encode(anyString()))
                    .thenReturn("$2a$10$encodedPassword");
            when(userRepository.save(any(User.class)))
                    .thenReturn(mockUser);

            // ✅ ACT — call the method we're testing
            assertDoesNotThrow(() ->
                authService.signup(signupRequest));

            // ✅ ASSERT — verify interactions
            // verify() checks if a method was called
            verify(userRepository, times(1))
                    .save(any(User.class));
            verify(passwordEncoder, times(1))
                    .encode("Password@123");
            verify(eventPublisher, times(1))
                    .publishUserRegistered(any(UserRegisteredEvent.class));
        }

        @Test
        @DisplayName("Should throw exception when email exists")
        void signup_EmailAlreadyExists() {
            // Email already registered
            when(userRepository.existsByEmail(
                    "john@example.com"))
                    .thenReturn(true);

            // ✅ assertThrows — expects exception to be thrown
            RuntimeException exception =
                assertThrows(RuntimeException.class,
                    () -> authService.signup(signupRequest));

            assertEquals(
                "Email already registered: john@example.com",
                exception.getMessage());

            // Verify save was NEVER called
            verify(userRepository, never())
                    .save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when employee code exists")
        void signup_EmployeeCodeAlreadyExists() {
            when(userRepository.existsByEmail(anyString()))
                    .thenReturn(false);
            when(userRepository.existsByEmployeeCode(
                    "EMP001"))
                    .thenReturn(true);

            RuntimeException exception =
                assertThrows(RuntimeException.class,
                    () -> authService.signup(signupRequest));

            assertEquals(
                "Employee code already exists: EMP001",
                exception.getMessage());
        }

        @Test
        @DisplayName("Should always set role to EMPLOYEE")
        void signup_AlwaysSetsEmployeeRole() {
            when(userRepository.existsByEmail(anyString()))
                    .thenReturn(false);
            when(userRepository.existsByEmployeeCode(
                    anyString()))
                    .thenReturn(false);
            when(passwordEncoder.encode(anyString()))
                    .thenReturn("encoded");

            // Capture the User object saved to DB
            // So we can verify its role
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> {
                        User savedUser =
                            invocation.getArgument(0);
                        // ✅ Verify role is always EMPLOYEE
                        assertEquals("EMPLOYEE",
                            savedUser.getRole());
                        return savedUser;
                    });

            authService.signup(signupRequest);
        }
    }

    // ═══════════════════════════════════════════════
    // LOGIN TESTS
    // ═══════════════════════════════════════════════

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully and return JWT")
        void login_Success() {
            // Authentication succeeds (no exception thrown)
            when(authenticationManager.authenticate(
                    any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null); // return value ignored

            when(userRepository.findByEmail(
                    "john@example.com"))
                    .thenReturn(Optional.of(mockUser));

            when(jwtService.generateToken(mockUser))
                    .thenReturn("mocked.jwt.token");

            AuthResponse response =
                authService.login(loginRequest);

            // ✅ Assert response values
            assertNotNull(response);
            assertEquals("mocked.jwt.token",
                response.getToken());
            assertEquals("john@example.com",
                response.getEmail());
            assertEquals("John Doe",
                response.getFullName());
            assertEquals("EMPLOYEE",
                response.getRole());
            assertEquals(1L, response.getUserId());
        }

        @Test
        @DisplayName("Should throw exception for invalid credentials")
        void login_InvalidCredentials() {
            // Authentication fails — throws exception
            when(authenticationManager.authenticate(
                    any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException(
                            "Bad credentials"));

            RuntimeException exception =
                assertThrows(RuntimeException.class,
                    () -> authService.login(loginRequest));

            assertEquals("Invalid email or password",
                exception.getMessage());

            // JWT should never be generated
            verify(jwtService, never())
                    .generateToken(any());
        }

        @Test
        @DisplayName("Should throw exception when user not found after auth")
        void login_UserNotFound() {
            when(authenticationManager.authenticate(
                    any()))
                    .thenReturn(null);

            when(userRepository.findByEmail(anyString()))
                    .thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                () -> authService.login(loginRequest));
        }
    }

    // ═══════════════════════════════════════════════
    // CHANGE ROLE TESTS
    // ═══════════════════════════════════════════════

    @Nested
    @DisplayName("Change Role Tests")
    class ChangeRoleTests {

        private User adminUser;
        private User employeeUser;
        private ChangeRoleRequest changeRoleRequest;

        @BeforeEach
        void setUp() {
            adminUser = User.builder()
                    .id(1L)
                    .email("admin@company.com")
                    .role("ADMIN")
                    .status("ACTIVE")
                    .build();

            employeeUser = User.builder()
                    .id(2L)
                    .email("john@example.com")
                    .role("EMPLOYEE")
                    .status("ACTIVE")
                    .fullName("John Doe")
                    .employeeCode("EMP001")
                    .build();

            changeRoleRequest = new ChangeRoleRequest();
            changeRoleRequest.setRole("MANAGER");
        }

        @Test
        @DisplayName("Admin should successfully change user role")
        void changeRole_Success() {
            when(userRepository.findByEmail("admin@company.com"))
                    .thenReturn(Optional.of(adminUser));
            when(userRepository.findById(2L))
                    .thenReturn(Optional.of(employeeUser));
            when(userRepository.save(any(User.class)))
                    .thenReturn(employeeUser);

            UserResponse response = authService.changeRole(
                    2L,
                    changeRoleRequest,
                    "admin@company.com");

            assertNotNull(response);
            verify(userRepository, times(1))
                    .save(any(User.class));
            verify(eventPublisher, times(1))
                    .publishUserRoleChanged(any(UserRegisteredEvent.class));
        }

        @Test
        @DisplayName("Non-admin should not change role")
        void changeRole_NotAdmin() {
            User nonAdmin = User.builder()
                    .id(3L)
                    .email("employee@company.com")
                    .role("EMPLOYEE")
                    .build();

            when(userRepository.findByEmail(
                    "employee@company.com"))
                    .thenReturn(Optional.of(nonAdmin));

            RuntimeException exception =
                assertThrows(RuntimeException.class,
                    () -> authService.changeRole(
                        2L,
                        changeRoleRequest,
                        "employee@company.com"));

            assertEquals("Only ADMIN can change roles",
                exception.getMessage());
        }

        @Test
        @DisplayName("Admin should not change own role")
        void changeRole_AdminCannotChangeOwnRole() {
            when(userRepository.findByEmail("admin@company.com"))
                    .thenReturn(Optional.of(adminUser));
            when(userRepository.findById(1L))
                    .thenReturn(Optional.of(adminUser));

            RuntimeException exception =
                assertThrows(RuntimeException.class,
                    () -> authService.changeRole(
                        1L,
                        changeRoleRequest,
                        "admin@company.com"));

            assertEquals(
                "Admin cannot change their own role",
                exception.getMessage());
        }
    }

    // ═══════════════════════════════════════════════
    // FORGOT PASSWORD (OTP FLOW) TESTS
    // ═══════════════════════════════════════════════

    @Nested
    @DisplayName("Forgot Password OTP Flow Tests")
    class ForgotPasswordTests {

        // ── Step 1: Request OTP ──────────────────────

        @Test
        @DisplayName("Should request OTP successfully")
        void requestPasswordReset_Success() {
            ForgotPasswordOtpRequest request =
                new ForgotPasswordOtpRequest();
            request.setEmail("john@example.com");

            when(userRepository.findByEmail(
                    "john@example.com"))
                    .thenReturn(Optional.of(mockUser));
            when(otpService.generateOtp("john@example.com"))
                    .thenReturn("123456");

            assertDoesNotThrow(() ->
                authService.requestPasswordReset(request));

            verify(otpService, times(1))
                    .generateOtp("john@example.com");
            verify(eventPublisher, times(1))
                    .publishOtpRequested(any(OtpEvent.class));
        }

        @Test
        @DisplayName("Should throw when email not found for OTP request")
        void requestPasswordReset_EmailNotFound() {
            ForgotPasswordOtpRequest request =
                new ForgotPasswordOtpRequest();
            request.setEmail("unknown@example.com");

            when(userRepository.findByEmail(
                    "unknown@example.com"))
                    .thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                () -> authService.requestPasswordReset(request));

            verify(otpService, never())
                    .generateOtp(anyString());
        }

        // ── Step 2: Verify OTP ───────────────────────

        @Test
        @DisplayName("Should verify OTP and return reset token")
        void verifyOtp_Success() {
            VerifyOtpRequest request = new VerifyOtpRequest();
            request.setEmail("john@example.com");
            request.setOtp("123456");

            when(userRepository.findByEmail(
                    "john@example.com"))
                    .thenReturn(Optional.of(mockUser));
            when(otpService.generateResetToken(
                    "john@example.com"))
                    .thenReturn("mock-reset-token");

            VerifyOtpResponse response =
                authService.verifyOtp(request);

            assertNotNull(response);
            assertEquals("mock-reset-token",
                response.getResetToken());
            assertEquals("OTP verified successfully",
                response.getMessage());

            verify(otpService, times(1))
                    .verifyOtp("john@example.com", "123456");
            verify(otpService, times(1))
                    .generateResetToken("john@example.com");
        }

        @Test
        @DisplayName("Should throw when OTP is invalid")
        void verifyOtp_InvalidOtp() {
            VerifyOtpRequest request = new VerifyOtpRequest();
            request.setEmail("john@example.com");
            request.setOtp("000000");

            when(userRepository.findByEmail(
                    "john@example.com"))
                    .thenReturn(Optional.of(mockUser));
            doThrow(new InvalidOtpException("Invalid OTP"))
                    .when(otpService)
                    .verifyOtp("john@example.com", "000000");

            assertThrows(InvalidOtpException.class,
                () -> authService.verifyOtp(request));

            verify(otpService, never())
                    .generateResetToken(anyString());
        }

        // ── Step 3: Reset Password ───────────────────

        @Test
        @DisplayName("Should reset password with valid token")
        void resetPassword_Success() {
            ForgotPasswordRequest request =
                new ForgotPasswordRequest();
            request.setResetToken("valid-token");
            request.setNewPassword("NewPass@123");
            request.setConfirmPassword("NewPass@123");

            when(otpService.validateResetToken("valid-token"))
                    .thenReturn("john@example.com");
            when(userRepository.findByEmail(
                    "john@example.com"))
                    .thenReturn(Optional.of(mockUser));
            when(passwordEncoder.encode(anyString()))
                    .thenReturn("$2a$10$newEncodedPassword");

            assertDoesNotThrow(() ->
                authService.resetPassword(request));

            verify(userRepository, times(1))
                    .save(any(User.class));
        }

        @Test
        @DisplayName("Should throw when passwords don't match")
        void resetPassword_PasswordMismatch() {
            ForgotPasswordRequest request =
                new ForgotPasswordRequest();
            request.setResetToken("valid-token");
            request.setNewPassword("NewPass@123");
            request.setConfirmPassword("Different@123");

            RuntimeException exception =
                assertThrows(RuntimeException.class,
                    () -> authService.resetPassword(request));

            assertEquals("Passwords do not match",
                exception.getMessage());

            verify(userRepository, never())
                    .save(any());
        }

        @Test
        @DisplayName("Should throw when reset token is invalid")
        void resetPassword_InvalidToken() {
            ForgotPasswordRequest request =
                new ForgotPasswordRequest();
            request.setResetToken("invalid-token");
            request.setNewPassword("NewPass@123");
            request.setConfirmPassword("NewPass@123");

            when(otpService.validateResetToken("invalid-token"))
                    .thenThrow(new InvalidOtpException(
                            "Invalid or expired reset token"));

            assertThrows(InvalidOtpException.class,
                () -> authService.resetPassword(request));

            verify(userRepository, never())
                    .save(any());
        }
    }
    
 // ═══════════════════════════════════════════════
 // UPDATE MANAGER TESTS
 // ═══════════════════════════════════════════════

 @Nested
 @DisplayName("Update Manager Tests")
 class UpdateManagerTests {

     private UpdateManagerRequest updateManagerRequest;

     @BeforeEach
     void setUp() {
         updateManagerRequest = new UpdateManagerRequest();
         updateManagerRequest.setManagerId(2L);
     }

     @Test
     @DisplayName("Should update manager successfully")
     void updateManager_Success() {
         User employee = User.builder()
                 .id(2L)
                 .email("john@example.com")
                 .role("EMPLOYEE")
                 .fullName("John Doe")
                 .employeeCode("EMP001")
                 .status("ACTIVE")
                 .build();

         when(userRepository.findById(2L))
                 .thenReturn(Optional.of(employee));
         when(userRepository.save(any(User.class)))
                 .thenReturn(employee);

         // Admin updating employee's manager
         // admin email != employee email → allowed
         UserResponse response = authService.updateManager(
                 2L,
                 "admin@company.com", // requester
                 updateManagerRequest);

         assertNotNull(response);
         verify(userRepository, times(1))
                 .save(any(User.class));
     }

     @Test
     @DisplayName("Should throw when admin tries to set own manager")
     void updateManager_AdminCannotSetOwnManager() {
         User admin = User.builder()
                 .id(1L)
                 .email("admin@company.com")
                 .role("ADMIN")
                 .status("ACTIVE")
                 .build();

         when(userRepository.findById(1L))
                 .thenReturn(Optional.of(admin));

         // Admin trying to update their own manager
         RuntimeException exception =
             assertThrows(RuntimeException.class,
                 () -> authService.updateManager(
                     1L,
                     "admin@company.com", // same email!
                     updateManagerRequest));

         assertEquals(
             "Admin cannot set their own manager",
             exception.getMessage());

         verify(userRepository, never()).save(any());
     }

     @Test
     @DisplayName("Should throw when user not found")
     void updateManager_UserNotFound() {
         when(userRepository.findById(99L))
                 .thenReturn(Optional.empty());

         RuntimeException exception =
             assertThrows(RuntimeException.class,
                 () -> authService.updateManager(
                     99L,
                     "admin@company.com",
                     updateManagerRequest));

         assertTrue(exception.getMessage()
             .contains("User not found with id: 99"));
     }
 }

 // ═══════════════════════════════════════════════
 // CHANGE STATUS TESTS
 // ═══════════════════════════════════════════════

 @Nested
 @DisplayName("Change Status Tests")
 class ChangeStatusTests {

     private User adminUser;
     private User employeeUser;
     private ChangeStatusRequest changeStatusRequest;

     @BeforeEach
     void setUp() {
         adminUser = User.builder()
                 .id(1L)
                 .email("admin@company.com")
                 .role("ADMIN")
                 .status("ACTIVE")
                 .fullName("Admin")
                 .employeeCode("ADM001")
                 .build();

         employeeUser = User.builder()
                 .id(2L)
                 .email("john@example.com")
                 .role("EMPLOYEE")
                 .status("ACTIVE")
                 .fullName("John Doe")
                 .employeeCode("EMP001")
                 .build();

         changeStatusRequest = new ChangeStatusRequest();
         changeStatusRequest.setStatus("INACTIVE");
     }

     @Test
     @DisplayName("Admin should change user status successfully")
     void changeStatus_Success() {
         when(userRepository.findByEmail(
                 "admin@company.com"))
                 .thenReturn(Optional.of(adminUser));
         when(userRepository.findById(2L))
                 .thenReturn(Optional.of(employeeUser));
         when(userRepository.save(any(User.class)))
                 .thenReturn(employeeUser);

         UserResponse response = authService.changeStatus(
                 2L,
                 changeStatusRequest,
                 "admin@company.com");

         assertNotNull(response);
         verify(userRepository, times(1))
                 .save(any(User.class));
     }

     @Test
     @DisplayName("Should activate user successfully")
     void changeStatus_Activate() {
         employeeUser.setStatus("INACTIVE");

         ChangeStatusRequest activateRequest =
             new ChangeStatusRequest();
         activateRequest.setStatus("ACTIVE");

         when(userRepository.findByEmail(
                 "admin@company.com"))
                 .thenReturn(Optional.of(adminUser));
         when(userRepository.findById(2L))
                 .thenReturn(Optional.of(employeeUser));
         when(userRepository.save(any(User.class)))
                 .thenAnswer(inv -> {
                     User saved = inv.getArgument(0);
                     assertEquals("ACTIVE", saved.getStatus());
                     return saved;
                 });

         authService.changeStatus(
                 2L, activateRequest, "admin@company.com");

         verify(userRepository, times(1)).save(any());
     }

     @Test
     @DisplayName("Non-admin should not change status")
     void changeStatus_NotAdmin() {
         User nonAdmin = User.builder()
                 .id(3L)
                 .email("employee@company.com")
                 .role("EMPLOYEE")
                 .build();

         when(userRepository.findByEmail(
                 "employee@company.com"))
                 .thenReturn(Optional.of(nonAdmin));

         RuntimeException exception =
             assertThrows(RuntimeException.class,
                 () -> authService.changeStatus(
                     2L,
                     changeStatusRequest,
                     "employee@company.com"));

         assertEquals("Only ADMIN can change status",
             exception.getMessage());

         verify(userRepository, never()).save(any());
     }

     @Test
     @DisplayName("Admin should not change own status")
     void changeStatus_AdminCannotChangeOwnStatus() {
         when(userRepository.findByEmail(
                 "admin@company.com"))
                 .thenReturn(Optional.of(adminUser));
         when(userRepository.findById(1L))
                 .thenReturn(Optional.of(adminUser));

         RuntimeException exception =
             assertThrows(RuntimeException.class,
                 () -> authService.changeStatus(
                     1L,
                     changeStatusRequest,
                     "admin@company.com"));

         assertEquals(
             "Admin cannot change their own status",
             exception.getMessage());
     }

     @Test
     @DisplayName("Should throw when target user not found")
     void changeStatus_UserNotFound() {
         when(userRepository.findByEmail(
                 "admin@company.com"))
                 .thenReturn(Optional.of(adminUser));
         when(userRepository.findById(99L))
                 .thenReturn(Optional.empty());

         RuntimeException exception =
             assertThrows(RuntimeException.class,
                 () -> authService.changeStatus(
                     99L,
                     changeStatusRequest,
                     "admin@company.com"));

         assertTrue(exception.getMessage()
             .contains("User not found with id: 99"));
     }

     @Test
     @DisplayName("Should throw when requester not found")
     void changeStatus_RequesterNotFound() {
         when(userRepository.findByEmail(
                 "unknown@company.com"))
                 .thenReturn(Optional.empty());

         assertThrows(RuntimeException.class,
             () -> authService.changeStatus(
                 2L,
                 changeStatusRequest,
                 "unknown@company.com"));
     }
 }

 // ═══════════════════════════════════════════════
 // DELETE USER TESTS
 // ═══════════════════════════════════════════════

 @Nested
 @DisplayName("Delete User Tests")
 class DeleteUserTests {

     @Test
     @DisplayName("Should soft delete user successfully")
     void deleteUser_Success() {
         when(userRepository.findById(1L))
                 .thenReturn(Optional.of(mockUser));
         when(userRepository.save(any(User.class)))
                 .thenAnswer(inv -> {
                     User saved = inv.getArgument(0);
                     // ✅ Verify soft delete behavior
                     assertEquals("DELETED",
                         saved.getStatus());
                     assertTrue(saved.getEmail()
                         .startsWith("deleted_"));
                     assertEquals("",
                         saved.getPassword());
                     return saved;
                 });

         // Should not throw
         assertDoesNotThrow(() ->
             authService.deleteUserById(1L));

         // Verify save called (soft delete)
         verify(userRepository, times(1))
                 .save(any(User.class));

         // Verify event published
         verify(eventPublisher, times(1))
                 .publishUserDeleted(any());
     }

     @Test
     @DisplayName("Should throw when user not found for delete")
     void deleteUser_NotFound() {
         when(userRepository.findById(99L))
                 .thenReturn(Optional.empty());

         RuntimeException exception =
             assertThrows(RuntimeException.class,
                 () -> authService.deleteUserById(99L));

         assertTrue(exception.getMessage()
             .contains("User not found with id: 99"));

         // Save should never be called
         verify(userRepository, never()).save(any());

         // Event should never be published
         verify(eventPublisher, never())
                 .publishUserDeleted(any());
     }

     @Test
     @DisplayName("Soft delete should wipe sensitive data")
     void deleteUser_WipesSensitiveData() {
         mockUser.setPassword("someEncodedPassword");
         mockUser.setEmail("john@example.com");

         when(userRepository.findById(1L))
                 .thenReturn(Optional.of(mockUser));
         when(userRepository.save(any(User.class)))
                 .thenReturn(mockUser);

         authService.deleteUserById(1L);

         // Capture what was saved
         verify(userRepository).save(argThat(user ->
             user.getStatus().equals("DELETED") &&
             user.getPassword().equals("") &&
             user.getEmail().contains("deleted_")
         ));
     }
 }

 // ═══════════════════════════════════════════════
 // GET PROFILE TESTS
 // ═══════════════════════════════════════════════

 @Nested
 @DisplayName("Get Profile Tests")
 class GetProfileTests {

     @Test
     @DisplayName("Should return profile successfully")
     void getProfile_Success() {
         when(userRepository.findByEmail(
                 "john@example.com"))
                 .thenReturn(Optional.of(mockUser));

         UserResponse response =
             authService.getProfile("john@example.com");

         assertNotNull(response);
         assertEquals("john@example.com",
             response.getEmail());
         assertEquals("John Doe",
             response.getFullName());
         assertEquals("EMPLOYEE", response.getRole());
         assertEquals("ACTIVE", response.getStatus());
     }

     @Test
     @DisplayName("Should throw when profile email not found")
     void getProfile_NotFound() {
         when(userRepository.findByEmail(
                 "unknown@example.com"))
                 .thenReturn(Optional.empty());

         RuntimeException exception =
             assertThrows(RuntimeException.class,
                 () -> authService.getProfile(
                     "unknown@example.com"));

         assertTrue(exception.getMessage()
             .contains("User not found"));
     }
 }

 // ═══════════════════════════════════════════════
 // MAP TO USER RESPONSE TESTS
 // (Testing null-safe defaults in mapToUserResponse)
 // ═══════════════════════════════════════════════

 @Nested
 @DisplayName("Null Safety Tests")
 class NullSafetyTests {

     @Test
     @DisplayName("Should handle null fields with defaults")
     void getUserById_NullFields_DefaultsApplied() {
         // User with null fields
         User userWithNulls = User.builder()
                 .id(5L)
                 .email("test@example.com")
                 .employeeCode(null)   // null
                 .fullName(null)       // null
                 .role(null)           // null
                 .status(null)         // null
                 .managerId(null)      // null
                 .build();

         when(userRepository.findById(5L))
                 .thenReturn(Optional.of(userWithNulls));

         UserResponse response =
             authService.getUserById(5L);

         // ✅ Verify defaults are applied
         assertEquals("Code not assigned",
             response.getEmployeeCode());
         assertEquals("Full name not provided",
             response.getFullName());
         assertEquals("Role information unavailable",
             response.getRole());
         assertEquals("Status unconfirmed",
             response.getStatus());
         assertEquals(0L, response.getManagerId());
     }

     @Test
     @DisplayName("Should return actual values when fields present")
     void getUserById_AllFieldsPresent() {
         when(userRepository.findById(1L))
                 .thenReturn(Optional.of(mockUser));

         UserResponse response =
             authService.getUserById(1L);

         assertEquals("EMP001",
             response.getEmployeeCode());
         assertEquals("John Doe",
             response.getFullName());
         assertEquals("EMPLOYEE", response.getRole());
         assertEquals("ACTIVE", response.getStatus());
         assertNotNull(response.getCreatedAt());
     }
 }

 // ═══════════════════════════════════════════════
 // EDGE CASE TESTS
 // ═══════════════════════════════════════════════

 @Nested
 @DisplayName("Edge Case Tests")
 class EdgeCaseTests {

     @Test
     @DisplayName("Signup should publish event after save")
     void signup_PublishesEventAfterSave() {
         when(userRepository.existsByEmail(anyString()))
                 .thenReturn(false);
         when(userRepository.existsByEmployeeCode(
                 anyString()))
                 .thenReturn(false);
         when(passwordEncoder.encode(anyString()))
                 .thenReturn("encoded");
         when(userRepository.save(any(User.class)))
                 .thenReturn(mockUser);

         authService.signup(signupRequest);

         // ✅ Verify order: save first, then publish event
         var inOrder = inOrder(
             userRepository, eventPublisher);
         inOrder.verify(userRepository)
                .save(any(User.class));
         inOrder.verify(eventPublisher)
                .publishUserRegistered(any());
     }

     @Test
     @DisplayName("Signup should return welcome message")
     void signup_ReturnsWelcomeMessage() {
         when(userRepository.existsByEmail(anyString()))
                 .thenReturn(false);
         when(userRepository.existsByEmployeeCode(
                 anyString()))
                 .thenReturn(false);
         when(passwordEncoder.encode(anyString()))
                 .thenReturn("encoded");
         when(userRepository.save(any(User.class)))
                 .thenReturn(mockUser);

         String result = authService.signup(signupRequest);

         assertNotNull(result);
         assertTrue(result.contains(
             "Registration successful"));
         assertTrue(result.contains("John Doe"));
     }

     @Test
     @DisplayName("Login throws BadCredentialsException not RuntimeException")
     void login_ThrowsCorrectExceptionType() {
         when(authenticationManager.authenticate(any()))
                 .thenThrow(new BadCredentialsException(
                     "Bad credentials"));

         // ✅ BadCredentialsException IS a RuntimeException
         // but verify exact type
         assertThrows(BadCredentialsException.class,
             () -> authService.login(loginRequest));
     }

     @Test
     @DisplayName("Change role publishes event with new role")
     void changeRole_PublishesEventWithNewRole() {
         User adminUser = User.builder()
                 .id(1L)
                 .email("admin@company.com")
                 .role("ADMIN")
                 .status("ACTIVE")
                 .fullName("Admin")
                 .employeeCode("ADM001")
                 .build();

         User employeeUser = User.builder()
                 .id(2L)
                 .email("john@example.com")
                 .role("EMPLOYEE")
                 .status("ACTIVE")
                 .fullName("John Doe")
                 .employeeCode("EMP001")
                 .build();

         ChangeRoleRequest request = new ChangeRoleRequest();
         request.setRole("MANAGER");

         when(userRepository.findByEmail(
                 "admin@company.com"))
                 .thenReturn(Optional.of(adminUser));
         when(userRepository.findById(2L))
                 .thenReturn(Optional.of(employeeUser));
         when(userRepository.save(any(User.class)))
                 .thenReturn(employeeUser);

         authService.changeRole(
             2L, request, "admin@company.com");

         // Verify event published with correct role
         verify(eventPublisher, times(1))
             .publishUserRoleChanged(
                 argThat(event ->
                     event.getRole().equals("MANAGER") &&
                     event.getEmail().equals(
                         "john@example.com")));
     }

     @Test
     @DisplayName("getAllUsers returns correct count")
     void getAllUsers_ReturnsCorrectCount() {
         List<User> users = List.of(
             mockUser,
             User.builder().id(2L)
                 .email("a@a.com")
                 .role("EMPLOYEE")
                 .status("ACTIVE")
                 .build(),
             User.builder().id(3L)
                 .email("b@b.com")
                 .role("MANAGER")
                 .status("ACTIVE")
                 .build()
         );

         when(userRepository.findAll())
                 .thenReturn(users);

         List<UserResponse> result =
             authService.getAllUsers();

         assertEquals(3, result.size());
     }
 }

    // ═══════════════════════════════════════════════
    // GET ALL USERS TESTS
    // ═══════════════════════════════════════════════

    @Nested
    @DisplayName("Get Users Tests")
    class GetUsersTests {

        @Test
        @DisplayName("Should return all users")
        void getAllUsers_Success() {
            User user2 = User.builder()
                    .id(2L)
                    .employeeCode("EMP002")
                    .fullName("Jane Doe")
                    .email("jane@example.com")
                    .role("EMPLOYEE")
                    .status("ACTIVE")
                    .build();

            when(userRepository.findAll())
                    .thenReturn(List.of(mockUser, user2));

            List<UserResponse> users =
                authService.getAllUsers();

            assertNotNull(users);
            assertEquals(2, users.size());
            assertEquals("john@example.com",
                users.get(0).getEmail());
            assertEquals("jane@example.com",
                users.get(1).getEmail());
        }

        @Test
        @DisplayName("Should return empty list when no users")
        void getAllUsers_EmptyList() {
            when(userRepository.findAll())
                    .thenReturn(List.of());

            List<UserResponse> users =
                authService.getAllUsers();

            assertNotNull(users);
            assertTrue(users.isEmpty());
        }

        @Test
        @DisplayName("Should return user by id")
        void getUserById_Success() {
            when(userRepository.findById(1L))
                    .thenReturn(Optional.of(mockUser));

            UserResponse response =
                authService.getUserById(1L);

            assertNotNull(response);
            assertEquals(1L, response.getId());
            assertEquals("john@example.com",
                response.getEmail());
        }

        @Test
        @DisplayName("Should throw when user id not found")
        void getUserById_NotFound() {
            when(userRepository.findById(99L))
                    .thenReturn(Optional.empty());

            RuntimeException exception =
                assertThrows(RuntimeException.class,
                    () -> authService.getUserById(99L));

            assertEquals("User not found with id: 99",
                exception.getMessage());
        }
    }
}