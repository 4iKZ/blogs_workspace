package com.blog.controller;

import com.blog.common.Result;
import com.blog.dto.ImageConvertDTO;
import com.blog.service.ImageProcessingService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImageProcessingService imageProcessingService;

    @Test
    @DisplayName("convertFormat - 成功场景")
    @WithMockUser
    void convertFormat_success() throws Exception {
        ImageConvertDTO convertDTO = new ImageConvertDTO();
        convertDTO.setOriginalSize(1024L);
        convertDTO.setConvertedSize(512L);
        convertDTO.setCompressionRatio(50.0);
        convertDTO.setMimeType("image/png");

        when(imageProcessingService.convertFormat(any(), anyString(), org.mockito.ArgumentMatchers.<Float>any()))
                .thenReturn(Result.success(convertDTO));

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());
        mockMvc.perform(multipart("/api/image/convert")
                .file(file)
                .param("format", "png")
                .param("quality", "0.8"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("convertAndDownload - 成功场景")
    @WithMockUser
    void convertAndDownload_success() throws Exception {
        ImageConvertDTO convertDTO = new ImageConvertDTO();
        convertDTO.setOriginalSize(1024L);
        convertDTO.setConvertedSize(512L);
        convertDTO.setCompressionRatio(Double.valueOf(50.0));
        convertDTO.setMimeType("image/png");

        when(imageProcessingService.convertFormat(any(), anyString(), org.mockito.ArgumentMatchers.<Float>any()))
                .thenReturn(Result.success(convertDTO));

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());
        mockMvc.perform(multipart("/api/image/convert/download")
                .file(file)
                .param("format", "png")
                .param("quality", "0.8"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("compressImage - 成功场景")
    @WithMockUser
    void compressImage_success() throws Exception {
        when(imageProcessingService.compressImage(any(), anyInt(), anyInt(), org.mockito.ArgumentMatchers.<Float>any()))
                .thenReturn(Result.success("compressed".getBytes()));

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());
        mockMvc.perform(multipart("/api/image/compress")
                .file(file)
                .param("maxWidth", "1024")
                .param("maxHeight", "1024")
                .param("quality", "0.8"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("batchConvertFormat - 成功场景")
    @WithMockUser
    void batchConvertFormat_success() throws Exception {
        ImageConvertDTO convertDTO = new ImageConvertDTO();
        convertDTO.setOriginalSize(1024L);
        convertDTO.setConvertedSize(512L);
        convertDTO.setCompressionRatio(50.0);
        convertDTO.setMimeType("image/png");

        when(imageProcessingService.convertFormat(any(), anyString(), org.mockito.ArgumentMatchers.<Float>any()))
                .thenReturn(Result.success(convertDTO));

        MockMultipartFile file = new MockMultipartFile("files", "test.jpg", "image/jpeg", "test".getBytes());
        mockMvc.perform(multipart("/api/image/batch-convert")
                .file(file)
                .param("format", "png")
                .param("quality", "0.8"))
                .andExpect(status().isOk());
    }
}
