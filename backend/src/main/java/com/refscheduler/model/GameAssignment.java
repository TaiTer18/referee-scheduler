package com.refscheduler.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_assignments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "game_id", nullable = false)
    private Integer gameId;

    @Column(name = "referee_id", nullable = false)
    private Integer refereeId;

    @Column(name = "assigned_by_admin_id", nullable = false)
    private Integer assignedByAdminId;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    @Column(length = 20)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public GameAssignment(Integer gameId, Integer refereeId, Integer assignedByAdminId) {
        this.gameId = gameId;
        this.refereeId = refereeId;
        this.assignedByAdminId = assignedByAdminId;
        this.status = "ASSIGNED";
    }

    @PrePersist
    public void onCreate() {
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
        if (status == null || status.isBlank()) {
            status = "ASSIGNED";
        }
    }
}
