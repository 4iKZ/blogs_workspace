package com.blog.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JWTUtils {

    private static final Logger log = LoggerFactory.getLogger(JWTUtils.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.refresh-secret}")
    private String refreshSecret;

    @Value("${jwt.expiration:900}")
    private Long expiration;

    @Value("${jwt.refresh-expiration:604800}")
    private Long refreshExpiration;

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    // ── Access Token ──────────────────────────────────────────────────────────

    public String generateAccessToken(Long userId, String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration * 1000);
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("username", username)
                .claim("userId", userId)
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getAccessKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            parseAccessToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Access token 验证失败: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            return parseAccessToken(token).getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    public boolean isAccessToken(String token) {
        try {
            return ACCESS_TOKEN_TYPE.equals(parseAccessToken(token).get(TOKEN_TYPE_CLAIM, String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getUserIdFromToken(String token) {
        return extractUserId(parseAccessToken(token));
    }

    public String getUsernameFromToken(String token) {
        return parseAccessToken(token).get("username", String.class);
    }

    public Long getRemainingTime(String token) {
        try {
            Date exp = parseAccessToken(token).getExpiration();
            return Math.max(0, (exp.getTime() - System.currentTimeMillis()) / 1000);
        } catch (JwtException | IllegalArgumentException e) {
            return 0L;
        }
    }

    // ── Refresh Token ─────────────────────────────────────────────────────────

    public String generateRefreshToken(Long userId, String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpiration * 1000);
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("username", username)
                .claim("userId", userId)
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getRefreshKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateRefreshToken(String token) {
        try {
            parseRefreshToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Refresh token 验证失败: {}", e.getMessage());
            return false;
        }
    }

    public boolean isRefreshTokenExpired(String token) {
        try {
            return parseRefreshToken(token).getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            return REFRESH_TOKEN_TYPE.equals(parseRefreshToken(token).get(TOKEN_TYPE_CLAIM, String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getUserIdFromRefreshToken(String token) {
        return extractUserId(parseRefreshToken(token));
    }

    public String getUsernameFromRefreshToken(String token) {
        return parseRefreshToken(token).get("username", String.class);
    }

    public Long getRemainingRefreshTime(String token) {
        try {
            Date exp = parseRefreshToken(token).getExpiration();
            return Math.max(0, (exp.getTime() - System.currentTimeMillis()) / 1000);
        } catch (JwtException | IllegalArgumentException e) {
            return 0L;
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private SecretKey getAccessKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    private SecretKey getRefreshKey() {
        return Keys.hmacShaKeyFor(refreshSecret.getBytes());
    }

    private Claims parseAccessToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getAccessKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Claims parseRefreshToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getRefreshKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Long extractUserId(Claims claims) {
        Object userIdObj = claims.get("userId");
        if (userIdObj instanceof Integer) {
            return ((Integer) userIdObj).longValue();
        } else if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        } else if (userIdObj != null) {
            return Long.parseLong(userIdObj.toString());
        }
        return Long.parseLong(claims.getSubject());
    }
}