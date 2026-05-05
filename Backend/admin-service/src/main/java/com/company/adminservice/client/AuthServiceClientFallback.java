package com.company.adminservice.client;

import com.company.adminservice.dto.UserResponse;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class AuthServiceClientFallback
        implements AuthServiceClient {

    @Override
    public UserResponse getUserById(Long id) {
        return UserResponse.builder()
                .id(id)
                .fullName("Unknown User")
                .build();
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return List.of();
    }

    @Override
    public String deleteUserById(Long id) {
        return "Fallback: Unable to delete user with id: " + id;
    }
}