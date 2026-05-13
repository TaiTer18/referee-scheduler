package com.refscheduler.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "referee_availability")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefereeAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "referee_id", nullable = false)
    private Integer refereeId;

    @Column(name = "game_id", nullable = false)
    private Integer gameId;

    @Column(name = "is_available")
    private Boolean isAvailable;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public RefereeAvailability(Integer refereeId, Integer gameId, Boolean isAvailable) {
        this.refereeId = refereeId;
        this.gameId = gameId;
        this.isAvailable = isAvailable;
    }

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (isAvailable == null) {
            isAvailable = Boolean.FALSE;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
