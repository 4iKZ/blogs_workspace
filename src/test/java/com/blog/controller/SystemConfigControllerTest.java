package com.blog.controller;

import com.blog.dto.SystemConfigDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 系统配置控制器测试类
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class SystemConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGetSystemConfig() throws Exception {
        mockMvc.perform(get("/api/system/config/{key}", "site_name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.configKey").value("site_name"));
    }

    @Test
    public void testGetSystemConfigNotFound() throws Exception {
        mockMvc.perform(get("/api/system/config/{key}", "non_existent_key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("配置项不存在"));
    }

    @Test
    public void testGetAllSystemConfigs() throws Exception {
        mockMvc.perform(get("/api/system/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    public void testGetSystemConfigsByType() throws Exception {
        mockMvc.perform(get("/api/system/config/type/{type}", "website"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    public void testGetSystemConfigsByTypeNotFound() throws Exception {
        mockMvc.perform(get("/api/system/config/type/{type}", "non_existent_type"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    public void testUpdateSystemConfig() throws Exception {
        // 创建配置DTO
        SystemConfigDTO configDTO = new SystemConfigDTO();
        configDTO.setConfigKey("site_name");
        configDTO.setConfigValue("新网站名称");
        configDTO.setConfigType("website");
        configDTO.setDescription("网站名称");

        String requestBody = objectMapper.writeValueAsString(configDTO);

        mockMvc.perform(put("/api/system/config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 验证配置已更新
        mockMvc.perform(get("/api/system/config/{key}", "site_name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.configValue").value("新网站名称"));
    }

    @Test
    public void testUpdateSystemConfigNotFound() throws Exception {
        // 创建不存在的配置DTO
        SystemConfigDTO configDTO = new SystemConfigDTO();
        configDTO.setConfigKey("non_existent_key");
        configDTO.setConfigValue("新值");
        configDTO.setConfigType("website");
        configDTO.setDescription("描述");

        String requestBody = objectMapper.writeValueAsString(configDTO);

        mockMvc.perform(put("/api/system/config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("配置项不存在"));
    }

    @Test
    public void testBatchUpdateSystemConfigs() throws Exception {
        // 创建多个配置DTO
        SystemConfigDTO config1 = new SystemConfigDTO();
        config1.setConfigKey("site_name");
        config1.setConfigValue("批量更新网站名称");
        config1.setConfigType("website");
        config1.setDescription("网站名称");

        SystemConfigDTO config2 = new SystemConfigDTO();
        config2.setConfigKey("site_description");
        config2.setConfigValue("批量更新网站描述");
        config2.setConfigType("website");
        config2.setDescription("网站描述");

        String requestBody = objectMapper.writeValueAsString(List.of(config1, config2));

        mockMvc.perform(put("/api/system/config/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 验证配置已更新
        mockMvc.perform(get("/api/system/config/{key}", "site_name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.configValue").value("批量更新网站名称"));

        mockMvc.perform(get("/api/system/config/{key}", "site_description"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.configValue").value("批量更新网站描述"));
    }

    @Test
    public void testGetWebsiteConfig() throws Exception {
        mockMvc.perform(get("/api/system/config/website"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    public void testUpdateWebsiteConfig() throws Exception {
        // 创建网站配置DTO
        SystemConfigDTO configDTO = new SystemConfigDTO();
        configDTO.setConfigKey("website_config");
        configDTO.setConfigValue("{\"siteName\":\"新网站\",\"siteDescription\":\"新描述\"}");
        configDTO.setConfigType("website");
        configDTO.setDescription("网站配置");

        String requestBody = objectMapper.writeValueAsString(configDTO);

        mockMvc.perform(put("/api/system/config/website")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    public void testGetEmailConfig() throws Exception {
        mockMvc.perform(get("/api/system/config/email"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    public void testUpdateEmailConfig() throws Exception {
        // 创建邮件配置DTO
        SystemConfigDTO configDTO = new SystemConfigDTO();
        configDTO.setConfigKey("email_config");
        configDTO.setConfigValue("{\"smtpHost\":\"smtp.new.com\",\"smtpPort\":587}");
        configDTO.setConfigType("email");
        configDTO.setDescription("邮件配置");

        String requestBody = objectMapper.writeValueAsString(configDTO);

        mockMvc.perform(put("/api/system/config/email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    public void testGetFileUploadConfig() throws Exception {
        mockMvc.perform(get("/api/system/config/file-upload"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    public void testUpdateFileUploadConfig() throws Exception {
        // 创建文件上传配置DTO
        SystemConfigDTO configDTO = new SystemConfigDTO();
        configDTO.setConfigKey("file_upload_config");
        configDTO.setConfigValue("{\"maxImageSize\":5242880,\"allowedImageTypes\":\"jpg,jpeg,png,gif\"}");
        configDTO.setConfigType("file_upload");
        configDTO.setDescription("文件上传配置");

        String requestBody = objectMapper.writeValueAsString(configDTO);

        mockMvc.perform(put("/api/system/config/file-upload")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}