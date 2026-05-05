package com.company.adminservice.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    /**
     * Feign Interceptor to forward the Authorization header from the 
     * current incoming request to the outgoing Feign request.
     */
	@Bean
	public RequestInterceptor requestInterceptor() {
	    return template -> {
	        ServletRequestAttributes attributes =
	            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

	        if (attributes != null) {
	            HttpServletRequest request = attributes.getRequest();
	            String authHeader = request.getHeader("Authorization");

	            if (authHeader != null && !authHeader.isEmpty()) {
	                template.header("Authorization", authHeader);
	            }
	        }
	    };
	}
    
}
