package com.refscheduler.dto.auth;

import com.refscheduler.dto.user.UserResponse;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    UserResponse user
) {
}
