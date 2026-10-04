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

    /** GitHub OAuth state 绑定 Cookie：把 state 绑定到发起端浏览器，防御登录 CSRF */
    public static final String OAUTH_STATE_COOKIE_NAME = "github_oauth_state";
    private static final String OAUTH_STATE_COOKIE_PATH = "/api/user/auth/github";
    // 与 UserServiceImpl.GITHUB_OAUTH_STATE_EXPIRE_MINUTES（10 分钟）保持一致
    private static final long OAUTH_STATE_MAX_AGE_SECONDS = 600;

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

    /** 下发 GitHub OAuth state Cookie（HttpOnly + Lax，绑定发起端浏览器） */
    public void setOauthState(HttpServletResponse response, String state) {
        addOauthState(response, state, OAUTH_STATE_MAX_AGE_SECONDS);
    }

    public Optional<String> readOauthState(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> OAUTH_STATE_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    public void clearOauthState(HttpServletResponse response) {
        addOauthState(response, "", 0);
    }

    private void addOauthState(HttpServletResponse response, String value, long maxAge) {
        ResponseCookie cookie = ResponseCookie.from(OAUTH_STATE_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(OAUTH_STATE_COOKIE_PATH)
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
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
