package com.company.apigateway.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.security.Keys;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Util Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    // ✅ Must match what Auth Service uses
    private static final String SECRET =
        "3cfa76ef14937c1c0ea519f8fc057a80" +
        "fcd04a7420f8e8bcd0a7567c272e007b";

    // Pre-generated valid token for testing
    // Contains: email=john@example.com, role=EMPLOYEE, userId=1
    private String validToken;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(
            jwtUtil, "secret", SECRET);

        // Generate a valid token using JJWT
        validToken = generateTestToken(
            "john@example.com",
            "EMPLOYEE",
            1L,
            System.currentTimeMillis() + 86400000L);
    }

    // ─── Helper to generate test tokens ───────────
    private String generateTestToken(
            String email,
            String role,
            Long userId,
            long expiration) {

        return io.jsonwebtoken.Jwts.builder()
                .subject(email)
                .claim("role", role)
                .claim("userId", userId)
                .claim("fullName", "John Doe")
                .issuedAt(new java.util.Date())
                .expiration(new java.util.Date(expiration))
                .signWith(
                    io.jsonwebtoken.security.Keys
                        .hmacShaKeyFor(SECRET.getBytes()))
                .compact();
    }

    // ═══════════════════════════════════════════════
    // IS TOKEN VALID TESTS
    // ═══════════════════════════════════════════════

    @Nested
    @DisplayName("Token Validation Tests")
    class TokenValidationTests {

        @Test
        @DisplayName("Should return true for valid token")
        void isTokenValid_ValidToken() {
            assertTrue(jwtUtil.isTokenValid(validToken));
        }

        @Test
        @DisplayName("Should return false for expired token")
        void isTokenValid_ExpiredToken()
                throws InterruptedException {
            // Token expired 1ms ago
            String expiredToken = generateTestToken(
                "john@example.com",
                "EMPLOYEE",
                1L,
                System.currentTimeMillis() - 1000L);

            assertFalse(jwtUtil.isTokenValid(expiredToken));
        }

        @Test
        @DisplayName("Should return false for malformed token")
        void isTokenValid_MalformedToken() {
            assertFalse(jwtUtil.isTokenValid(
                "this.is.not.a.valid.jwt"));
        }

        @Test
        @DisplayName("Should return false for empty token")
        void isTokenValid_EmptyToken() {
            assertFalse(jwtUtil.isTokenValid(""));
        }

        @Test
        @DisplayName("Should return false for null token")
        void isTokenValid_NullToken() {
            assertFalse(jwtUtil.isTokenValid(null));
        }

        @Test
        @DisplayName("Should return false for token with wrong signature")
        void isTokenValid_WrongSignature() {
            // Token signed with different secret
            String wrongSecretToken =
                io.jsonwebtoken.Jwts.builder()
                    .subject("john@example.com")
                    .claim("role", "EMPLOYEE")
                    .expiration(new java.util.Date(
                        System.currentTimeMillis()
                        + 86400000L))
                    .signWith(
                        Keys.hmacShaKeyFor("wrongsecretwrongsecretwrongsecretwrongsecretwrong".getBytes()))
                    .compact();

            assertFalse(
                jwtUtil.isTokenValid(wrongSecretToken));
        }

        @Test
        @DisplayName("Should return false for token with only 2 parts")
        void isTokenValid_IncompleteToken() {
            assertFalse(jwtUtil.isTokenValid(
                "header.payload"));
        }
    }

    // ═══════════════════════════════════════════════
    // EXTRACT USERNAME TESTS
    // ═══════════════════════════════════════════════

    @Nested
    @DisplayName("Extract Username Tests")
    class ExtractUsernameTests {

        @Test
        @DisplayName("Should extract email as username")
        void extractUsername_Success() {
            String username =
                jwtUtil.extractUsername(validToken);

            assertEquals("john@example.com", username);
        }

        @Test
        @DisplayName("Should extract manager email")
        void extractUsername_Manager() {
            String managerToken = generateTestToken(
                "manager@example.com",
                "MANAGER",
                2L,
                System.currentTimeMillis() + 86400000L);

            assertEquals("manager@example.com",
                jwtUtil.extractUsername(managerToken));
        }

        @Test
        @DisplayName("Should throw for invalid token")
        void extractUsername_InvalidToken() {
            assertThrows(Exception.class,
                () -> jwtUtil.extractUsername(
                    "invalid.token"));
        }
    }

    // ═══════════════════════════════════════════════
    // EXTRACT ROLE TESTS
    // ═══════════════════════════════════════════════

    @Nested
    @DisplayName("Extract Role Tests")
    class ExtractRoleTests {

        @Test
        @DisplayName("Should extract EMPLOYEE role")
        void extractRole_Employee() {
            assertEquals("EMPLOYEE",
                jwtUtil.extractRole(validToken));
        }

        @Test
        @DisplayName("Should extract MANAGER role")
        void extractRole_Manager() {
            String managerToken = generateTestToken(
                "manager@example.com",
                "MANAGER",
                2L,
                System.currentTimeMillis() + 86400000L);

            assertEquals("MANAGER",
                jwtUtil.extractRole(managerToken));
        }

        @Test
        @DisplayName("Should extract ADMIN role")
        void extractRole_Admin() {
            String adminToken = generateTestToken(
                "admin@example.com",
                "ADMIN",
                3L,
                System.currentTimeMillis() + 86400000L);

            assertEquals("ADMIN",
                jwtUtil.extractRole(adminToken));
        }
    }

    // ═══════════════════════════════════════════════
    // EXTRACT USER ID TESTS
    // ═══════════════════════════════════════════════

    @Nested
    @DisplayName("Extract UserId Tests")
    class ExtractUserIdTests {

        @Test
        @DisplayName("Should extract userId correctly")
        void extractUserId_Success() {
            Long userId = jwtUtil.extractUserId(validToken);

            assertEquals(1L, userId);
        }

        @Test
        @DisplayName("Should extract different userIds")
        void extractUserId_DifferentUsers() {
            String token = generateTestToken(
                "user5@example.com",
                "EMPLOYEE",
                5L,
                System.currentTimeMillis() + 86400000L);

            assertEquals(5L,
                jwtUtil.extractUserId(token));
        }

        @Test
        @DisplayName("Should return null for token without userId")
        void extractUserId_NoUserIdClaim() {
            // Token without userId claim
            String tokenNoUserId =
                io.jsonwebtoken.Jwts.builder()
                    .subject("john@example.com")
                    .claim("role", "EMPLOYEE")
                    .expiration(new java.util.Date(
                        System.currentTimeMillis()
                        + 86400000L))
                    .signWith(
                        io.jsonwebtoken.security.Keys
                            .hmacShaKeyFor(
                                SECRET.getBytes()))
                    .compact();

            assertNull(
                jwtUtil.extractUserId(tokenNoUserId));
        }
    }
}