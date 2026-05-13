package com.refscheduler.dto.admin;

import com.refscheduler.dto.user.UserResponse;

import java.time.LocalDateTime;

public record GameRefereeAvailabilityResponse(
    UserResponse referee,
    Boolean isAvailable,
    LocalDateTime updatedAt
) {
}
