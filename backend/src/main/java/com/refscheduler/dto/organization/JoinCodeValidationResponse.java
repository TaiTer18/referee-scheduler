package com.refscheduler.dto.organization;

public record JoinCodeValidationResponse(
    boolean valid,
    Integer organizationId,
    String organizationName
) {
}
