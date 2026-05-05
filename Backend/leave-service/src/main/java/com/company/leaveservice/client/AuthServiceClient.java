package com.company.leaveservice.client;

import com.company.leaveservice.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "auth-service",
    fallback = AuthServiceClientFallback.class
)
public interface AuthServiceClient {

    @GetMapping("/auth/users/{id}")
    UserResponse getUserById(@PathVariable("id") Long id);
}