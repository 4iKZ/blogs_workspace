package com.blog.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 文件上传控制器测试类
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testUploadImage() throws Exception {
        // 创建模拟图片文件
        MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        mockMvc.perform(multipart("/api/file/upload/image")
                .file(imageFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("上传成功"))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    public void testUploadImageWithInvalidFormat() throws Exception {
        // 创建不支持的文件格式
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "test.exe",
                "application/x-msdownload",
                "executable content".getBytes()
        );

        mockMvc.perform(multipart("/api/file/upload/image")
                .file(invalidFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("不支持的图片格式"));
    }

    @Test
    public void testUploadImageWithLargeFile() throws Exception {
        // 创建大文件（超过10MB限制）
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "large.jpg",
                "image/jpeg",
                largeContent
        );

        mockMvc.perform(multipart("/api/file/upload/image")
                .file(largeFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("文件大小超过限制"));
    }

    @Test
    public void testUploadFile() throws Exception {
        // 创建模拟文件
        MockMultipartFile attachmentFile = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "test pdf content".getBytes()
        );

        mockMvc.perform(multipart("/api/file/upload/file")
                .file(attachmentFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("上传成功"))
                .andExpect(jsonPath("$.data.filename").value("document.pdf"))
                .andExpect(jsonPath("$.data.fileType").value("application/pdf"))
                .andExpect(jsonPath("$.data.url").exists());
    }

    @Test
    public void testBatchUploadFiles() throws Exception {
        // 创建多个模拟文件
        List<MockMultipartFile> files = new ArrayList<>();
        for (int i = 1; i <= 2; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "files",
                    "batch" + i + ".jpg",
                    "image/jpeg",
                    ("batch file content " + i).getBytes()
            );
            files.add(file);
        }

        mockMvc.perform(multipart("/api/file/upload/batch")
                .file(files.get(0))
                .file(files.get(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("上传成功"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    public void testGetFileInfo() throws Exception {
        // 先上传文件
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "info.jpg",
                "image/jpeg",
                "file info test content".getBytes()
        );

        String response = mockMvc.perform(multipart("/api/file/upload/image")
                .file(file))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 解析JSON获取文件ID
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response);
        Long fileId = root.path("data").path("id").asLong();

        mockMvc.perform(get("/api/file/{fileId}", fileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.filename").value("info.jpg"))
                .andExpect(jsonPath("$.data.fileType").value("image/jpeg"));
    }

    @Test
    public void testGetFileInfoNotFound() throws Exception {
        mockMvc.perform(get("/api/file/{fileId}", 99999))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("文件不存在"));
    }

    @Test
    public void testDeleteFile() throws Exception {
        // 先上传文件
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "delete.jpg",
                "image/jpeg",
                "delete test content".getBytes()
        );

        String response = mockMvc.perform(multipart("/api/file/upload/image")
                .file(file))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 解析JSON获取文件ID
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response);
        Long fileId = root.path("data").path("id").asLong();

        mockMvc.perform(delete("/api/file/{fileId}", fileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("文件删除成功"));
    }

    @Test
    public void testDeleteFileNotFound() throws Exception {
        mockMvc.perform(delete("/api/file/{fileId}", 99999))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("文件不存在"));
    }

    @Test
    public void testGetFileList() throws Exception {
        // 上传多个文件
        for (int i = 1; i <= 3; i++) {
            MockMultipartFile imageFile = new MockMultipartFile(
                    "file",
                    "user" + i + ".jpg",
                    "image/jpeg",
                    ("user test content " + i).getBytes()
            );
            mockMvc.perform(multipart("/api/file/upload/image").file(imageFile));
        }

        mockMvc.perform(get("/api/file/list")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    public void testCheckFileByMd5() throws Exception {
        // 创建模拟文件
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "md5test.jpg",
                "image/jpeg",
                "md5 test content".getBytes()
        );

        // 先上传文件，获取返回的文件信息
        String response = mockMvc.perform(multipart("/api/file/upload/image")
                .file(file))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 解析JSON获取文件MD5
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response);
        String md5 = root.path("data").path("md5").asText();

        // 检查文件是否存在
        mockMvc.perform(get("/api/file/check/md5/{md5}", md5))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.exist").value(true));
    }
}