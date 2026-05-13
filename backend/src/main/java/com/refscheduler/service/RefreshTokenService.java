package com.refscheduler.service;

import com.refscheduler.exception.UnauthorizedException;
import com.refscheduler.model.RefreshToken;
import com.refscheduler.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenExpirationDays;

    public RefreshTokenService(
        RefreshTokenRepository refreshTokenRepository,
        @Value("${app.refresh-token.expiration-days}") long refreshTokenExpirationDays
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }

    @Transactional
    public String createRefreshToken(Integer userId) {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = new RefreshToken(
            userId,
            tokenHash,
            LocalDateTime.now().plusDays(refreshTokenExpirationDays)
        );

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    public RefreshToken findValidToken(String rawToken) {
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new UnauthorizedException("Refresh token not found."));

        if (refreshToken.getRevokedAt() != null) {
            throw new UnauthorizedException("Refresh token has been revoked.");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Refresh token has expired.");
        }

        return refreshToken;
    }

    @Transactional
    public void revokeToken(String rawToken) {
        RefreshToken refreshToken = findValidToken(rawToken);
        revokeToken(refreshToken);
    }

    @Transactional
    public void revokeToken(RefreshToken refreshToken) {
        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashedBytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm not found.", exception);
        }
    }
}
