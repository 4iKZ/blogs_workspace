package com.blog.controller;

import com.blog.test.AbstractControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerIntegrationTest extends AbstractControllerTest {

    @Test
    @DisplayName("获取作者排行榜 - 未登录应允许访问")
    void getTopAuthors_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/user/top-authors")
                .param("limit", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("生成 GitHub OAuth state - 未登录应允许访问")
    void generateGithubState_shouldBePublic() throws Exception {
        mockMvc.perform(post("/api/user/auth/github/state"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("获取指定用户公开信息 - 未登录应允许访问")
    void getPublicUserInfo_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/user/public/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("校验访问令牌有效性 - 未登录应允许访问")
    void validateToken_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/user/token/validate"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("用户登出 - 未登录应允许访问")
    void logout_shouldBePublic() throws Exception {
        mockMvc.perform(post("/api/user/logout"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("用户注册 - 未登录应允许访问")
    void register_shouldBePublic() throws Exception {
        mockMvc.perform(post("/api/user/register")
                .contentType("application/json")
                .content("{\"username\":\"test\",\"email\":\"test@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("用户注册 - 缺少用户名应返回错误")
    void register_missingUsername_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/user/register")
                .contentType("application/json")
                .content("{\"email\":\"test@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("用户注册 - 缺少邮箱应返回错误")
    void register_missingEmail_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/user/register")
                .contentType("application/json")
                .content("{\"username\":\"test\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("用户注册 - 缺少密码应返回错误")
    void register_missingPassword_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/user/register")
                .contentType("application/json")
                .content("{\"username\":\"test\",\"email\":\"test@example.com\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("用户登录 - 未登录应允许访问")
    void login_shouldBePublic() throws Exception {
        mockMvc.perform(post("/api/user/login")
                .contentType("application/json")
                .content("{\"username\":\"test\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("用户登录 - 缺少用户名应返回错误")
    void login_missingUsername_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/user/login")
                .contentType("application/json")
                .content("{\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("用户登录 - 缺少密码应返回错误")
    void login_missingPassword_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/user/login")
                .contentType("application/json")
                .content("{\"username\":\"test\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("轮换刷新令牌 - 未登录应允许访问")
    void refreshToken_shouldBePublic() throws Exception {
        mockMvc.perform(post("/api/user/token/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("发送邮箱重置验证码 - 未登录应允许访问")
    void sendResetCode_shouldBePublic() throws Exception {
        mockMvc.perform(post("/api/user/password/reset/send")
                .contentType("application/json")
                .content("{\"email\":\"test@example.com\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("验证码重置密码 - 未登录应允许访问")
    void resetPasswordByCode_shouldBePublic() throws Exception {
        mockMvc.perform(post("/api/user/password/reset")
                .contentType("application/json")
                .content("{\"email\":\"test@example.com\",\"code\":\"123456\",\"newPassword\":\"newpassword123\"}"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("GitHub OAuth 回调接口 - 未登录应允许访问")
    void githubCallback_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/user/auth/github/callback")
                .param("code", "test-code"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("获取用户信息 - 未登录应返回 401")
    void getUserInfo_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/user/info"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("更新用户信息 - 未登录应返回 401")
    void updateUserInfo_shouldRequireAuth() throws Exception {
        mockMvc.perform(put("/api/user/info")
                .contentType("application/json")
                .content("{\"nickname\":\"test\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("修改密码 - 未登录应返回 401")
    void changePassword_shouldRequireAuth() throws Exception {
        mockMvc.perform(put("/api/user/password")
                .contentType("application/json")
                .content("{\"oldPassword\":\"old123\",\"newPassword\":\"new123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("上传用户头像 - 未登录应返回 401")
    void uploadAvatar_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/user/avatar/upload"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("关注用户 - 未登录应返回 401")
    void followUser_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/user/follow/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("取消关注用户 - 未登录应返回 401")
    void unfollowUser_shouldRequireAuth() throws Exception {
        mockMvc.perform(delete("/api/user/unfollow/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("检查是否关注 - 未登录应返回 401")
    void isFollowing_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/user/is-following/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("获取关注列表 - 未登录应返回 401")
    void getFollowings_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/user/followings")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("获取粉丝列表 - 未登录应返回 401")
    void getFollowers_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/user/followers")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("获取用户信息 - 登录后应放行到控制器")
    void getUserInfo_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/user/info")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("更新用户信息 - 登录后应放行到控制器")
    void updateUserInfo_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(put("/api/user/info")
                .contentType("application/json")
                .content("{\"nickname\":\"test\"}")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("检查是否关注 - 登录后应放行到控制器")
    void isFollowing_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/user/is-following/1")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("关注用户 - 登录后应放行到控制器")
    void followUser_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(post("/api/user/follow/1")
                .requestAttr("userId", 1L))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser
    @DisplayName("取消关注用户 - 登录后应放行到控制器")
    void unfollowUser_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/user/unfollow/1")
                .requestAttr("userId", 1L))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser
    @DisplayName("修改密码 - 登录后应放行到控制器")
    void changePassword_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(put("/api/user/password")
                .contentType("application/json")
                .content("{\"oldPassword\":\"old123\",\"newPassword\":\"new123\"}")
                .requestAttr("userId", 1L))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("获取关注列表 - 登录后应放行到控制器")
    void getFollowings_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/user/followings")
                .param("page", "1")
                .param("size", "10")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("获取粉丝列表 - 登录后应放行到控制器")
    void getFollowers_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/user/followers")
                .param("page", "1")
                .param("size", "10")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("上传用户头像 - 登录后应放行到控制器")
    void uploadAvatar_shouldReachControllerWhenAuthenticated() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", "test".getBytes());
        mockMvc.perform(multipart("/api/user/avatar/upload")
                .file(file)
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }
}
