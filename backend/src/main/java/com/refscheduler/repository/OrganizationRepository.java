package com.refscheduler.repository;

import com.refscheduler.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Integer> {
    boolean existsByJoinCode(String joinCode);
    Optional<Organization> findByJoinCode(String joinCode);
    Optional<Organization> findByJoinCodeAndActiveTrue(String joinCode);
}
