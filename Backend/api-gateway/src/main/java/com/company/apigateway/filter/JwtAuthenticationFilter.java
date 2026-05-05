package com.company.apigateway.filter;

import com.company.apigateway.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter
        implements GlobalFilter, Ordered {
    // ✅ GlobalFilter = runs on EVERY request automatically
    // No need to configure per route in YML!
    // Ordered = we control when this filter runs
    //           lower number = runs earlier

    private final JwtUtil jwtUtil;

    @Override
    public int getOrder() {
        return -1; // ✅ Run before all other filters
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                              GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        log.debug("Gateway intercepted: {} {}",
                request.getMethod(), path);

        // ✅ STEP 1: Skip JWT for public routes
        if (isPublicRoute(path)) {
            log.debug("Public route — skipping JWT: {}", path);
            return chain.filter(exchange);
        }

        // ✅ STEP 2: Check Authorization header exists
        if (!request.getHeaders()
                    .containsHeader(HttpHeaders.AUTHORIZATION)) {
            return onError(exchange.getResponse(),
                    "Authorization header is missing",
                    HttpStatus.UNAUTHORIZED);
        }

        // ✅ STEP 3: Extract token
        String authHeader = request.getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null
                || !authHeader.startsWith("Bearer ")) {
            return onError(exchange.getResponse(),
                    "Invalid Authorization header format",
                    HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        // ✅ STEP 4: Validate token
        if (!jwtUtil.isTokenValid(token)) {
            return onError(exchange.getResponse(),
                    "JWT token is invalid or expired",
                    HttpStatus.UNAUTHORIZED);
        }

        // ✅ STEP 5: Extract user info from token
        String email  = jwtUtil.extractUsername(token);
        String role   = jwtUtil.extractRole(token);
        Long   userId = jwtUtil.extractUserId(token);

        log.debug("JWT valid — user: {} role: {}",
                email, role);

        // ✅ STEP 6: Add user info as headers for downstream
        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Email", email)
                .header("X-User-Role",  role)
                .header("X-User-Id",
                        userId != null
                        ? userId.toString() : "")
                .build();

        return chain.filter(
                exchange.mutate()
                        .request(modifiedRequest)
                        .build());
    }

    // ─── Public Routes ─────────────────────────────────

    private boolean isPublicRoute(String path) {
        return path.startsWith("/auth/signup")
            || path.startsWith("/auth/login")
            || path.startsWith("/auth/forgot-password")
            || path.startsWith("/auth/verify-otp")
            || path.startsWith("/auth/reset-password")
            || path.startsWith("/actuator")
            || path.contains("/v3/api-docs")
            || path.contains("/swagger-ui")
            || path.contains("/swagger-resources")
            || path.contains("/webjars");
    }

    // ─── Error Helper ──────────────────────────────────

    private Mono<Void> onError(ServerHttpResponse response,
                                String message,
                                HttpStatus status) {
        response.setStatusCode(status);
        response.getHeaders()
                .setContentType(MediaType.APPLICATION_JSON);

        String body = String.format(
                "{\"error\":\"%s\",\"message\":\"%s\"}",
                status.getReasonPhrase(), message);

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        return response.writeWith(
                Mono.just(response.bufferFactory()
                        .wrap(bytes)));
    }
}