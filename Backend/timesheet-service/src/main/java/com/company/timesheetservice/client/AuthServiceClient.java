package com.company.timesheetservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.company.timesheetservice.dto.UserResponse;

@FeignClient(name = "auth-service", fallback = AuthServiceClientFallback.class)
public interface AuthServiceClient {
	
    @GetMapping("/auth/users/{id}")
    UserResponse getUserById(@PathVariable("id") Long id);

}
