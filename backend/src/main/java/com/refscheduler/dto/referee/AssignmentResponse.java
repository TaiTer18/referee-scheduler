package com.refscheduler.dto.referee;

import com.refscheduler.dto.game.GameResponse;
import com.refscheduler.model.GameAssignment;

import java.time.LocalDateTime;

public record AssignmentResponse(
    Integer assignmentId,
    Integer gameId,
    Integer refereeId,
    Integer assignedByAdminId,
    LocalDateTime assignedAt,
    String status,
    String notes,
    GameResponse game
) {
    public static AssignmentResponse from(GameAssignment assignment, GameResponse game) {
        return new AssignmentResponse(
            assignment.getId(),
            assignment.getGameId(),
            assignment.getRefereeId(),
            assignment.getAssignedByAdminId(),
            assignment.getAssignedAt(),
            assignment.getStatus(),
            assignment.getNotes(),
            game
        );
    }
}
