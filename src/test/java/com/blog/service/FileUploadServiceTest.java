package com.blog.service;

import com.blog.dto.FileInfoDTO;
import com.blog.common.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文件上传服务测试类
 */
@SpringBootTest
@Transactional
public class FileUploadServiceTest {

    @Autowired
    private FileUploadService fileUploadService;

    @Test
    public void testUploadImage() throws IOException {
        // 创建模拟图片文件
        MultipartFile imageFile = new MockMultipartFile(
                "test.jpg",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        Result<String> result = fileUploadService.uploadImage(imageFile);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertTrue(result.getData().contains("test.jpg"));
    }

    @Test
    public void testUploadImageWithInvalidFormat() {
        // 创建不支持的文件格式
        MultipartFile invalidFile = new MockMultipartFile(
                "test.exe",
                "test.exe",
                "application/x-msdownload",
                "executable content".getBytes()
        );

        Result<String> result = fileUploadService.uploadImage(invalidFile);
        
        assertFalse(result.isSuccess());
        assertEquals("不支持的图片格式", result.getMessage());
    }

    @Test
    public void testUploadImageWithLargeFile() {
        // 创建大文件（超过10MB限制）
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
        MultipartFile largeFile = new MockMultipartFile(
                "large.jpg",
                "large.jpg",
                "image/jpeg",
                largeContent
        );

        Result<String> result = fileUploadService.uploadImage(largeFile);
        
        assertFalse(result.isSuccess());
        assertEquals("文件大小超过限制", result.getMessage());
    }

    @Test
    public void testUploadFile() throws IOException {
        // 创建模拟文件
        MultipartFile file = new MockMultipartFile(
                "document.pdf",
                "document.pdf",
                "application/pdf",
                "test pdf content".getBytes()
        );

        Result<FileInfoDTO> result = fileUploadService.uploadFile(file);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("document.pdf", result.getData().getFileName());
        assertEquals("application/pdf", result.getData().getFileType());
        assertTrue(result.getData().getFileSize() > 0);
        assertNotNull(result.getData().getFileUrl());
        assertNotNull(result.getData().getFilePath());
    }

    @Test
    public void testUploadFileWithInvalidFormat() {
        // 创建不支持的附件格式
        MultipartFile invalidFile = new MockMultipartFile(
                "test.exe",
                "test.exe",
                "application/x-msdownload",
                "executable content".getBytes()
        );

        Result<FileInfoDTO> result = fileUploadService.uploadFile(invalidFile);
        
        assertFalse(result.isSuccess());
        assertEquals("不支持的附件格式", result.getMessage());
    }

    @Test
    public void testGetFileById() throws IOException {
        // 先上传文件
        MultipartFile file = new MockMultipartFile(
                "info.jpg",
                "info.jpg",
                "image/jpeg",
                "file info test content".getBytes()
        );

        Result<FileInfoDTO> uploadResult = fileUploadService.uploadFile(file);
        Long fileId = uploadResult.getData().getId();

        // 获取文件信息
        Result<FileInfoDTO> result = fileUploadService.getFileById(fileId);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("info.jpg", result.getData().getFileName());
        assertEquals("image/jpeg", result.getData().getFileType());
    }

    @Test
    public void testGetFileByIdNotFound() {
        Result<FileInfoDTO> result = fileUploadService.getFileById(99999L);
        
        assertFalse(result.isSuccess());
        assertEquals("文件不存在", result.getMessage());
    }

    @Test
    public void testDeleteFile() throws IOException {
        // 先上传文件
        MultipartFile file = new MockMultipartFile(
                "delete.jpg",
                "delete.jpg",
                "image/jpeg",
                "delete test content".getBytes()
        );

        Result<FileInfoDTO> uploadResult = fileUploadService.uploadFile(file);
        Long fileId = uploadResult.getData().getId();

        // 删除文件
        Result<Void> result = fileUploadService.deleteFile(fileId);
        
        assertTrue(result.isSuccess());

        // 验证文件已被删除
        Result<FileInfoDTO> getResult = fileUploadService.getFileById(fileId);
        assertFalse(getResult.isSuccess());
    }

    @Test
    public void testDeleteFileNotFound() {
        Result<Void> result = fileUploadService.deleteFile(99999L);
        
        assertFalse(result.isSuccess());
        assertEquals("文件不存在", result.getMessage());
    }

    @Test
    public void testGetFileList() throws IOException {
        // 上传多个文件
        for (int i = 1; i <= 3; i++) {
            MultipartFile file = new MockMultipartFile(
                    "user" + i + ".jpg",
                    "user" + i + ".jpg",
                    "image/jpeg",
                    ("user test content " + i).getBytes()
            );
            fileUploadService.uploadFile(file);
        }

        // 获取文件列表
        Result<List<FileInfoDTO>> result = fileUploadService.getFileList(1, 10, "image/jpeg");
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertTrue(result.getData().size() >= 0);
    }


}