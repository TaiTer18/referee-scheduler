package com.refscheduler.dto.game;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record GameRequest(
    @NotNull LocalDate gameDate,
    @NotNull LocalTime gameTime,
    @NotBlank String location,
    @NotBlank String homeTeam,
    @NotBlank String awayTeam,
    String ageGroup,
    String status,
    String notes
) {
}
