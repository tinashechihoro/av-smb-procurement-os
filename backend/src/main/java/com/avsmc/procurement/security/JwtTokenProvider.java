package com.avsmc.procurement.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    static final String TOKEN_TYPE_ACCESS = "access";
    static final String TOKEN_TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final long expirationMs;
    private final long refreshExpirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs,
            @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        if (secret == null || secret.isBlank() || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "JWT secret must be at least 32 bytes. Set the JWT_SECRET environment variable.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateToken(UUID userId, UUID organisationId, String roleCode) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("orgId", organisationId.toString())
                .claim("role", roleCode)
                .claim("type", TOKEN_TYPE_ACCESS)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(UUID userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", TOKEN_TYPE_REFRESH)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshExpirationMs))
                .signWith(key)
                .compact();
    }

    public UUID getUserIdFromToken(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public UUID getOrganisationIdFromToken(String token) {
        String orgId = parseClaims(token).get("orgId", String.class);
        if (orgId == null) {
            throw new JwtException("Token has no organisation claim");
        }
        return UUID.fromString(orgId);
    }

    public String getRoleCodeFromToken(String token) {
        String role = parseClaims(token).get("role", String.class);
        if (role == null) {
            throw new JwtException("Token has no role claim");
        }
        return role;
    }

    public boolean isRefreshToken(String token) {
        return TOKEN_TYPE_REFRESH.equals(parseClaims(token).get("type", String.class));
    }

    /**
     * Validates a token for API access: signature, expiry and access-type.
     * Refresh tokens are rejected so they cannot be replayed as access tokens.
     */
    public boolean validateAccessToken(String token) {
        try {
            Claims claims = parseClaims(token);
            String type = claims.get("type", String.class);
            return TOKEN_TYPE_ACCESS.equals(type)
                    && claims.getSubject() != null
                    && claims.get("orgId", String.class) != null
                    && claims.get("role", String.class) != null;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Validates a refresh token presented to the /auth/refresh endpoint.
     */
    public boolean validateRefreshToken(String token) {
        try {
            return isRefreshToken(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
