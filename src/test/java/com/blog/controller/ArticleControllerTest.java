package com.blog.controller;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.dto.ArticleDTO;
import com.blog.dto.ChunkedUploadIdRequest;
import com.blog.dto.ChunkedUploadInitRequest;
import com.blog.service.ArticleRankService;
import com.blog.service.ArticleService;
import com.blog.service.ChunkedUploadService;
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

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ArticleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ArticleService articleService;

    @MockBean
    private ArticleRankService articleRankService;

    @MockBean
    private ChunkedUploadService chunkedUploadService;

    @Test
    @DisplayName("initChunkedUpload - 成功场景")
    @WithMockUser
    void initChunkedUpload_success() throws Exception {
        ChunkedUploadService.UploadInitialization initialized = new ChunkedUploadService.UploadInitialization(
                "upload-123", 1024 * 1024, 10L, System.currentTimeMillis() + 3600000);

        when(chunkedUploadService.initUpload(anyLong(), anyString(), anyLong(), anyInt(), anyString()))
                .thenReturn(initialized);

        Map<String, Object> request = Map.of(
                "fileName", "test.jpg",
                "fileSize", 1024L,
                "totalChunks", 10,
                "fileHash", "hash123"
        );

        mockMvc.perform(post("/api/article/init-upload")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectToJson(request))
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("uploadChunk - 成功场景")
    @WithMockUser
    void uploadChunk_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "chunk1.bin", "application/octet-stream", "chunk data".getBytes());
        ChunkedUploadService.ChunkedUploadStatus status = new ChunkedUploadService.ChunkedUploadStatus(
                "upload-123", "test.jpg", 1024L, 10);

        when(chunkedUploadService.uploadChunk(anyLong(), anyString(), anyInt(), any()))
                .thenReturn(true);
        when(chunkedUploadService.getUploadStatus(anyLong(), anyString()))
                .thenReturn(status);

        mockMvc.perform(multipart("/api/article/upload-chunk")
                .file(file)
                .param("uploadId", "upload-123")
                .param("index", "0")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("completeChunkedUpload - 成功场景")
    @WithMockUser
    void completeChunkedUpload_success() throws Exception {
        when(chunkedUploadService.completeUpload(anyLong(), anyString()))
                .thenReturn("https://example.com/file.jpg");

        Map<String, Object> request = Map.of(
                "uploadId", "upload-123"
        );

        mockMvc.perform(post("/api/article/complete-upload")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectToJson(request))
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("cancelChunkedUpload - 成功场景")
    @WithMockUser
    void cancelChunkedUpload_success() throws Exception {
        when(chunkedUploadService.cancelUpload(anyLong(), anyString()))
                .thenReturn(true);

        Map<String, Object> request = Map.of(
                "uploadId", "upload-123"
        );

        mockMvc.perform(post("/api/article/cancel-upload")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectToJson(request))
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("checkUpload - 成功场景")
    @WithMockUser
    void checkUpload_success() throws Exception {
        when(chunkedUploadService.checkResumeUpload(anyLong(), anyString()))
                .thenReturn("upload-123");

        mockMvc.perform(get("/api/article/check-upload/fileHash123")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("getUploadStatus - 成功场景")
    @WithMockUser
    void getUploadStatus_success() throws Exception {
        ChunkedUploadService.ChunkedUploadStatus status = new ChunkedUploadService.ChunkedUploadStatus(
                "upload-123", "test.jpg", 1024L, 10);

        when(chunkedUploadService.getUploadStatus(anyLong(), anyString()))
                .thenReturn(status);

        mockMvc.perform(get("/api/article/upload-status/upload-123")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    private String objectToJson(Object obj) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        return mapper.writeValueAsString(obj);
    }
}
