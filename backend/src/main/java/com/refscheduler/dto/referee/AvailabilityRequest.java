package com.refscheduler.dto.referee;

import jakarta.validation.constraints.NotNull;

public record AvailabilityRequest(
    @NotNull Integer gameId,
    @NotNull Boolean isAvailable
) {
}
