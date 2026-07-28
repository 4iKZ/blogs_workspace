package com.blog.service;

import com.blog.BlogBackendApplication;
import com.blog.dto.FileInfoDTO;
import com.blog.common.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文件上传服务测试类
 */
@SpringBootTest(classes = BlogBackendApplication.class)
@ActiveProfiles("test")
@Transactional
public class FileUploadServiceTest {

    @Autowired
    private FileUploadService fileUploadService;

    @org.junit.jupiter.api.BeforeEach
    void setUpRequestContext() {
        HttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setAttribute("userId", 1L);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @org.junit.jupiter.api.AfterEach
    void tearDownRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    private static byte[] createValidJpegBytes() throws IOException {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", bos);
        return bos.toByteArray();
    }

    @Test
    public void testUploadImage() throws IOException {
        byte[] jpegBytes = createValidJpegBytes();
        MultipartFile imageFile = new MockMultipartFile(
                "test.jpg", "test.jpg", "image/jpeg", jpegBytes);

        Result<String> result = fileUploadService.uploadImage(imageFile);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }

    @Test
    public void testUploadImageWithInvalidFormat() {
        MultipartFile invalidFile = new MockMultipartFile(
                "test.exe", "test.exe", "application/x-msdownload",
                "executable content".getBytes());

        Result<String> result = fileUploadService.uploadImage(invalidFile);

        assertFalse(result.isSuccess());
        assertNotNull(result.getMessage());
    }

    @Test
    public void testUploadImageWithLargeFile() {
        byte[] largeContent = new byte[11 * 1024 * 1024];
        MultipartFile largeFile = new MockMultipartFile(
                "large.jpg", "large.jpg", "image/jpeg", largeContent);

        Result<String> result = fileUploadService.uploadImage(largeFile);

        assertFalse(result.isSuccess());
        assertNotNull(result.getMessage());
    }

    @Test
    public void testUploadFile() throws IOException {
        MultipartFile file = new MockMultipartFile(
                "document.pdf", "document.pdf", "application/pdf",
                "test pdf content".getBytes());

        Result<FileInfoDTO> result = fileUploadService.uploadFile(file);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertTrue(result.getData().getFileName().endsWith(".pdf"));
        assertTrue(result.getData().getFileSize() > 0);
        assertNotNull(result.getData().getFileUrl());
    }

    @Test
    public void testUploadFileWithEmptyFile() {
        MultipartFile emptyFile = new MockMultipartFile(
                "empty.pdf", "empty.pdf", "application/pdf", new byte[0]);

        Result<FileInfoDTO> result = fileUploadService.uploadFile(emptyFile);

        assertFalse(result.isSuccess());
        assertNotNull(result.getMessage());
    }

    @Test
    public void testGetFileById() throws IOException {
        MultipartFile file = new MockMultipartFile(
                "info.jpg", "info.jpg", "image/jpeg",
                "file info test content".getBytes());

        Result<FileInfoDTO> uploadResult = fileUploadService.uploadFile(file);
        Long fileId = uploadResult.getData().getId();

        Result<FileInfoDTO> result = fileUploadService.getFileById(fileId);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertTrue(result.getData().getFileName().endsWith(".jpg"));
    }

    @Test
    public void testGetFileByIdNotFound() {
        Result<FileInfoDTO> result = fileUploadService.getFileById(99999L);

        assertFalse(result.isSuccess());
        assertNotNull(result.getMessage());
    }

    @Test
    public void testDeleteFile() throws IOException {
        MultipartFile file = new MockMultipartFile(
                "delete.jpg", "delete.jpg", "image/jpeg",
                "delete test content".getBytes());

        Result<FileInfoDTO> uploadResult = fileUploadService.uploadFile(file);
        Long fileId = uploadResult.getData().getId();

        Result<Void> result = fileUploadService.deleteFile(fileId);

        assertTrue(result.isSuccess());
    }

    @Test
    public void testDeleteFileNotFound() {
        Result<Void> result = fileUploadService.deleteFile(99999L);

        assertFalse(result.isSuccess());
        assertNotNull(result.getMessage());
    }



    @Test
    public void testGetFileList() throws IOException {
        for (int i = 1; i <= 3; i++) {
            MultipartFile file = new MockMultipartFile(
                    "user" + i + ".jpg", "user" + i + ".jpg", "image/jpeg",
                    ("user test content " + i).getBytes());
            fileUploadService.uploadFile(file);
        }

        Result<List<FileInfoDTO>> result = fileUploadService.getFileList(1, 10, "image/jpeg");

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }
}
