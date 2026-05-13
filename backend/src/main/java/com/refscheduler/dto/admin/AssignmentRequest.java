package com.refscheduler.dto.admin;

import jakarta.validation.constraints.NotNull;

public record AssignmentRequest(
    @NotNull Integer gameId,
    @NotNull Integer refereeId,
    String notes
) {
}
