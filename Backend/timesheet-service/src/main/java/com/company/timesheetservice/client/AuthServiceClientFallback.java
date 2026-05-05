package com.company.timesheetservice.client;

import com.company.timesheetservice.dto.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class AuthServiceClientFallback
        implements AuthServiceClient {

    // ✅ If Auth Service is DOWN or slow
    // return this default response instead of crashing
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