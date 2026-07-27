package com.blog.security;

import com.blog.config.JwtProperties;
import com.blog.utils.JWTUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JwtExpirationConfigTest {

    private static final String ACCESS_SECRET = "access-secret-at-least-thirty-two-bytes-long";
    private static final String REFRESH_SECRET = "refresh-secret-at-least-thirty-two-bytes-long";

    @Test
    void accessAndRefreshExpirationsAreExactSecondsAndContainSessionClaims() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(ACCESS_SECRET);
        properties.setRefreshSecret(REFRESH_SECRET);
        properties.setAccessExpirationSeconds(900);
        properties.setRefreshExpirationSeconds(604800);
        JWTUtils jwt = new JWTUtils(properties);

        String access = jwt.generateAccessToken(7L, "alice", 3);
        String refresh = jwt.generateRefreshToken(7L, "alice", 3);

        Claims accessClaims = parse(access, ACCESS_SECRET);
        Claims refreshClaims = parse(refresh, REFRESH_SECRET);
        assertThat(accessClaims.getExpiration().getTime() - accessClaims.getIssuedAt().getTime()).isEqualTo(900_000);
        assertThat(refreshClaims.getExpiration().getTime() - refreshClaims.getIssuedAt().getTime()).isEqualTo(604_800_000);
        assertThat(accessClaims.getId()).isNotBlank();
        assertThat(refreshClaims.getId()).isNotBlank();
        assertThat(accessClaims.get("tokenVersion", Integer.class)).isEqualTo(3);
        assertThat(refreshClaims.get("tokenVersion", Integer.class)).isEqualTo(3);
        assertThat(accessClaims.get("tokenType", String.class)).isEqualTo("access");
        assertThat(refreshClaims.get("tokenType", String.class)).isEqualTo("refresh");
    }

    @Test
    void invalidAccessExpirationPreventsContextStartup() {
        new ApplicationContextRunner()
                .withUserConfiguration(Config.class)
                .withPropertyValues(
                        "jwt.secret=" + ACCESS_SECRET,
                        "jwt.refresh-secret=" + REFRESH_SECRET,
                        "jwt.access-expiration-seconds=59",
                        "jwt.refresh-expiration-seconds=604800")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void expirationBoundaryValuesAreAccepted() {
        new ApplicationContextRunner()
                .withUserConfiguration(Config.class)
                .withPropertyValues(
                        "jwt.secret=" + ACCESS_SECRET,
                        "jwt.refresh-secret=" + REFRESH_SECRET,
                        "jwt.access-expiration-seconds=60",
                        "jwt.refresh-expiration-seconds=86400")
                .run(context -> assertThat(context).hasNotFailed());
        new ApplicationContextRunner()
                .withUserConfiguration(Config.class)
                .withPropertyValues(
                        "jwt.secret=" + ACCESS_SECRET,
                        "jwt.refresh-secret=" + REFRESH_SECRET,
                        "jwt.access-expiration-seconds=3600",
                        "jwt.refresh-expiration-seconds=2592000")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void invalidRefreshExpirationPreventsContextStartup() {
        for (long invalid : new long[]{86399, 2592001}) {
            new ApplicationContextRunner()
                    .withUserConfiguration(Config.class)
                    .withPropertyValues(
                            "jwt.secret=" + ACCESS_SECRET,
                            "jwt.refresh-secret=" + REFRESH_SECRET,
                            "jwt.access-expiration-seconds=900",
                            "jwt.refresh-expiration-seconds=" + invalid)
                    .run(context -> assertThat(context).hasFailed());
        }
    }

    @Test
    void missingOrShortSecretsPreventContextStartup() {
        new ApplicationContextRunner()
                .withUserConfiguration(Config.class)
                .withPropertyValues(
                        "jwt.access-expiration-seconds=900",
                        "jwt.refresh-expiration-seconds=604800")
                .run(context -> assertThat(context).hasFailed());
        new ApplicationContextRunner()
                .withUserConfiguration(Config.class)
                .withPropertyValues(
                        "jwt.secret=too-short",
                        "jwt.refresh-secret=also-too-short",
                        "jwt.access-expiration-seconds=900",
                        "jwt.refresh-expiration-seconds=604800")
                .run(context -> assertThat(context).hasFailed());
    }

    private Claims parse(String token, String secret) {
        return Jwts.parserBuilder()
                .setSigningKey(secret.getBytes(StandardCharsets.UTF_8))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(JwtProperties.class)
    static class Config {
    }
}
