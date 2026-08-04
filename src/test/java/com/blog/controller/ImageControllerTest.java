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
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
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

    @Autowired
    private ImageController imageController;

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

    @Test
    @DisplayName("convertFormat - 服务返回错误时应处理")
    @WithMockUser
    void convertFormat_serviceError_shouldHandleGracefully() throws Exception {
        when(imageProcessingService.convertFormat(any(), anyString(), org.mockito.ArgumentMatchers.<Float>any()))
                .thenReturn(Result.error("转换失败"));

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());
        mockMvc.perform(multipart("/api/image/convert")
                .file(file)
                .param("format", "png")
                .param("quality", "0.8"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("convertFormat - 返回数据为null时应处理")
    @WithMockUser
    void convertFormat_nullData_shouldHandleGracefully() throws Exception {
        when(imageProcessingService.convertFormat(any(), anyString(), org.mockito.ArgumentMatchers.<Float>any()))
                .thenReturn(Result.success(null));

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());
        mockMvc.perform(multipart("/api/image/convert")
                .file(file)
                .param("format", "png")
                .param("quality", "0.8"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("convertAndDownload - 服务返回错误时应返回400")
    @WithMockUser
    void convertAndDownload_serviceError_shouldReturnBadRequest() throws Exception {
        when(imageProcessingService.convertFormat(any(), anyString(), org.mockito.ArgumentMatchers.<Float>any()))
                .thenReturn(Result.error("转换失败"));

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());
        mockMvc.perform(multipart("/api/image/convert/download")
                .file(file)
                .param("format", "png")
                .param("quality", "0.8"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("convertAndDownload - 返回数据为null时应返回400")
    @WithMockUser
    void convertAndDownload_nullData_shouldReturnBadRequest() throws Exception {
        when(imageProcessingService.convertFormat(any(), anyString(), org.mockito.ArgumentMatchers.<Float>any()))
                .thenReturn(Result.success(null));

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());
        mockMvc.perform(multipart("/api/image/convert/download")
                .file(file)
                .param("format", "png")
                .param("quality", "0.8"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("convertAndDownload - 文件名无扩展名时应使用默认名")
    @WithMockUser
    void convertAndDownload_noExtension_shouldUseDefaultName() throws Exception {
        ImageConvertDTO convertDTO = new ImageConvertDTO();
        convertDTO.setOriginalSize(1024L);
        convertDTO.setConvertedSize(512L);
        convertDTO.setCompressionRatio(50.0);
        convertDTO.setMimeType("image/png");

        when(imageProcessingService.convertFormat(any(), anyString(), org.mockito.ArgumentMatchers.<Float>any()))
                .thenReturn(Result.success(convertDTO));

        MockMultipartFile file = new MockMultipartFile("file", "no_extension", "image/jpeg", "test".getBytes());
        mockMvc.perform(multipart("/api/image/convert/download")
                .file(file)
                .param("format", "png")
                .param("quality", "0.8"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("convertAndDownload - 文件名为null时应使用默认名")
    @WithMockUser
    void convertAndDownload_nullFileName_shouldUseDefaultName() throws Exception {
        ImageConvertDTO convertDTO = new ImageConvertDTO();
        convertDTO.setOriginalSize(1024L);
        convertDTO.setConvertedSize(512L);
        convertDTO.setCompressionRatio(50.0);
        convertDTO.setMimeType("image/png");

        when(imageProcessingService.convertFormat(any(), anyString(), org.mockito.ArgumentMatchers.<Float>any()))
                .thenReturn(Result.success(convertDTO));

        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn(null);

        ResponseEntity<byte[]> response = imageController.convertAndDownload(file, "png", 0.8f);

        int statusCode = response.getStatusCodeValue();
        org.springframework.http.ContentDisposition disposition = response.getHeaders().getContentDisposition();
        String filename = disposition != null ? disposition.getFilename() : null;
        if (statusCode != 200 || !"image.png".equals(filename)) {
            throw new AssertionError("Expected OK with default filename, got status=" + statusCode + ", filename=" + filename);
        }
    }

    @Test
    @DisplayName("convertAndDownload - 服务抛出异常时应返回500")
    @WithMockUser
    void convertAndDownload_serviceException_shouldReturnError() throws Exception {
        when(imageProcessingService.convertFormat(any(), anyString(), org.mockito.ArgumentMatchers.<Float>any()))
                .thenThrow(new RuntimeException("Conversion failed"));

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());
        mockMvc.perform(multipart("/api/image/convert/download")
                .file(file)
                .param("format", "png")
                .param("quality", "0.8"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("compressImage - 服务抛出异常时应返回500")
    @WithMockUser
    void compressImage_serviceException_shouldReturnError() throws Exception {
        when(imageProcessingService.compressImage(any(), anyInt(), anyInt(), org.mockito.ArgumentMatchers.<Float>any()))
                .thenThrow(new RuntimeException("Compression failed"));

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());
        mockMvc.perform(multipart("/api/image/compress")
                .file(file)
                .param("maxWidth", "1024")
                .param("maxHeight", "1024")
                .param("quality", "0.8"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("compressImage - 服务返回错误时应返回400")
    @WithMockUser
    void compressImage_serviceError_shouldReturnBadRequest() throws Exception {
        when(imageProcessingService.compressImage(any(), anyInt(), anyInt(), org.mockito.ArgumentMatchers.<Float>any()))
                .thenReturn(Result.error("压缩失败"));

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());
        mockMvc.perform(multipart("/api/image/compress")
                .file(file)
                .param("maxWidth", "1024")
                .param("maxHeight", "1024")
                .param("quality", "0.8"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("compressImage - 返回数据为null时应返回400")
    @WithMockUser
    void compressImage_nullData_shouldReturnBadRequest() throws Exception {
        when(imageProcessingService.compressImage(any(), anyInt(), anyInt(), org.mockito.ArgumentMatchers.<Float>any()))
                .thenReturn(Result.success(null));

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());
        mockMvc.perform(multipart("/api/image/compress")
                .file(file)
                .param("maxWidth", "1024")
                .param("maxHeight", "1024")
                .param("quality", "0.8"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("compressImage - 文件名为null时应使用默认名")
    @WithMockUser
    void compressImage_nullFileName_shouldUseDefaultName() throws Exception {
        when(imageProcessingService.compressImage(any(), anyInt(), anyInt(), org.mockito.ArgumentMatchers.<Float>any()))
                .thenReturn(Result.success("compressed".getBytes()));

        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn(null);

        ResponseEntity<byte[]> response = imageController.compressImage(file, 1024, 1024, 0.8f);

        int statusCode = response.getStatusCodeValue();
        org.springframework.http.ContentDisposition disposition = response.getHeaders().getContentDisposition();
        String filename = disposition != null ? disposition.getFilename() : null;
        if (statusCode != 200 || !"compressed_image.jpg".equals(filename)) {
            throw new AssertionError("Expected OK with default filename, got status=" + statusCode + ", filename=" + filename);
        }
    }
}
