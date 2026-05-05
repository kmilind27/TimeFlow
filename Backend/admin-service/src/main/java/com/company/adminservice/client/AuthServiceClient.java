package com.company.adminservice.client;

import com.company.adminservice.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import java.util.List;

@FeignClient(
    name = "auth-service",
    fallback = AuthServiceClientFallback.class
)
public interface AuthServiceClient {

    @GetMapping("/auth/users/{id}")
    UserResponse getUserById(
            @PathVariable("id") Long id);

    @GetMapping("/auth/users")
    List<UserResponse> getAllUsers();

    @DeleteMapping("/auth/users/{id}")
    String deleteUserById(@PathVariable("id") Long id);
}