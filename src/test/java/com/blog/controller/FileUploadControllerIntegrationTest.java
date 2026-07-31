package com.blog.controller;

import com.blog.test.AbstractControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FileUploadControllerIntegrationTest extends AbstractControllerTest {

    @Test
    @DisplayName("上传图片 - 未登录应返回 401")
    void uploadImage_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/file/upload/image"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("上传文件 - 未登录应返回 401")
    void uploadFile_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/file/upload/file"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("批量上传文件 - 未登录应返回 401")
    void batchUploadFiles_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/file/upload/batch"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("获取文件列表 - 未登录应返回 401")
    void getFileList_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/file/list")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("获取文件详情 - 未登录应返回 401")
    void getFileInfo_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/file/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("删除文件 - 未登录应返回 401")
    void deleteFile_shouldRequireAuth() throws Exception {
        mockMvc.perform(delete("/api/file/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("检查MD5文件是否存在 - 未登录应返回 401")
    void checkFileByMd5_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/file/check/md5/abc123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("上传图片 - 登录后应放行到控制器")
    void uploadImage_shouldReachControllerWhenAuthenticated() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());
        mockMvc.perform(multipart("/api/file/upload/image")
                .file(file))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("上传文件 - 登录后应放行到控制器")
    void uploadFile_shouldReachControllerWhenAuthenticated() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "test".getBytes());
        mockMvc.perform(multipart("/api/file/upload/file")
                .file(file))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("批量上传文件 - 登录后应放行到控制器")
    void batchUploadFiles_shouldReachControllerWhenAuthenticated() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "test.txt", "text/plain", "test".getBytes());
        mockMvc.perform(multipart("/api/file/upload/batch")
                .file(file))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("获取文件列表 - 登录后应放行到控制器")
    void getFileList_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/file/list")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("获取文件详情 - 登录后应放行到控制器")
    void getFileInfo_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/file/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("删除文件 - 登录后应放行到控制器")
    void deleteFile_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/file/1"))
                .andExpect(status().isOk());
    }
}
