package com.frankie.fintech.service;

import com.frankie.fintech.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private final String jwtSecret;
    private final long accessTokenTtlMs;
    private final long refreshTokenTtlMs;

    public JwtService(
        @Value("${app.jwt.secret:fintech-secret-key-please-change-this-value-123456}") String jwtSecret,
        @Value("${app.jwt.access-token-ttl-ms:900000}") long accessTokenTtlMs,
        @Value("${app.jwt.refresh-token-ttl-ms:604800000}") long refreshTokenTtlMs
    ) {
        this.jwtSecret = jwtSecret;
        this.accessTokenTtlMs = accessTokenTtlMs;
        this.refreshTokenTtlMs = refreshTokenTtlMs;
    }

    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", user.getId() != null ? user.getId().toString() : null);
        claims.put("roles", user.getRoles() == null ? java.util.Set.of() : user.getRoles());
        return buildToken(claims, user.getEmail(), accessTokenTtlMs);
    }

    public String generateRefreshToken(User user) {
        return buildToken(Map.of("type", "refresh"), user.getEmail(), refreshTokenTtlMs);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username != null && username.equals(userDetails.getUsername()) && isTokenNotExpired(token);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private String buildToken(Map<String, Object> claims, String subject, long ttlMs) {
        Instant now = Instant.now();
        return Jwts.builder()
            .claims(claims)
            .subject(subject)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(ttlMs)))
            .signWith(getSignInKey())
            .compact();
    }

    private boolean isTokenNotExpired(String token) {
        Date expiration = extractClaim(token, Claims::getExpiration);
        return expiration.after(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSignInKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
