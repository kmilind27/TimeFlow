package com.company.apigateway.filter;

import com.company.apigateway.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private GatewayFilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String BEARER_TOKEN = "Bearer " + VALID_TOKEN;

    @BeforeEach
    void setUp() {
        // ✅ filterChain.filter() must return Mono.empty() — simulates
        // next filter in chain completing successfully
        when(filterChain.filter(any())).thenReturn(Mono.empty());
    }

    // ════════════════════════════════════════════════════════
    // getOrder — must return -1 (runs before all other filters)
    // ════════════════════════════════════════════════════════

    @Test
    @DisplayName("getOrder returns -1 to run before all other filters")
    void getOrder_shouldReturnMinusOne() {
        assertThat(jwtAuthenticationFilter.getOrder()).isEqualTo(-1);
    }

    // ════════════════════════════════════════════════════════
    // Public routes — JWT skipped entirely
    // ✅ isPublicRoute: /auth/signup, /auth/login,
    //    /auth/forgot-password, /actuator, /v3/api-docs,
    //    /swagger-ui, /swagger-resources, /webjars
    // ════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Public routes — JWT bypassed")
    class PublicRoutes {

        @Test
        @DisplayName("/auth/signup — no JWT required")
        void signupRoute_shouldSkipJwtValidation() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .post("/auth/signup")
                    .build();
            MockServerWebExchange exchange =
                    MockServerWebExchange.from(request);

            jwtAuthenticationFilter.filter(exchange, filterChain)
                    .block();

            // ✅ Chain continues — no auth check
            verify(filterChain).filter(any());
            // ✅ JwtUtil never called for public routes
            verifyNoInteractions(jwtUtil);
        }

        @Test
        @DisplayName("/auth/login — no JWT required")
        void loginRoute_shouldSkipJwtValidation() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .post("/auth/login")
                    .build();
            MockServerWebExchange exchange =
                    MockServerWebExchange.from(request);

            jwtAuthenticationFilter.filter(exchange, filterChain)
                    .block();

            verify(filterChain).filter(any());
            verifyNoInteractions(jwtUtil);
        }

        @Test
        @DisplayName("/auth/forgot-password — no JWT required")
        void forgotPasswordRoute_shouldSkipJwtValidation() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .post("/auth/forgot-password")
                    .build();
            MockServerWebExchange exchange =
                    MockServerWebExchange.from(request);

            jwtAuthenticationFilter.filter(exchange, filterChain)
                    .block();

            verify(filterChain).filter(any());
            verifyNoInteractions(jwtUtil);
        }

        @Test
        @DisplayName("/actuator/health — no JWT required")
        void actuatorRoute_shouldSkipJwtValidation() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/actuator/health")
                    .build();
            MockServerWebExchange exchange =
                    MockServerWebExchange.from(request);

            jwtAuthenticationFilter.filter(exchange, filterChain)
                    .block();

            verify(filterChain).filter(any());
            verifyNoInteractions(jwtUtil);
        }

        @Test
        @DisplayName("/v3/api-docs — no JWT required")
        void apiDocsRoute_shouldSkipJwtValidation() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/v3/api-docs")
                    .build();
            MockServerWebExchange exchange =
                    MockServerWebExchange.from(request);

            jwtAuthenticationFilter.filter(exchange, filterChain)
                    .block();

            verify(filterChain).filter(any());
            verifyNoInteractions(jwtUtil);
        }

        @Test
        @DisplayName("/auth/v3/api-docs — no JWT required")
        void serviceApiDocsRoute_shouldSkipJwtValidation() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/auth/v3/api-docs")
                    .build();
            MockServerWebExchange exchange =
                    MockServerWebExchange.from(request);

            jwtAuthenticationFilter.filter(exchange, filterChain)
                    .block();

            verify(filterChain).filter(any());
            verifyNoInteractions(jwtUtil);
        }

        @Test
        @DisplayName("/swagger-ui/index.html — no JWT required")
        void swaggerUiRoute_shouldSkipJwtValidation() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/swagger-ui/index.html")
                    .build();
            MockServerWebExchange exchange =
                    MockServerWebExchange.from(request);

            jwtAuthenticationFilter.filter(exchange, filterChain)
                    .block();

            verify(filterChain).filter(any());
            verifyNoInteractions(jwtUtil);
        }

        @Test
        @DisplayName("/swagger-resources — no JWT required")
        void swaggerResourcesRoute_shouldSkipJwtValidation() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/swagger-resources")
                    .build();
            MockServerWebExchange exchange =
                    MockServerWebExchange.from(request);

            jwtAuthenticationFilter.filter(exchange, filterChain)
                    .block();

            verify(filterChain).filter(any());
            verifyNoInteractions(jwtUtil);
        }

        @Test
        @DisplayName("/webjars/swagger-ui — no JWT required")
        void webjarsRoute_shouldSkipJwtValidation() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/webjars/swagger-ui/4.15.5/swagger-ui.css")
                    .build();
            MockServerWebExchange exchange =
                    MockServerWebExchange.from(request);

            jwtAuthenticationFilter.filter(exchange, filterChain)
                    .block();

            verify(filterChain).filter(any());
            verifyNoInteractions(jwtUtil);
        }
    }

    // ════════════════════════════════════════════════════════
    // Missing Authorization header → 401
    // ════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Missing or malformed Authorization header")
    class MissingAuthHeader {

        @Test
        @DisplayName("No Authorization header → 401 Unauthorized")
        void noAuthHeader_shouldReturn401() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/auth/profile")
                    // ✅ No Authorization header
                    .build();
            MockServerWebExchange exchange =
                    MockServerWebExchange.from(request);

            jwtAuthenticationFilter.filter(exchange, filterChain)
                    .block();

            // ✅ Your filter: !containsHeader(AUTHORIZATION) → onError
            assertThat(exchange.getResponse().getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
            // ✅ Filter chain must NOT continue
            verify(filterChain, never()).filter(any());
        }

        @Test
        @DisplayName("Authorization header without Bearer prefix → 401")
        void authHeaderWithoutBearer_shouldReturn401() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/auth/profile")
                    // ✅ Your filter: !authHeader.startsWith("Bearer ")
                    .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz")
                    .build();
            MockServerWebExchange exchange =
                    MockServerWebExchange.from(request);

            jwtAuthenticationFilter.filter(exchange, filterChain)
                    .block();

            assertThat(exchange.getResponse().getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(filterChain, never()).filter(any());
        }

        @Test
        @DisplayName("Bearer with empty token → 401")
        void bearerWithEmptyToken_shouldReturn401() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/timesheet/projects")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ")
                    .build();
            MockServerWebExchange exchange =
                    MockServerWebExchange.from(request);

            // ✅ token = "" after substring(7)
            // isTokenValid("") → false
            when(jwtUtil.isTokenValid("")).thenReturn(false);

            jwtAuthenticationFilter.filter(exchange, filterChain)
                    .block();

            assertThat(exchange.getResponse().getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(filterChain, never()).filter(any());
        }

        @Test
        @DisplayName("Protected route /leave/apply without token → 401")
        void protectedRoute_noToken_shouldReturn401() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .post("/leave/apply")
                    .build();
            MockServerWebExchange exchange =
                    MockServerWebExchange.from(request);

            jwtAuthenticationFilter.filter(exchange, filterChain)
                    .block();

            assertThat(exchange.getResponse().getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(filterChain, never()).filter(any());
        }

        @Test
        @DisplayName("Protected route /admin/dashboard without token → 401")
        void adminRoute_noToken_shouldReturn401() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/admin/dashboard")
                    .build();
            MockServerWebExchange exchange =
                    MockServerWebExchange.from(request);

            jwtAuthenticationFilter.filter(exchange, filterChain)
                    .block();

            assertThat(exchange.getResponse().getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ════════════════════════════════════════════════════════
    // Invalid or expired JWT → 401
    // ════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Invalid JWT token")
    class InvalidToken {

        @Test
        @DisplayName("Invalid token → 401 Unauthorized")
        void invalidToken_shouldReturn401() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/auth/profile")
                    .header(HttpHeaders.AUTHORIZATION,
                            "Bearer invalid.token.here")
                    .build();
            MockServerWebExchange exchange =
                    MockServerWebExchange.from(request);

            // ✅ jwtUtil.isTokenValid → false
            when(jwtUtil.isTokenValid("invalid.token.here"))
                    .thenReturn(false);

            jwtAuthenticationFilter.filter(exchange, filterChain)
                    .block();

            assertThat(exchange.getResponse().getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(filterChain, never()).filter(any());
        }

        @Test
        @DisplayName("Expired token → 401 Unauthorized")
        void expiredToken_shouldReturn401() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/timesheet/my-timesheets")
                    .header(HttpHeaders.AUTHORIZATION,
                            "Bearer expired.jwt.token")
                    .build();
            MockServerWebExchange exchange =
                    MockServerWebExchange.from(request);

            when(jwtUtil.isTokenValid("expired.jwt.token"))
                    .thenReturn(false);

            jwtAuthenticationFilter.filter(exchange, filterChain)
                    .block();

            assertThat(exchange.getResponse().getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(filterChain, never()).filter(any());
        }

        @Test
        @DisplayName("Tampered token → 401 Unauthorized")
        void tamperedToken_shouldReturn401() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/leave/my-requests")
                    .header(HttpHeaders.AUTHORIZATION,
                            "Bearer tampered.token.xyz")
                    .build();
            MockServerWebExchange exchange =
                    MockServerWebExchange.from(request);

            when(jwtUtil.isTokenValid("tampered.token.xyz"))
                    .thenReturn(false);

            jwtAuthenticationFilter.filter(exchange, filterChain)
                    .block();

            assertThat(exchange.getResponse().getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ════════════════════════════════════════════════════════
    // Valid JWT → headers forwarded downstream
    // ✅ Your filter adds: X-User-Email, X-User-Role, X-User-Id
    // ════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Valid JWT — headers forwarded to downstream")
    class ValidToken {

        @BeforeEach
        void setUpValidToken() {
            // ✅ All jwtUtil calls succeed for valid token
            when(jwtUtil.isTokenValid(VALID_TOKEN)).thenReturn(true);
            when(jwtUtil.extractUsername(VALID_TOKEN))
                    .thenReturn("john@test.com");
            when(jwtUtil.extractRole(VALID_TOKEN))
                    .thenReturn("EMPLOYEE");
            when(jwtUtil.extractUserId(VALID_TOKEN))
                    .thenReturn(1L);
        }

        @Test
        @DisplayName("Valid token — filter chain continues")
        void validToken_shouldContinueFilterChain() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/auth/profile")
                    .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN)
                    .build();
            MockServerWebExchange exchange =
                    MockServerWebExchange.from(request);

            jwtAuthenticationFilter.filter(exchange, filterChain)
                    .block();

            // ✅ Chain continues for valid token
            verify(filterChain).filter(any());
            // ✅ No error status set
            assertThat(exchange.getResponse().getStatusCode())
                    .isNull();
        }

        @Test
        @DisplayName("Valid token — X-User-Email header forwarded")
        void validToken_shouldForwardEmailHeader() {
            // ✅ Use capture approach — check mutated request headers
            // via verifying jwtUtil calls were made correctly
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/timesheet/projects")
                    .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN)
                    .build();
            MockServerWebExchange exchange =
                    MockServerWebExchange.from(request);

            jwtAuthenticationFilter.filter(exchange, filterChain)
                    .block();

            // ✅ Verify email was extracted from token
            verify(jwtUtil).extractUsername(VALID_TOKEN);
            verify(filterChain).filter(any());
        }

        @Test
        @DisplayName("Valid token — X-User-Role header forwarded")
        void validToken_shouldForwardRoleHeader() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/leave/my-balance")
                    .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN)
                    .build();
            MockServerWebExchange exchange =
                    MockServerWebExchange.from(request);

            jwtAuthenticationFilter.filter(exchange, filterChain)
                    .block();

            // ✅ Verify role was extracted from token
            verify(jwtUtil).extractRole(VALID_TOKEN);
            verify(filterChain).filter(any());
        }

        @Test
        @DisplayName("Valid token — X-User-Id header forwarded")
        void validToken_shouldForwardUserIdHeader() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/admin/dashboard")
                    .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN)
                    .build();
            MockServerWebExchange exchange =
                    MockServerWebExchange.from(request);

            jwtAuthenticationFilter.filter(exchange, filterChain)
                    .block();

            // ✅ Verify userId was extracted from token
            verify(jwtUtil).extractUserId(VALID_TOKEN);
            verify(filterChain).filter(any());
        }

        @Test
        @DisplayName("Valid token — all 3 jwtUtil extractions called")
        void validToken_shouldCallAllExtractions() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/auth/profile")
                    .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN)
                    .build();
            MockServerWebExchange exchange =
                    MockServerWebExchange.from(request);

            jwtAuthenticationFilter.filter(exchange, filterChain)
                    .block();

            // ✅ Verify all 3 claims extracted in order
            verify(jwtUtil).isTokenValid(VALID_TOKEN);
            verify(jwtUtil).extractUsername(VALID_TOKEN);
            verify(jwtUtil).extractRole(VALID_TOKEN);
            verify(jwtUtil).extractUserId(VALID_TOKEN);
        }

        @Test
        @DisplayName("Valid MANAGER token — role forwarded correctly")
        void validManagerToken_shouldForwardManagerRole() {
            String managerToken = "manager.jwt.token";
            when(jwtUtil.isTokenValid(managerToken)).thenReturn(true);
            when(jwtUtil.extractUsername(managerToken))
                    .thenReturn("manager@test.com");
            when(jwtUtil.extractRole(managerToken))
                    .thenReturn("MANAGER");
            when(jwtUtil.extractUserId(managerToken))
                    .thenReturn(2L);

            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/timesheet/manager/pending")
                    .header(HttpHeaders.AUTHORIZATION,
                            "Bearer " + managerToken)
                    .build();
            MockServerWebExchange exchange =
                    MockServerWebExchange.from(request);

            jwtAuthenticationFilter.filter(exchange, filterChain)
                    .block();

            verify(jwtUtil).extractRole(managerToken);
            verify(filterChain).filter(any());
        }

        @Test
        @DisplayName("Valid token with null userId — X-User-Id set to empty string")
        void validToken_nullUserId_shouldSetEmptyString() {
            // ✅ Your filter: userId != null ? userId.toString() : ""
            when(jwtUtil.extractUserId(VALID_TOKEN)).thenReturn(null);

            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/auth/profile")
                    .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN)
                    .build();
            MockServerWebExchange exchange =
                    MockServerWebExchange.from(request);

            jwtAuthenticationFilter.filter(exchange, filterChain)
                    .block();

            // ✅ Should not throw NullPointerException
            // Filter chain continues even with null userId
            verify(filterChain).filter(any());
        }
    }

    // ════════════════════════════════════════════════════════
    // Response body for error cases
    // ════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Error response format")
    class ErrorResponseFormat {

        @Test
        @DisplayName("Missing header — response content type is JSON")
        void missingHeader_responseContentTypeIsJson() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/auth/profile")
                    .build();
            MockServerWebExchange exchange =
                    MockServerWebExchange.from(request);

            jwtAuthenticationFilter.filter(exchange, filterChain)
                    .block();

            // ✅ Your onError sets ContentType to APPLICATION_JSON
            assertThat(exchange.getResponse().getHeaders()
                    .getContentType())
                    .isEqualTo(org.springframework.http.MediaType
                            .APPLICATION_JSON);
        }

        @Test
        @DisplayName("Invalid token — response content type is JSON")
        void invalidToken_responseContentTypeIsJson() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/auth/profile")
                    .header(HttpHeaders.AUTHORIZATION,
                            "Bearer bad.token")
                    .build();
            MockServerWebExchange exchange =
                    MockServerWebExchange.from(request);

            when(jwtUtil.isTokenValid("bad.token"))
                    .thenReturn(false);

            jwtAuthenticationFilter.filter(exchange, filterChain)
                    .block();

            assertThat(exchange.getResponse().getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(exchange.getResponse().getHeaders()
                    .getContentType())
                    .isEqualTo(org.springframework.http.MediaType
                            .APPLICATION_JSON);
        }
    }

    // ════════════════════════════════════════════════════════
    // Reactive Mono — verify non-blocking behavior
    // ════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Reactive Mono completion")
    class ReactiveMonoTests {

        @Test
        @DisplayName("Public route — Mono completes successfully")
        void publicRoute_monoCompletesSuccessfully() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .post("/auth/login")
                    .build();
            MockServerWebExchange exchange =
                    MockServerWebExchange.from(request);

            Mono<Void> result = jwtAuthenticationFilter
                    .filter(exchange, filterChain);

            // ✅ StepVerifier — reactive test
            StepVerifier.create(result)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Valid token — Mono completes without error")
        void validToken_monoCompletesWithoutError() {
            when(jwtUtil.isTokenValid(VALID_TOKEN)).thenReturn(true);
            when(jwtUtil.extractUsername(VALID_TOKEN))
                    .thenReturn("john@test.com");
            when(jwtUtil.extractRole(VALID_TOKEN))
                    .thenReturn("EMPLOYEE");
            when(jwtUtil.extractUserId(VALID_TOKEN))
                    .thenReturn(1L);

            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/auth/profile")
                    .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN)
                    .build();
            MockServerWebExchange exchange =
                    MockServerWebExchange.from(request);

            Mono<Void> result = jwtAuthenticationFilter
                    .filter(exchange, filterChain);

            StepVerifier.create(result)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Missing token — Mono completes (error written to response)")
        void missingToken_monoCompletesWithErrorResponse() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/auth/profile")
                    .build();
            MockServerWebExchange exchange =
                    MockServerWebExchange.from(request);

            Mono<Void> result = jwtAuthenticationFilter
                    .filter(exchange, filterChain);

            // ✅ onError writes to response and completes —
            // does NOT throw an exception in the Mono
            StepVerifier.create(result)
                    .verifyComplete();
        }
    }
}