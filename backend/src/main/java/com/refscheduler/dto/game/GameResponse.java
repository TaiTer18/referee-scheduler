package com.refscheduler.dto.game;

import com.refscheduler.model.Game;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record GameResponse(
    Integer id,
    LocalDate gameDate,
    LocalTime gameTime,
    String location,
    String homeTeam,
    String awayTeam,
    String ageGroup,
    String status,
    Integer assignedRefereeId,
    Integer organizationId,
    String notes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static GameResponse from(Game game) {
        return new GameResponse(
            game.getId(),
            game.getGameDate(),
            game.getGameTime(),
            game.getLocation(),
            game.getHomeTeam(),
            game.getAwayTeam(),
            game.getAgeGroup(),
            game.getStatus(),
            game.getAssignedRefereeId(),
            game.getOrganizationId(),
            game.getNotes(),
            game.getCreatedAt(),
            game.getUpdatedAt()
        );
    }
}
