package com.company.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {

        CorsConfiguration config = new CorsConfiguration();

        // ✅ Allowed origins — frontend URLs
        // During development: localhost:3000 (React default)
        // During production: your actual domain
        config.setAllowedOriginPatterns(List.of("*"));
        /*
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:8080",
                // "http://localhost:4200",    // Angular dev
                "http://localhost:5173"     // Vite dev
        ));
        */

        // ✅ Allowed HTTP methods
        config.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "PATCH",
                "OPTIONS"
        ));

        // ✅ Allowed headers
        // Authorization → JWT token
        // Content-Type  → application/json
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With",
                "Origin"
        ));

        // ✅ Allow credentials (cookies, auth headers)
        config.setAllowCredentials(true);

        // ✅ How long browser caches preflight response
        config.setMaxAge(3600L);

        // ✅ Apply to ALL routes
        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}