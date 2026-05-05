package com.company.authservice.security;

import com.company.authservice.model.User;
import com.company.authservice.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

// ════════════════════════════════════════════════════════════════
// JwtAuthFilterTest
// ════════════════════════════════════════════════════════════════
@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private User mockUser;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();

        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("john@test.com");
        mockUser.setRole("EMPLOYEE");
        mockUser.setPassword("encodedPassword");
        mockUser.setStatus("ACTIVE");
    }

    // ════════════════════════════════════════════════════════
    // Swagger / API docs paths → skip filter
    // ✅ Your filter: path.contains("/v3/api-docs") || path.contains("/swagger")
    // ════════════════════════════════════════════════════════

    @ParameterizedTest
    @ValueSource(strings = {
            "/swagger-ui/index.html",
            "/v3/api-docs",
            "/auth/v3/api-docs/swagger-config"
    })
    void doFilterInternal_swaggerPaths_shouldSkipFilter(String uri)
            throws ServletException, IOException {

        request.setRequestURI(uri);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
    }

    // ════════════════════════════════════════════════════════
    // No Authorization header → skip
    // ════════════════════════════════════════════════════════

    @Test
    void doFilterInternal_noAuthHeader_shouldSkipFilter()
            throws ServletException, IOException {
        request.setRequestURI("/auth/profile");
        // No Authorization header

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_authHeaderWithoutBearerPrefix_shouldSkipFilter()
            throws ServletException, IOException {
        request.setRequestURI("/auth/profile");
        // ✅ Your filter checks: authHeader.startsWith("Bearer ")
        request.addHeader("Authorization", "Basic somebase64token");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void doFilterInternal_emptyAuthHeader_shouldSkipFilter()
            throws ServletException, IOException {
        request.setRequestURI("/auth/profile");
        request.addHeader("Authorization", "");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
    }

    // ════════════════════════════════════════════════════════
    // Malformed token → extractUsername throws → skip
    // ════════════════════════════════════════════════════════

    @Test
    void doFilterInternal_malformedToken_shouldSkipAndContinueChain()
            throws ServletException, IOException {
        request.setRequestURI("/auth/profile");
        request.addHeader("Authorization", "Bearer malformed.token.here");

        // ✅ Your filter catches any exception from extractUsername
        when(jwtService.extractUsername("malformed.token.here"))
                .thenThrow(new RuntimeException("Malformed JWT"));

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ════════════════════════════════════════════════════════
    // Valid token → sets SecurityContext
    // ════════════════════════════════════════════════════════

    @Test
    void doFilterInternal_validToken_shouldSetAuthentication()
            throws ServletException, IOException {
        request.setRequestURI("/auth/profile");
        request.addHeader("Authorization", "Bearer valid.jwt.token");

        when(jwtService.extractUsername("valid.jwt.token"))
                .thenReturn("john@test.com");
        when(userDetailsService.loadUserByUsername("john@test.com"))
                .thenReturn(mockUser);
        when(jwtService.isTokenValid("valid.jwt.token", mockUser))
                .thenReturn(true);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // ✅ SecurityContext must be set
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNotNull();
        assertThat(SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal())
                .isEqualTo(mockUser);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_validToken_shouldSetCorrectPrincipal()
            throws ServletException, IOException {
        request.setRequestURI("/auth/users");
        request.addHeader("Authorization", "Bearer valid.jwt.token");

        when(jwtService.extractUsername("valid.jwt.token"))
                .thenReturn("john@test.com");
        when(userDetailsService.loadUserByUsername("john@test.com"))
                .thenReturn(mockUser);
        when(jwtService.isTokenValid("valid.jwt.token", mockUser))
                .thenReturn(true);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getPrincipal()).isEqualTo(mockUser);
        assertThat(auth.getCredentials()).isNull();
    }

    // ════════════════════════════════════════════════════════
    // Invalid token → isTokenValid false → no auth set
    // ════════════════════════════════════════════════════════

    @Test
    void doFilterInternal_invalidToken_shouldNotSetAuthentication()
            throws ServletException, IOException {
        request.setRequestURI("/auth/profile");
        request.addHeader("Authorization", "Bearer invalid.jwt.token");

        when(jwtService.extractUsername("invalid.jwt.token"))
                .thenReturn("john@test.com");
        when(userDetailsService.loadUserByUsername("john@test.com"))
                .thenReturn(mockUser);
        // ✅ Token fails validation
        when(jwtService.isTokenValid("invalid.jwt.token", mockUser))
                .thenReturn(false);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    // ════════════════════════════════════════════════════════
    // Already authenticated → skip loading user again
    // ════════════════════════════════════════════════════════

    @Test
    void doFilterInternal_alreadyAuthenticated_shouldNotReAuthenticate()
            throws ServletException, IOException {
        request.setRequestURI("/auth/profile");
        request.addHeader("Authorization", "Bearer valid.jwt.token");

        // ✅ Pre-set authentication in SecurityContext
        var existingAuth = new org.springframework.security.authentication
                .UsernamePasswordAuthenticationToken(
                mockUser, null, mockUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(jwtService.extractUsername("valid.jwt.token"))
                .thenReturn("john@test.com");
        // ✅ Your filter: if userEmail != null && authentication == null
        // Since authentication is already set → skip loading user

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // ✅ userDetailsService should NOT be called again
        verifyNoInteractions(userDetailsService);
        verify(filterChain).doFilter(request, response);
    }

    // ════════════════════════════════════════════════════════
    // Filter chain always continues regardless of outcome
    // ════════════════════════════════════════════════════════

    @Test
    void doFilterInternal_always_shouldCallFilterChain()
            throws ServletException, IOException {
        request.setRequestURI("/auth/login");
        // No header → should still call chain

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }
}


// ════════════════════════════════════════════════════════════════
// CustomUserDetailsServiceTest — separate class in same file
// ════════════════════════════════════════════════════════════════
// Note: Move to its own file: CustomUserDetailsServiceTest.java

class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        org.mockito.MockitoAnnotations.openMocks(this);

        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("john@test.com");
        mockUser.setPassword("encodedPassword");
        mockUser.setRole("EMPLOYEE");
        mockUser.setStatus("ACTIVE");
    }

    // ════════════════════════════════════════════════════════
    // loadUserByUsername — success
    // ════════════════════════════════════════════════════════

    @Test
    void loadUserByUsername_whenUserExists_shouldReturnUserDetails() {
        when(userRepository.findByEmail("john@test.com"))
                .thenReturn(Optional.of(mockUser));

        var userDetails =
                customUserDetailsService.loadUserByUsername("john@test.com");

        assertThat(userDetails).isNotNull();
        // ✅ User entity implements UserDetails — getUsername() returns email
        assertThat(userDetails.getUsername()).isEqualTo("john@test.com");
        verify(userRepository).findByEmail("john@test.com");
    }

    @Test
    void loadUserByUsername_whenUserNotFound_shouldThrowUsernameNotFoundException() {
        when(userRepository.findByEmail("notfound@test.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                customUserDetailsService.loadUserByUsername("notfound@test.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                // ✅ Your message: "User not found with email: " + username
                .hasMessageContaining("User not found with email: notfound@test.com");
    }

    @Test
    void loadUserByUsername_shouldCallRepositoryWithCorrectEmail() {
        when(userRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(mockUser));

        customUserDetailsService.loadUserByUsername("admin@test.com");

        verify(userRepository).findByEmail("admin@test.com");
        verify(userRepository, never()).findByEmail("john@test.com");
    }
}