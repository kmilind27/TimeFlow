package com.company.leaveservice.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HeaderAuthFilter Unit Tests")
class HeaderAuthFilterTest {

    private HeaderAuthFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new HeaderAuthFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
        // Clear security context before each test
        SecurityContextHolder.clearContext();
    }

    // ═══════════════════════════════════════════════════════════
    // Authentication set correctly
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("When all required headers are present")
    class WithValidHeaders {

        @Test
        @DisplayName("Sets authentication with correct principal and role")
        void setsAuthenticationWithRole() throws Exception {
            request.addHeader("X-User-Id", "1");
            request.addHeader("X-User-Email", "john@example.com");
            request.addHeader("X-User-Role", "EMPLOYEE");

            filter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(auth, "Authentication should be set");
            assertTrue(auth.isAuthenticated());
            assertEquals("john@example.com", auth.getPrincipal());
        }

        @Test
        @DisplayName("Role is prefixed with ROLE_ in granted authority")
        void roleIsPrefixedCorrectly() throws Exception {
            request.addHeader("X-User-Id", "1");
            request.addHeader("X-User-Email", "john@example.com");
            request.addHeader("X-User-Role", "MANAGER");

            filter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertTrue(auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER")));
        }

        @Test
        @DisplayName("ADMIN role is prefixed to ROLE_ADMIN")
        void adminRoleIsPrefixed() throws Exception {
            request.addHeader("X-User-Id", "42");
            request.addHeader("X-User-Email", "admin@example.com");
            request.addHeader("X-User-Role", "ADMIN");

            filter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertTrue(auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        }

        @Test
        @DisplayName("Filter chain continues after setting authentication")
        void filterChainContinues() throws Exception {
            request.addHeader("X-User-Id", "1");
            request.addHeader("X-User-Email", "john@example.com");
            request.addHeader("X-User-Role", "EMPLOYEE");

            filter.doFilterInternal(request, response, filterChain);

            // MockFilterChain tracks whether doFilter was called
            assertNotNull(filterChain.getRequest(),
                    "Filter chain should have continued");
        }
    }

    // ═══════════════════════════════════════════════════════════
    // No authentication when headers missing
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("When required headers are missing")
    class WithMissingHeaders {

        @Test
        @DisplayName("No auth set when X-User-Id is absent")
        void noAuthWhenUserIdMissing() throws Exception {
            request.addHeader("X-User-Email", "john@example.com");
            request.addHeader("X-User-Role", "EMPLOYEE");
            // X-User-Id is missing

            filter.doFilterInternal(request, response, filterChain);

            assertNull(SecurityContextHolder.getContext().getAuthentication(),
                    "Authentication should NOT be set without X-User-Id");
        }

        @Test
        @DisplayName("No auth set when X-User-Role is absent")
        void noAuthWhenRoleMissing() throws Exception {
            request.addHeader("X-User-Id", "1");
            request.addHeader("X-User-Email", "john@example.com");
            // X-User-Role is missing

            filter.doFilterInternal(request, response, filterChain);

            assertNull(SecurityContextHolder.getContext().getAuthentication(),
                    "Authentication should NOT be set without X-User-Role");
        }

        @Test
        @DisplayName("No auth set when all headers are absent")
        void noAuthWhenNoHeaders() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("Filter chain still continues even without headers")
        void filterChainContinuesWithoutHeaders() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            assertNotNull(filterChain.getRequest(),
                    "Filter chain should continue even when no headers present");
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Email is optional
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Email header is optional")
    class EmailOptional {

        @Test
        @DisplayName("Auth still set even when X-User-Email is absent")
        void authSetWithoutEmail() throws Exception {
            request.addHeader("X-User-Id", "1");
            request.addHeader("X-User-Role", "EMPLOYEE");
            // X-User-Email intentionally absent

            filter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(auth, "Auth should be set with only Id + Role");
            // principal will be null because email header is null
            assertNull(auth.getPrincipal());
        }
    }
}