package com.company.timesheetservice.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
        SecurityContextHolder.clearContext();
    }

    // ═══════════════════════════════════════════════════════════
    // Swagger / Actuator skip logic (NEW vs leave-service filter)
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Swagger and Actuator paths are skipped (no auth set)")
    class SkippedPathsTests {

        @Test
        @DisplayName("Skips /v3/api-docs path — no auth set, chain continues")
        void skipsV3ApiDocs() throws Exception {
            request.setRequestURI("/v3/api-docs");
            request.addHeader("X-User-Id", "1");
            request.addHeader("X-User-Role", "EMPLOYEE");

            filter.doFilterInternal(request, response, filterChain);

            // Auth should NOT be set — filter returned early
            assertNull(SecurityContextHolder.getContext().getAuthentication());
            assertNotNull(filterChain.getRequest());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "/v3/api-docs/swagger-config",
                "/swagger-ui/index.html",
                "/swagger-resources/configuration/ui",
                "/webjars/springfox-swagger-ui/swagger-ui.css",
                "/actuator/health"
        })
        @DisplayName("Should skip filter for public Swagger & actuator paths")
        void shouldSkipFilterForPublicPaths(String uri) throws Exception {

            request.setRequestURI(uri);

            filter.doFilterInternal(request, response, filterChain);

            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Authentication set correctly for normal paths
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Normal request paths — authentication is set")
    class NormalPathTests {

        @Test
        @DisplayName("Sets auth with correct principal (email) and ROLE_ prefix")
        void setsAuthWithRolePrefix() throws Exception {
            request.setRequestURI("/timesheet/entries");
            request.addHeader("X-User-Id", "1");
            request.addHeader("X-User-Email", "john@example.com");
            request.addHeader("X-User-Role", "EMPLOYEE");

            filter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(auth);
            assertTrue(auth.isAuthenticated());
            assertEquals("john@example.com", auth.getPrincipal());
            assertTrue(auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE")));
        }

        @Test
        @DisplayName("MANAGER role is prefixed to ROLE_MANAGER")
        void managerRolePrefixed() throws Exception {
            request.setRequestURI("/timesheet/manager/pending");
            request.addHeader("X-User-Id", "2");
            request.addHeader("X-User-Email", "mgr@example.com");
            request.addHeader("X-User-Role", "MANAGER");

            filter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertTrue(auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER")));
        }

        @Test
        @DisplayName("ADMIN role is prefixed to ROLE_ADMIN")
        void adminRolePrefixed() throws Exception {
            request.setRequestURI("/timesheet/manager/review/1");
            request.addHeader("X-User-Id", "3");
            request.addHeader("X-User-Email", "admin@example.com");
            request.addHeader("X-User-Role", "ADMIN");

            filter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertTrue(auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        }

        @Test
        @DisplayName("Filter chain continues after auth is set")
        void filterChainContinues() throws Exception {
            request.setRequestURI("/timesheet/projects");
            request.addHeader("X-User-Id", "1");
            request.addHeader("X-User-Role", "EMPLOYEE");

            filter.doFilterInternal(request, response, filterChain);

            assertNotNull(filterChain.getRequest());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Missing headers — no auth set
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Missing headers — authentication NOT set")
    class MissingHeaderTests {

        @Test
        @DisplayName("No auth when X-User-Id missing")
        void noAuthWhenUserIdMissing() throws Exception {
            request.setRequestURI("/timesheet/projects");
            request.addHeader("X-User-Role", "EMPLOYEE");

            filter.doFilterInternal(request, response, filterChain);

            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("No auth when X-User-Role missing")
        void noAuthWhenRoleMissing() throws Exception {
            request.setRequestURI("/timesheet/projects");
            request.addHeader("X-User-Id", "1");

            filter.doFilterInternal(request, response, filterChain);

            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("No auth when both headers missing")
        void noAuthWhenAllMissing() throws Exception {
            request.setRequestURI("/timesheet/projects");

            filter.doFilterInternal(request, response, filterChain);

            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("Filter chain still continues even without headers")
        void chainContinuesWithoutHeaders() throws Exception {
            request.setRequestURI("/timesheet/projects");

            filter.doFilterInternal(request, response, filterChain);

            assertNotNull(filterChain.getRequest());
        }
    }
}