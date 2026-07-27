package com.blog.security;

import com.blog.common.Result;
import com.blog.controller.UserController;
import com.blog.dto.TokenRefreshResponseDTO;
import com.blog.dto.UserDTO;
import com.blog.dto.UserLoginDTO;
import com.blog.exception.BusinessException;
import com.blog.service.RefreshTokenCookieService;
import com.blog.service.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void loginControllerMovesRefreshTokenToCookieAndRemovesItFromBody() {
        UserController controller = new UserController();
        UserService userService = mock(UserService.class);
        RefreshTokenCookieService cookies = new RefreshTokenCookieService(true, 604800);
        UserLoginDTO request = new UserLoginDTO();
        UserDTO user = new UserDTO();
        user.setAccessToken("access-token");
        user.setRefreshToken("refresh-token");
        when(userService.login(request)).thenReturn(Result.success(user));
        setField(controller, "userService", userService);
        setField(controller, "refreshTokenCookieService", cookies);
        MockHttpServletResponse response = new MockHttpServletResponse();

        Result<UserDTO> result = controller.login(request, response);

        assertThat(result.getData().getAccessToken()).isEqualTo("access-token");
        assertThat(result.getData().getRefreshToken()).isNull();
        assertThat(response.getHeader("Set-Cookie"))
                .contains("refresh_token=refresh-token")
                .contains("HttpOnly");
    }

    @Test
    void refreshControllerUsesCookieWithNoRequestBodyAndRotatesCookie() throws Exception {
        UserController controller = new UserController();
        UserService userService = mock(UserService.class);
        RefreshTokenCookieService cookies = new RefreshTokenCookieService(true, 604800);
        when(userService.refreshToken("old-refresh"))
                .thenReturn(Result.success(new TokenRefreshResponseDTO("new-access", "new-refresh")));
        setField(controller, "userService", userService);
        setField(controller, "refreshTokenCookieService", cookies);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refresh_token", "old-refresh"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        var result = controller.refreshToken(request, response);

        assertThat(result.getData()).containsEntry("token", "new-access").hasSize(1);
        assertThat(response.getHeader("Set-Cookie")).contains("refresh_token=new-refresh");
        assertThat(UserController.class.getMethod(
                "refreshToken", jakarta.servlet.http.HttpServletRequest.class,
                jakarta.servlet.http.HttpServletResponse.class).getParameterCount()).isEqualTo(2);
    }

    @Test
    void logoutControllerPassesCookieToServiceAndAlwaysClearsIt() {
        UserController controller = new UserController();
        UserService userService = mock(UserService.class);
        RefreshTokenCookieService cookies = new RefreshTokenCookieService(true, 604800);
        when(userService.logout(7L, "refresh-token", "Bearer access-token"))
                .thenReturn(Result.success());
        setField(controller, "userService", userService);
        setField(controller, "refreshTokenCookieService", cookies);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 7L);
        request.setCookies(new Cookie("refresh_token", "refresh-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.logout("Bearer access-token", request, response);

        verify(userService).logout(7L, "refresh-token", "Bearer access-token");
        assertThat(response.getHeader("Set-Cookie"))
                .contains("refresh_token=")
                .contains("Max-Age=0");
    }

    @Test
    void refreshControllerRejectsMissingCookieBeforeCallingService() {
        UserController controller = new UserController();
        setField(controller, "userService", mock(UserService.class));
        setField(controller, "refreshTokenCookieService", new RefreshTokenCookieService(true, 604800));

        assertThatThrownBy(() -> controller.refreshToken(
                new MockHttpServletRequest(), new MockHttpServletResponse()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("缺少刷新令牌");
    }

    private static void setField(Object target, String name, Object value) {
        try {
            var field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
