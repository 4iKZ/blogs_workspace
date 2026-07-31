package com.blog.controller;

import com.blog.test.AbstractControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ImageControllerIntegrationTest extends AbstractControllerTest {

    @Test
    @DisplayName("提取图片元信息 - 未登录应返回 401")
    void extractMetadata_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/image/metadata"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("转换图片格式 - 未登录应返回 401")
    void convertFormat_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/image/convert")
                .param("format", "png"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("转换图片格式并下载 - 未登录应返回 401")
    void convertAndDownload_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/image/convert/download")
                .param("format", "png"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("批量转换图片格式 - 未登录应返回 401")
    void batchConvertFormat_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/image/batch-convert")
                .param("format", "png"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("获取支持的输出格式列表 - 未登录应返回 401")
    void getSupportedFormats_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/image/formats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("压缩图片 - 未登录应返回 401")
    void compressImage_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/image/compress")
                .param("maxWidth", "1024")
                .param("maxHeight", "1024")
                .param("quality", "0.8"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("验证图片文件 - 未登录应返回 401")
    void validateImage_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/image/validate"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("提取图片元信息 - 登录后应放行到控制器")
    void extractMetadata_shouldReachControllerWhenAuthenticated() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());
        mockMvc.perform(multipart("/api/image/metadata")
                .file(file))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("转换图片格式 - 登录后应放行到控制器")
    void convertFormat_shouldReachControllerWhenAuthenticated() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());
        mockMvc.perform(multipart("/api/image/convert")
                .file(file)
                .param("format", "png"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("转换图片格式 - 支持转换为 jpg")
    void convertFormat_toJpg_shouldReachControllerWhenAuthenticated() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "test".getBytes());
        mockMvc.perform(multipart("/api/image/convert")
                .file(file)
                .param("format", "jpg"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("转换图片格式 - 支持转换为 webp")
    void convertFormat_toWebp_shouldReachControllerWhenAuthenticated() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());
        mockMvc.perform(multipart("/api/image/convert")
                .file(file)
                .param("format", "webp"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("转换图片格式并下载 - 登录后应放行到控制器")
    void convertAndDownload_shouldReachControllerWhenAuthenticated() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());
        mockMvc.perform(multipart("/api/image/convert/download")
                .file(file)
                .param("format", "png"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("批量转换图片格式 - 登录后应放行到控制器")
    void batchConvertFormat_shouldReachControllerWhenAuthenticated() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "test.jpg", "image/jpeg", "test".getBytes());
        mockMvc.perform(multipart("/api/image/batch-convert")
                .file(file)
                .param("format", "png"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("获取支持的输出格式列表 - 登录后应放行到控制器")
    void getSupportedFormats_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/image/formats"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("压缩图片 - 登录后应放行到控制器")
    void compressImage_shouldReachControllerWhenAuthenticated() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());
        mockMvc.perform(multipart("/api/image/compress")
                .file(file)
                .param("maxWidth", "1024")
                .param("maxHeight", "1024")
                .param("quality", "0.8"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("验证图片文件 - 登录后应放行到控制器")
    void validateImage_shouldReachControllerWhenAuthenticated() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());
        mockMvc.perform(multipart("/api/image/validate")
                .file(file))
                .andExpect(status().isOk());
    }
}
