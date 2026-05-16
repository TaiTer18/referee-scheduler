package com.refscheduler.dto.auth;

import com.refscheduler.dto.organization.OrganizationMembershipResponse;
import com.refscheduler.dto.user.UserResponse;

import java.util.List;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    UserResponse user,
    List<OrganizationMembershipResponse> memberships
) {
}
