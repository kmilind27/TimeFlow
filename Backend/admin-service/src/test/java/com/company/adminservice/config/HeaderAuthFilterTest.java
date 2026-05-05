package com.company.adminservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeaderAuthFilterTest {

    @InjectMocks
    private HeaderAuthFilter headerAuthFilter;

    @Mock
    private FilterChain filterChain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        // Clear SecurityContext before each test
        SecurityContextHolder.clearContext();
    }

    // ════════════════════════════════════════════════════════
    // Valid headers — SecurityContext should be set
    // ════════════════════════════════════════════════════════

    @Test
    void doFilterInternal_whenAllHeadersPresent_shouldSetAuthentication()
            throws ServletException, IOException {

        request.addHeader("X-User-Id", "1");
        request.addHeader("X-User-Email", "john@test.com");
        request.addHeader("X-User-Role", "ADMIN");

        headerAuthFilter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo("john@test.com");
        assertThat(auth.isAuthenticated()).isTrue();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_whenAllHeadersPresent_shouldSetCorrectRole()
            throws ServletException, IOException {

        request.addHeader("X-User-Id", "1");
        request.addHeader("X-User-Email", "manager@test.com");
        request.addHeader("X-User-Role", "MANAGER");

        headerAuthFilter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities())
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
    }

    @Test
    void doFilterInternal_whenEmployeeRole_shouldSetRoleEmployee()
            throws ServletException, IOException {

        request.addHeader("X-User-Id", "3");
        request.addHeader("X-User-Email", "emp@test.com");
        request.addHeader("X-User-Role", "EMPLOYEE");

        headerAuthFilter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getAuthorities())
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));
    }

    // ════════════════════════════════════════════════════════
    // Missing headers — SecurityContext should NOT be set
    // ════════════════════════════════════════════════════════

    @Test
    void doFilterInternal_whenNoHeaders_shouldNotSetAuthentication()
            throws ServletException, IOException {

        // No headers added
        headerAuthFilter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
        // Filter chain must still continue
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_whenOnlyUserIdMissing_shouldNotSetAuthentication()
            throws ServletException, IOException {

        // Missing X-User-Id
        request.addHeader("X-User-Email", "john@test.com");
        request.addHeader("X-User-Role", "ADMIN");

        headerAuthFilter.doFilterInternal(request, response, filterChain);

        // Your filter checks: userId != null && role != null
        // Missing userId → no auth set
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_whenOnlyRoleMissing_shouldNotSetAuthentication()
            throws ServletException, IOException {

        // Missing X-User-Role
        request.addHeader("X-User-Id", "1");
        request.addHeader("X-User-Email", "john@test.com");

        headerAuthFilter.doFilterInternal(request, response, filterChain);

        // Your filter checks: userId != null && role != null
        // Missing role → no auth set
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
        verify(filterChain).doFilter(request, response);
    }

    // ════════════════════════════════════════════════════════
    // Filter chain always continues
    // ════════════════════════════════════════════════════════

    @Test
    void doFilterInternal_withValidHeaders_shouldAlwaysCallFilterChain()
            throws ServletException, IOException {

        request.addHeader("X-User-Id", "1");
        request.addHeader("X-User-Email", "john@test.com");
        request.addHeader("X-User-Role", "ADMIN");

        headerAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withNoHeaders_shouldAlwaysCallFilterChain()
            throws ServletException, IOException {

        headerAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }
}