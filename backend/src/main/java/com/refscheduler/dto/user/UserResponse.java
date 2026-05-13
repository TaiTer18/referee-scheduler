package com.refscheduler.dto.user;

import com.refscheduler.model.User;

import java.time.LocalDateTime;

public record UserResponse(
    Integer id,
    String email,
    String fullName,
    String phoneNumber,
    String role,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getPhoneNumber(),
            user.getRole(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}
