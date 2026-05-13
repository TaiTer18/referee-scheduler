package com.refscheduler.repository;

import com.refscheduler.model.GameAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameAssignmentRepository extends JpaRepository<GameAssignment, Integer> {
    List<GameAssignment> findByGameId(Integer gameId);
    List<GameAssignment> findByRefereeId(Integer refereeId);
    List<GameAssignment> findByAssignedByAdminId(Integer adminId);
    Optional<GameAssignment> findTopByGameIdOrderByAssignedAtDesc(Integer gameId);
}
