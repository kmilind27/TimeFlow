package com.company.timesheetservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class HeaderAuthFilter extends OncePerRequestFilter {
    // ✅ This filter runs on every request
    // Reads headers that Gateway forwarded
    // Sets SecurityContext so Spring Security
    // treats request as authenticated

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
    	
    	String path = request.getRequestURI();

        // ✅ Skip filter for Swagger URLs
        if (path.contains("/v3/api-docs")
                || path.contains("/swagger-ui")
                || path.contains("/swagger-resources")
                || path.contains("/webjars")
                || path.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        // ✅ Read headers forwarded by Gateway
        String userId = request.getHeader("X-User-Id");
        String email  = request.getHeader("X-User-Email");
        String role   = request.getHeader("X-User-Role");

        // ✅ If headers exist → set authentication
        if (userId != null && role != null) {
            // Create authority from role
            // e.g. "EMPLOYEE" → "ROLE_EMPLOYEE"
            SimpleGrantedAuthority authority =
                new SimpleGrantedAuthority("ROLE_" + role);

            // Create authentication token
            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                    email,    // principal
                    null,     // credentials
                    List.of(authority)
                );

            // ✅ Set in SecurityContext
            // Now Spring Security sees this as authenticated!
            SecurityContextHolder.getContext()
                                 .setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}