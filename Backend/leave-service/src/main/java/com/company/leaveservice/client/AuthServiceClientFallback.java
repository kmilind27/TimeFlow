package com.company.leaveservice.client;

import com.company.leaveservice.dto.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class AuthServiceClientFallback
        implements AuthServiceClient {

    @Override
    public UserResponse getUserById(Long id) {
        return UserResponse.builder()
                .id(id)
                .fullName("Unknown User")
                .email("unknown@example.com")
                .role("UNKNOWN")
                .build();
    }
}