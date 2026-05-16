package com.refscheduler.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record JoinOrganizationRequest(
    @NotBlank String joinCode
) {
}
