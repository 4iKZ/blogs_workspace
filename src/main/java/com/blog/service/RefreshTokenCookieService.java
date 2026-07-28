package com.blog.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

@Service
public class RefreshTokenCookieService {

    public static final String COOKIE_NAME = "refresh_token";
    private static final String COOKIE_PATH = "/api/user";

    private final boolean secure;
    private final long maxAgeSeconds;

    public RefreshTokenCookieService(
            @Value("${security.refresh-cookie.secure:true}") boolean secure,
            @Value("${jwt.refresh-expiration-seconds:604800}") long maxAgeSeconds) {
        this.secure = secure;
        this.maxAgeSeconds = maxAgeSeconds;
    }

    public void setRefreshToken(HttpServletResponse response, String token) {
        add(response, token, maxAgeSeconds);
    }

    public Optional<String> readRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    public void clearRefreshToken(HttpServletResponse response) {
        add(response, "", 0);
    }

    private void add(HttpServletResponse response, String value, long maxAge) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
