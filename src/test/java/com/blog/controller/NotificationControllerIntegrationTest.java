package com.blog.controller;

import com.blog.test.AbstractControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationControllerIntegrationTest extends AbstractControllerTest {

    @Test
    @DisplayName("未读消息数量 - 未登录应返回 401")
    void getUnreadCount_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/notification/unread-count"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("消息列表 - 未登录应返回 401")
    void getNotificationList_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/notification/list"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("标记消息为已读 - 未登录应返回 401")
    void markAsRead_shouldRequireAuth() throws Exception {
        mockMvc.perform(put("/api/notification/1/read"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("标记所有消息为已读 - 未登录应返回 401")
    void markAllAsRead_shouldRequireAuth() throws Exception {
        mockMvc.perform(put("/api/notification/read-all"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("删除消息 - 未登录应返回 401")
    void deleteNotification_shouldRequireAuth() throws Exception {
        mockMvc.perform(delete("/api/notification/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("未读消息数量 - 登录后应放行到控制器")
    @WithMockUser
    void getUnreadCount_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/notification/unread-count")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("消息列表 - 登录后应放行到控制器")
    @WithMockUser
    void getNotificationList_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/notification/list")
                .param("page", "1")
                .param("size", "20")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("标记消息为已读 - 登录后应放行到控制器")
    void markAsRead_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(put("/api/notification/1/read")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("标记所有消息为已读 - 登录后应放行到控制器")
    void markAllAsRead_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(put("/api/notification/read-all")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("删除消息 - 登录后应放行到控制器")
    void deleteNotification_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/notification/1")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }
}
