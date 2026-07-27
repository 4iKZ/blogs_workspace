package com.blog.controller;

import com.blog.common.Result;
import com.blog.dto.TokenRefreshResponseDTO;
import com.blog.dto.UserDTO;
import com.blog.service.RefreshTokenCookieService;
import com.blog.service.AccessLogService;
import com.blog.service.TOSService;
import com.blog.service.UserService;
import com.blog.service.impl.CustomUserDetailsServiceImpl;
import com.blog.mapper.UserMapper;
import com.blog.utils.JWTUtils;
import com.blog.utils.RedisUtils;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RefreshTokenCookieService.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private TOSService tosService;

    @MockBean
    private AccessLogService accessLogService;

    @MockBean
    private JWTUtils jwtUtils;

    @MockBean
    private CustomUserDetailsServiceImpl customUserDetailsService;

    @MockBean
    private RedisUtils redisUtils;

    @MockBean
    private UserMapper userMapper;

    @Test
    void registerUsesCurrentSingularRouteAndDtoContract() throws Exception {
        Mockito.when(userService.register(any())).thenReturn(Result.success("注册成功"));

        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"alice",
                                  "password":"Strong123!",
                                  "confirmPassword":"Strong123!",
                                  "email":"alice@example.com",
                                  "emailCode":"123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("注册成功"));
    }

    @Test
    void loginReturnsAccessTokenAndSetsHttpOnlyRefreshCookie() throws Exception {
        UserDTO user = new UserDTO();
        user.setAccessToken("access-token");
        user.setRefreshToken("refresh-token");
        Mockito.when(userService.login(any())).thenReturn(Result.success(user));

        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"Strong123!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(cookie().httpOnly("refresh_token", true))
                .andExpect(cookie().value("refresh_token", "refresh-token"));
    }

    @Test
    void refreshUsesCookieAndEmptyBodyAndReturnsOnlyAccessToken() throws Exception {
        Mockito.when(userService.refreshToken("old-refresh"))
                .thenReturn(Result.success(new TokenRefreshResponseDTO("new-access", "new-refresh")));

        mockMvc.perform(post("/api/user/token/refresh")
                        .cookie(new Cookie("refresh_token", "old-refresh")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("new-access"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(cookie().httpOnly("refresh_token", true))
                .andExpect(cookie().value("refresh_token", "new-refresh"));
    }

    @Test
    void logoutUsesCookieAndClearsIt() throws Exception {
        Mockito.when(userService.logout(eq(7L), eq("refresh-token"), eq("Bearer access-token")))
                .thenReturn(Result.success());

        mockMvc.perform(post("/api/user/logout")
                        .header("Authorization", "Bearer access-token")
                        .requestAttr("userId", 7L)
                        .cookie(new Cookie("refresh_token", "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("refresh_token", 0));
    }
}
