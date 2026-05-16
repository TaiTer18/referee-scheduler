package com.refscheduler.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "games")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "game_date", nullable = false)
    private LocalDate gameDate;

    @Column(name = "game_time", nullable = false)
    private LocalTime gameTime;

    @Column(nullable = false)
    private String location;

    @Column(name = "home_team", nullable = false)
    private String homeTeam;

    @Column(name = "away_team", nullable = false)
    private String awayTeam;

    @Column(name = "age_group")
    private String ageGroup;

    @Column(length = 20)
    private String status;

    @Column(name = "assigned_referee_id")
    private Integer assignedRefereeId;

    @Column(name = "organization_id", nullable = false)
    private Integer organizationId;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Game(LocalDate gameDate, LocalTime gameTime, String location,
                String homeTeam, String awayTeam, String ageGroup) {
        this.gameDate = gameDate;
        this.gameTime = gameTime;
        this.location = location;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.ageGroup = ageGroup;
        this.status = "OPEN";
    }

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null || status.isBlank()) {
            status = "OPEN";
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
