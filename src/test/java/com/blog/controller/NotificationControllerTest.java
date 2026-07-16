package com.blog.controller;

import com.blog.test.AbstractControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class NotificationControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("未读通知数量 - 需要认证")
    void unreadCount_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/notification/unread-count"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("通知列表 - 需要认证")
    void notificationList_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/notification/list"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("标记通知已读 - 需要认证")
    void markAsRead_shouldRequireAuth() throws Exception {
        mockMvc.perform(put("/api/notification/1/read"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("全部标记已读 - 需要认证")
    void markAllAsRead_shouldRequireAuth() throws Exception {
        mockMvc.perform(put("/api/notification/read-all"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("删除通知 - 需要认证")
    void deleteNotification_shouldRequireAuth() throws Exception {
        mockMvc.perform(delete("/api/notification/1"))
                .andExpect(status().isUnauthorized());
    }
}
