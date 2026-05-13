package com.refscheduler.dto.referee;

import com.refscheduler.dto.game.GameResponse;
import com.refscheduler.model.RefereeAvailability;

import java.time.LocalDateTime;

public record GameAvailabilityResponse(
    Integer availabilityId,
    Integer refereeId,
    Integer gameId,
    Boolean isAvailable,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    GameResponse game
) {
    public static GameAvailabilityResponse from(RefereeAvailability availability, GameResponse game) {
        return new GameAvailabilityResponse(
            availability.getId(),
            availability.getRefereeId(),
            availability.getGameId(),
            availability.getIsAvailable(),
            availability.getCreatedAt(),
            availability.getUpdatedAt(),
            game
        );
    }
}
