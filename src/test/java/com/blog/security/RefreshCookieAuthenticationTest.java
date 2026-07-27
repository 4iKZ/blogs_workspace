package com.blog.security;

import com.blog.service.RefreshTokenCookieService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshCookieAuthenticationTest {

    @Test
    void refreshCookieIsHttpOnlyStrictScopedAndSecureInProduction() {
        RefreshTokenCookieService service = new RefreshTokenCookieService(true, 604800);
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.setRefreshToken(response, "refresh-token");

        String header = response.getHeader("Set-Cookie");
        assertThat(header)
                .contains("refresh_token=refresh-token")
                .contains("Path=/api/user")
                .contains("Max-Age=604800")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Strict");
    }

    @Test
    void readsAndClearsOnlyTheRefreshCookie() {
        RefreshTokenCookieService service = new RefreshTokenCookieService(false, 604800);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("other", "ignored"), new Cookie("refresh_token", "expected"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(service.readRefreshToken(request)).contains("expected");
        service.clearRefreshToken(response);

        assertThat(response.getHeader("Set-Cookie"))
                .contains("refresh_token=")
                .contains("Max-Age=0")
                .contains("HttpOnly")
                .contains("SameSite=Strict")
                .doesNotContain("Secure");
    }
}
