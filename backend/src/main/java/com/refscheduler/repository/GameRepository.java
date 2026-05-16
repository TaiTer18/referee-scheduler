package com.refscheduler.repository;

import com.refscheduler.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface GameRepository extends JpaRepository<Game, Integer> {
    List<Game> findByGameDate(LocalDate gameDate);
    List<Game> findByStatus(String status);
    List<Game> findByAgeGroup(String ageGroup);
    List<Game> findByAssignedRefereeIdIsNullOrderByGameDateAscGameTimeAsc();
    List<Game> findAllByOrderByGameDateAscGameTimeAsc();
    List<Game> findByOrganizationIdInOrderByGameDateAscGameTimeAsc(Collection<Integer> organizationIds);
    List<Game> findByOrganizationIdInAndAssignedRefereeIdIsNullOrderByGameDateAscGameTimeAsc(Collection<Integer> organizationIds);
}
