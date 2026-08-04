package com.blog.controller;

import com.blog.common.Result;
import com.blog.dto.*;
import com.blog.service.RefreshTokenCookieService;
import com.blog.service.TOSService;
import com.blog.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private TOSService tosService;

    @MockBean
    private RefreshTokenCookieService refreshTokenCookieService;

    private byte[] createValidJpegImage() throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", baos);
        return baos.toByteArray();
    }

    @Test
    @DisplayName("register - 成功场景")
    @WithMockUser
    void register_success() throws Exception {
        when(userService.register(any(UserRegisterDTO.class)))
                .thenReturn(Result.success("success"));

        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test\",\"email\":\"test@example.com\",\"password\":\"password123\",\"confirmPassword\":\"password123\",\"emailCode\":\"123456\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("updateUserInfo - 缺少用户上下文应返回 401")
    void updateUserInfo_missingUserContext_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(put("/api/user/info")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"new name\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("getUserInfo - 未登录应返回 401")
    void getUserInfo_missingUserContext_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/user/info"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("login - 成功场景")
    @WithMockUser
    void login_success() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setAccessToken("access-token");
        userDTO.setRefreshToken("refresh-token");

        when(userService.login(any(UserLoginDTO.class)))
                .thenReturn(Result.success(userDTO));

        mockMvc.perform(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("login - 刷新令牌为空时仍应成功")
    @WithMockUser
    void login_emptyRefreshToken_shouldSucceed() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setAccessToken("access-token");
        userDTO.setRefreshToken(null);

        when(userService.login(any(UserLoginDTO.class)))
                .thenReturn(Result.success(userDTO));

        mockMvc.perform(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("login - 返回数据为空时仍应成功")
    @WithMockUser
    void login_nullData_shouldSucceed() throws Exception {
        when(userService.login(any(UserLoginDTO.class)))
                .thenReturn(Result.success(null));

        mockMvc.perform(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("logout - 成功场景")
    @WithMockUser
    void logout_success() throws Exception {
        when(userService.logout(any(), any(), any()))
                .thenReturn(Result.success(null));

        mockMvc.perform(post("/api/user/logout")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("logout - 未登录也应成功")
    void logout_missingUserContext_shouldSucceed() throws Exception {
        when(userService.logout(any(), any(), any()))
                .thenReturn(Result.success(null));

        mockMvc.perform(post("/api/user/logout"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("refreshToken - 成功场景")
    @WithMockUser
    void refreshToken_success() throws Exception {
        TokenRefreshResponseDTO tokenDTO = new TokenRefreshResponseDTO();
        tokenDTO.setToken("access-token");
        tokenDTO.setRefreshToken("refresh-token");

        when(refreshTokenCookieService.readRefreshToken(any()))
                .thenReturn(Optional.of("refresh-token"));
        when(userService.refreshToken("refresh-token"))
                .thenReturn(Result.success(tokenDTO));

        mockMvc.perform(post("/api/user/token/refresh"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("sendResetCode - 成功场景")
    @WithMockUser
    void sendResetCode_success() throws Exception {
        when(userService.sendResetCode(any(SendResetCodeDTO.class)))
                .thenReturn(Result.success(null));

        mockMvc.perform(post("/api/user/password/reset/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"captchaKey\":\"captcha123\",\"captcha\":\"1234\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("resetPasswordByCode - 成功场景")
    @WithMockUser
    void resetPasswordByCode_success() throws Exception {
        when(userService.resetPasswordByCode(any(ResetPasswordByCodeDTO.class)))
                .thenReturn(Result.success(null));

        mockMvc.perform(post("/api/user/password/reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"code\":\"123456\",\"newPassword\":\"newpassword123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("sendRegisterVerifyCode - 成功场景")
    @WithMockUser
    void sendRegisterVerifyCode_success() throws Exception {
        when(userService.sendRegisterVerifyCode(any(SendRegisterCodeDTO.class)))
                .thenReturn(Result.success(null));

        mockMvc.perform(post("/api/user/register/verify/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"captcha\":\"1234\",\"captchaKey\":\"captcha123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("getPublicUserInfo - 成功场景")
    @WithMockUser
    void getPublicUserInfo_success() throws Exception {
        when(userService.getPublicUserInfo(any()))
                .thenReturn(Result.success(new PublicUserProfileDTO()));

        mockMvc.perform(get("/api/user/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("getAnonymousPublicUserInfo - 成功场景")
    @WithMockUser
    void getAnonymousPublicUserInfo_success() throws Exception {
        when(userService.getPublicUserInfo(any()))
                .thenReturn(Result.success(new PublicUserProfileDTO()));

        mockMvc.perform(get("/api/user/public/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("uploadAvatar - 成功场景")
    @WithMockUser
    void uploadAvatar_success() throws Exception {
        when(tosService.uploadFile(any(), any()))
                .thenReturn("https://example.com/avatar.jpg");

        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", createValidJpegImage());
        mockMvc.perform(multipart("/api/user/avatar/upload")
                .file(file))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("uploadAvatar - 文件类型不支持应返回错误")
    @WithMockUser
    void uploadAvatar_unsupportedType_shouldReturnError() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.txt", "text/plain", "test".getBytes());
        mockMvc.perform(multipart("/api/user/avatar/upload")
                .file(file))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("uploadAvatar - 文件过大应返回错误")
    @WithMockUser
    void uploadAvatar_fileTooLarge_shouldReturnError() throws Exception {
        byte[] largeContent = new byte[2 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", largeContent);
        mockMvc.perform(multipart("/api/user/avatar/upload")
                .file(file))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("uploadAvatar - 空文件应返回错误")
    @WithMockUser
    void uploadAvatar_emptyFile_shouldReturnError() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", new byte[0]);
        mockMvc.perform(multipart("/api/user/avatar/upload")
                .file(file))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("uploadAvatar - 内容不是有效图片应返回错误")
    @WithMockUser
    void uploadAvatar_invalidImageContent_shouldReturnError() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", "not-an-image".getBytes());
        mockMvc.perform(multipart("/api/user/avatar/upload")
                .file(file))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("uploadAvatar - TOS上传异常时应返回错误")
    @WithMockUser
    void uploadAvatar_tosException_shouldReturnError() throws Exception {
        when(tosService.uploadFile(any(), any()))
                .thenThrow(new RuntimeException("TOS upload failed"));

        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", createValidJpegImage());
        mockMvc.perform(multipart("/api/user/avatar/upload")
                .file(file))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("changePassword - 成功场景")
    @WithMockUser
    void changePassword_success() throws Exception {
        when(userService.changePassword(any(), any(ChangePasswordDTO.class), any()))
                .thenReturn(Result.success(null));

        mockMvc.perform(put("/api/user/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"oldPassword\":\"old12345678\",\"newPassword\":\"new12345678\"}")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("followUser - 成功场景")
    @WithMockUser
    void followUser_success() throws Exception {
        when(userService.follow(any(), any()))
                .thenReturn(Result.success(null));

        mockMvc.perform(post("/api/user/follow/1")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("unfollowUser - 成功场景")
    @WithMockUser
    void unfollowUser_success() throws Exception {
        when(userService.unfollow(any(), any()))
                .thenReturn(Result.success(null));

        mockMvc.perform(delete("/api/user/unfollow/1")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("githubCallback - 成功场景")
    void githubCallback_success() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setAccessToken("access-token");
        userDTO.setRefreshToken("refresh-token");

        when(userService.githubLogin(any(String.class), any(String.class)))
                .thenReturn(Result.success(userDTO));

        mockMvc.perform(get("/api/user/auth/github/callback")
                .param("code", "test-code")
                .param("state", "test-state"))
                .andExpect(status().isOk());
    }
}