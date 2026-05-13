package com.refscheduler.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
        @Value("${app.jwt.secret}") String secret,
        @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        byte[] keyBytes = secret.length() >= 32
            ? secret.getBytes(StandardCharsets.UTF_8)
            : Decoders.BASE64.decode(secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    public String generateToken(UserPrincipal userPrincipal) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
            .subject(userPrincipal.getUsername())
            .claim("userId", userPrincipal.getId())
            .claim("role", userPrincipal.getRole())
            .issuedAt(now)
            .expiration(expiration)
            .signWith(signingKey)
            .compact();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserPrincipal userPrincipal) {
        Claims claims = extractClaims(token);
        return claims.getSubject().equals(userPrincipal.getUsername())
            && claims.getExpiration().after(new Date());
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
