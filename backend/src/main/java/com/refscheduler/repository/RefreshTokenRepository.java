package com.refscheduler.repository;

import com.refscheduler.model.RefreshToken;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {
    List<RefreshToken> findByUserId(Integer userId);
    Optional<RefreshToken> findByTokenHash(String tokenHash);
} 