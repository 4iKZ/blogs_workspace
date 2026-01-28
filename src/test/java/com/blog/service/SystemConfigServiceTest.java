package com.blog.service;

import com.blog.dto.SystemConfigDTO;
import com.blog.dto.WebsiteConfigDTO;
import com.blog.dto.EmailConfigDTO;
import com.blog.dto.FileUploadConfigDTO;
import com.blog.common.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 系统配置服务测试类
 */
@SpringBootTest
@Transactional
public class SystemConfigServiceTest {

    @Autowired
    private SystemConfigService systemConfigService;

    @Test
    public void testGetSystemConfig() {
        // 获取存在的配置项
        Result<SystemConfigDTO> result = systemConfigService.getSystemConfig("site_name");
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("site_name", result.getData().getConfigKey());
    }

    @Test
    public void testGetSystemConfigNotFound() {
        // 获取不存在的配置项
        Result<SystemConfigDTO> result = systemConfigService.getSystemConfig("non_existent_key");
        
        assertFalse(result.isSuccess());
        assertEquals("配置项不存在", result.getMessage());
    }

    @Test
    public void testGetAllSystemConfigs() {
        // 获取所有配置项
        Result<List<SystemConfigDTO>> result = systemConfigService.getAllSystemConfigs();
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertTrue(result.getData().size() > 0);
    }

    @Test
    public void testGetSystemConfigsByType() {
        // 获取网站配置
        Result<List<SystemConfigDTO>> result = systemConfigService.getSystemConfigsByType("website");
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertTrue(result.getData().size() > 0);
        
        // 验证所有配置项类型为website
        for (SystemConfigDTO config : result.getData()) {
            assertEquals("website", config.getConfigType());
        }
    }

    @Test
    public void testGetSystemConfigsByTypeNotFound() {
        // 获取不存在的配置类型
        Result<List<SystemConfigDTO>> result = systemConfigService.getSystemConfigsByType("non_existent_type");
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(0, result.getData().size());
    }

    @Test
    public void testUpdateSystemConfig() {
        // 创建配置DTO
        SystemConfigDTO configDTO = new SystemConfigDTO();
        configDTO.setConfigKey("site_name");
        configDTO.setConfigValue("新网站名称");
        configDTO.setConfigType("website");
        configDTO.setDescription("网站名称");

        // 更新配置
        Result<Void> result = systemConfigService.updateSystemConfig(configDTO);
        
        assertTrue(result.isSuccess());

        // 验证配置已更新
        Result<SystemConfigDTO> getResult = systemConfigService.getSystemConfig("site_name");
        assertEquals("新网站名称", getResult.getData().getConfigValue());
    }

    @Test
    public void testUpdateSystemConfigNotFound() {
        // 创建不存在的配置DTO
        SystemConfigDTO configDTO = new SystemConfigDTO();
        configDTO.setConfigKey("non_existent_key");
        configDTO.setConfigValue("新值");
        configDTO.setConfigType("website");
        configDTO.setDescription("描述");

        // 更新配置
        Result<Void> result = systemConfigService.updateSystemConfig(configDTO);
        
        assertFalse(result.isSuccess());
        assertEquals("配置项不存在", result.getMessage());
    }

    @Test
    public void testBatchUpdateSystemConfigs() {
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

        List<SystemConfigDTO> configs = List.of(config1, config2);

        // 批量更新配置
        Result<Void> result = systemConfigService.batchUpdateSystemConfigs(configs);
        
        assertTrue(result.isSuccess());

        // 验证配置已更新
        Result<SystemConfigDTO> getResult1 = systemConfigService.getSystemConfig("site_name");
        assertEquals("批量更新网站名称", getResult1.getData().getConfigValue());

        Result<SystemConfigDTO> getResult2 = systemConfigService.getSystemConfig("site_description");
        assertEquals("批量更新网站描述", getResult2.getData().getConfigValue());
    }

    @Test
    public void testGetWebsiteConfig() {
        // 获取网站配置
        Result<WebsiteConfigDTO> result = systemConfigService.getWebsiteConfig();
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }

    @Test
    public void testUpdateWebsiteConfig() {
        // 创建网站配置DTO
        WebsiteConfigDTO configDTO = new WebsiteConfigDTO();
        configDTO.setWebsiteName("新网站");
        configDTO.setWebsiteDescription("新描述");

        // 更新网站配置
        Result<Void> result = systemConfigService.updateWebsiteConfig(configDTO);
        
        assertTrue(result.isSuccess());
    }

    @Test
    public void testGetEmailConfig() {
        // 获取邮件配置
        Result<EmailConfigDTO> result = systemConfigService.getEmailConfig();
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }

    @Test
    public void testUpdateEmailConfig() {
        // 创建邮件配置DTO
        EmailConfigDTO configDTO = new EmailConfigDTO();
        configDTO.setSmtpHost("smtp.new.com");
        configDTO.setSmtpPort(587);

        // 更新邮件配置
        Result<Void> result = systemConfigService.updateEmailConfig(configDTO);
        
        assertTrue(result.isSuccess());
    }

    @Test
    public void testGetFileUploadConfig() {
        // 获取文件上传配置
        Result<FileUploadConfigDTO> result = systemConfigService.getFileUploadConfig();
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }

    @Test
    public void testUpdateFileUploadConfig() {
        // 创建文件上传配置DTO
        FileUploadConfigDTO configDTO = new FileUploadConfigDTO();
        configDTO.setMaxFileSize(5); // 5MB
        configDTO.setAllowedImageTypes("jpg,jpeg,png,gif");

        // 更新文件上传配置
        Result<Void> result = systemConfigService.updateFileUploadConfig(configDTO);
        
        assertTrue(result.isSuccess());
    }
}