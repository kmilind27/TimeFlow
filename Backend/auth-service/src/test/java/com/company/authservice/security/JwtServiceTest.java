package com.company.authservice.security;

import com.company.authservice.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    // ✅ Must be at least 32 chars for HMAC-SHA256
    private static final String SECRET =
            "mySecretKeyForTestingPurposesOnly1234567890";
    private static final Long EXPIRATION = 3600000L; // 1 hour

    private User mockUser;
    private String validToken;

    @BeforeEach
    void setUp() {
        // ✅ Inject @Value fields via ReflectionTestUtils
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION);

        // ✅ User entity — used in generateToken (instanceof User check)
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("john@test.com");
        mockUser.setFullName("John Doe");
        mockUser.setRole("EMPLOYEE");
        mockUser.setPassword("encodedPassword");
        mockUser.setEmployeeCode("EMP001");
        mockUser.setStatus("ACTIVE");

        // Generate a real token for use in tests
        validToken = jwtService.generateToken(mockUser);
    }

    // ════════════════════════════════════════════════════════
    // generateToken — with User entity (extraClaims added)
    // ════════════════════════════════════════════════════════

    @Test
    void generateToken_shouldReturnNonNullToken() {
        String token = jwtService.generateToken(mockUser);

        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void generateToken_shouldContainEmailAsSubject() {
        String token = jwtService.generateToken(mockUser);

        String extractedEmail = jwtService.extractUsername(token);

        assertThat(extractedEmail).isEqualTo("john@test.com");
    }

    @Test
    void generateToken_shouldContainRoleClaim() {
        String token = jwtService.generateToken(mockUser);

        // ✅ extractRole uses claims.get("role", String.class)
        String role = jwtService.extractRole(token);

        assertThat(role).isEqualTo("EMPLOYEE");
    }

    @Test
    void generateToken_shouldContainUserIdClaim() {
        String token = jwtService.generateToken(mockUser);

        // ✅ extractUserId uses claims.get("userId", Long.class)
        Long userId = jwtService.extractUserId(token);

        assertThat(userId).isEqualTo(1L);
    }

    @Test
    void generateToken_withManagerRole_shouldContainCorrectRole() {
        mockUser.setRole("MANAGER");
        String token = jwtService.generateToken(mockUser);

        assertThat(jwtService.extractRole(token)).isEqualTo("MANAGER");
    }

    @Test
    void generateToken_withAdminRole_shouldContainCorrectRole() {
        mockUser.setRole("ADMIN");
        String token = jwtService.generateToken(mockUser);

        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void generateToken_withPlainUserDetails_shouldNotAddExtraClaims() {
        // ✅ Non-User UserDetails — instanceof check fails → no extraClaims
        UserDetails plainUserDetails =
                org.springframework.security.core.userdetails.User.builder()
                        .username("plain@test.com")
                        .password("password")
                        .authorities(Collections.emptyList())
                        .build();

        String token = jwtService.generateToken(plainUserDetails);

        assertThat(token).isNotNull();
        // subject should still be the username
        assertThat(jwtService.extractUsername(token)).isEqualTo("plain@test.com");
        // role claim will be null since no extraClaims added
        assertThat(jwtService.extractRole(token)).isNull();
    }

    // ════════════════════════════════════════════════════════
    // extractUsername
    // ════════════════════════════════════════════════════════

    @Test
    void extractUsername_shouldReturnEmailFromToken() {
        String email = jwtService.extractUsername(validToken);

        assertThat(email).isEqualTo("john@test.com");
    }

    @Test
    void extractUsername_differentUser_shouldReturnCorrectEmail() {
        mockUser.setEmail("admin@test.com");
        String token = jwtService.generateToken(mockUser);

        assertThat(jwtService.extractUsername(token)).isEqualTo("admin@test.com");
    }

    // ════════════════════════════════════════════════════════
    // extractRole
    // ════════════════════════════════════════════════════════

    @Test
    void extractRole_shouldReturnRoleFromToken() {
        String role = jwtService.extractRole(validToken);

        assertThat(role).isEqualTo("EMPLOYEE");
    }

    // ════════════════════════════════════════════════════════
    // extractUserId
    // ════════════════════════════════════════════════════════

    @Test
    void extractUserId_shouldReturnUserIdFromToken() {
        Long userId = jwtService.extractUserId(validToken);

        assertThat(userId).isEqualTo(1L);
    }

    @Test
    void extractUserId_differentId_shouldReturnCorrect() {
        mockUser.setId(99L);
        String token = jwtService.generateToken(mockUser);

        assertThat(jwtService.extractUserId(token)).isEqualTo(99L);
    }

    // ════════════════════════════════════════════════════════
    // isTokenValid
    // ════════════════════════════════════════════════════════

    @Test
    void isTokenValid_withCorrectUserAndValidToken_shouldReturnTrue() {
        boolean valid = jwtService.isTokenValid(validToken, mockUser);

        assertThat(valid).isTrue();
    }

    @Test
    void isTokenValid_withWrongUser_shouldReturnFalse() {
        // ✅ Different email → username mismatch
        mockUser.setEmail("different@test.com");

        boolean valid = jwtService.isTokenValid(validToken, mockUser);

        assertThat(valid).isFalse();
    }

    @Test
    void isTokenValid_withExpiredToken_shouldReturnFalse() {
        // ✅ Generate token with -1ms expiration → already expired
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1L);
        String expiredToken = jwtService.generateToken(mockUser);

        // Reset to normal expiration for validation
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION);
        mockUser.setEmail("john@test.com");

        // ✅ Expired token — isTokenExpired returns true
        assertThatThrownBy(() -> jwtService.isTokenValid(expiredToken, mockUser))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    // ════════════════════════════════════════════════════════
    // extractAllClaims — invalid token
    // ════════════════════════════════════════════════════════

    @Test
    void extractUsername_withInvalidToken_shouldThrowException() {
        assertThatThrownBy(() -> jwtService.extractUsername("invalid.token.here"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void extractUsername_withTamperedToken_shouldThrowException() {
        // Tamper the signature portion of the token
        String tamperedToken = validToken.substring(0,
                validToken.lastIndexOf('.') + 1) + "tamperedsignature";

        assertThatThrownBy(() -> jwtService.extractUsername(tamperedToken))
                .isInstanceOf(Exception.class);
    }

    @Test
    void extractUsername_withTokenSignedByDifferentKey_shouldThrowException() {
        // Sign with a different secret key
        SecretKey differentKey = Keys.hmacShaKeyFor(
                "differentSecretKeyForTestingPurposes1234".getBytes());

        String foreignToken = Jwts.builder()
                .subject("john@test.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(differentKey)
                .compact();

        assertThatThrownBy(() -> jwtService.extractUsername(foreignToken))
                .isInstanceOf(io.jsonwebtoken.security.SecurityException.class);
    }
}