package com.blog.controller;

import com.blog.common.Result;
import com.blog.dto.FileInfoDTO;
import com.blog.service.FileUploadService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 文件上传控制器测试类 - Mock 服务层
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "user")
public class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileUploadService fileUploadService;

    @Test
    public void testUploadImage() throws Exception {
        when(fileUploadService.uploadImage(any())).thenReturn(Result.success("https://mock.local/test.jpg"));

        MockMultipartFile imageFile = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "test image content".getBytes());

        mockMvc.perform(multipart("/api/file/upload/image").file(imageFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    public void testUploadImageWithInvalidFormat() throws Exception {
        when(fileUploadService.uploadImage(any())).thenReturn(Result.error("只允许上传图片文件"));

        MockMultipartFile invalidFile = new MockMultipartFile(
                "file", "test.exe", "application/x-msdownload", "executable content".getBytes());

        mockMvc.perform(multipart("/api/file/upload/image").file(invalidFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    public void testUploadImageWithLargeFile() throws Exception {
        when(fileUploadService.uploadImage(any())).thenReturn(Result.error("文件大小不能超过5MB"));

        byte[] largeContent = new byte[11 * 1024 * 1024];
        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "large.jpg", "image/jpeg", largeContent);

        mockMvc.perform(multipart("/api/file/upload/image").file(largeFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    public void testUploadFile() throws Exception {
        FileInfoDTO fileInfo = new FileInfoDTO();
        fileInfo.setFileName("document.pdf");
        fileInfo.setFileType("application/pdf");
        when(fileUploadService.uploadFile(any())).thenReturn(Result.success(fileInfo));

        MockMultipartFile attachmentFile = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", "test pdf content".getBytes());

        mockMvc.perform(multipart("/api/file/upload/file").file(attachmentFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    public void testBatchUploadFiles() throws Exception {
        FileInfoDTO file1 = new FileInfoDTO();
        file1.setFileName("batch1.jpg");
        FileInfoDTO file2 = new FileInfoDTO();
        file2.setFileName("batch2.jpg");
        when(fileUploadService.batchUploadFiles(any())).thenReturn(Result.success(List.of(file1, file2)));

        MockMultipartFile fileA = new MockMultipartFile(
                "files", "batch1.jpg", "image/jpeg", "batch file content 1".getBytes());
        MockMultipartFile fileB = new MockMultipartFile(
                "files", "batch2.jpg", "image/jpeg", "batch file content 2".getBytes());

        mockMvc.perform(multipart("/api/file/upload/batch").file(fileA).file(fileB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    public void testGetFileInfo() throws Exception {
        FileInfoDTO fileInfo = new FileInfoDTO();
        fileInfo.setFileName("info.jpg");
        fileInfo.setFileType("image/jpeg");
        when(fileUploadService.getFileById(1L)).thenReturn(Result.success(fileInfo));

        mockMvc.perform(get("/api/file/{fileId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    public void testGetFileInfoNotFound() throws Exception {
        when(fileUploadService.getFileById(99999L)).thenReturn(Result.error("文件不存在"));

        mockMvc.perform(get("/api/file/{fileId}", 99999))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    public void testDeleteFile() throws Exception {
        when(fileUploadService.deleteFile(1L)).thenReturn(Result.success());

        mockMvc.perform(delete("/api/file/{fileId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    public void testDeleteFileNotFound() throws Exception {
        when(fileUploadService.deleteFile(99999L)).thenReturn(Result.error("文件不存在"));

        mockMvc.perform(delete("/api/file/{fileId}", 99999))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    public void testGetFileList() throws Exception {
        FileInfoDTO file1 = new FileInfoDTO();
        file1.setFileName("user1.jpg");
        when(fileUploadService.getFileList(anyInt(), anyInt(), anyString())).thenReturn(Result.success(List.of(file1)));

        mockMvc.perform(get("/api/file/list")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    public void testCheckFileByMd5() throws Exception {
        FileInfoDTO fileInfo = new FileInfoDTO();
        fileInfo.setFileMd5("abc123");
        when(fileUploadService.checkFileExists("abc123")).thenReturn(Result.success(fileInfo));

        mockMvc.perform(get("/api/file/check/md5/{md5}", "abc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
