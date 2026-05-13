package com.refscheduler.dto.referee;

import java.util.List;

public record AvailabilityStatusResponse(
    Integer refereeId,
    List<GameAvailabilityResponse> availability
) {
}
