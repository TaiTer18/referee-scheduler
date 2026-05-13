package com.refscheduler.dto.admin;

import com.refscheduler.dto.referee.AssignmentResponse;
import com.refscheduler.dto.referee.GameAvailabilityResponse;
import com.refscheduler.dto.user.UserResponse;

import java.util.List;

public record RefereeDetailResponse(
    UserResponse referee,
    List<GameAvailabilityResponse> availability,
    List<AssignmentResponse> assignments
) {
}
