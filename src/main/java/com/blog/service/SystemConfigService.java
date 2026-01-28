package com.blog.service;

import com.blog.common.Result;
import com.blog.dto.SystemConfigDTO;
import com.blog.dto.WebsiteConfigDTO;
import com.blog.dto.EmailConfigDTO;
import com.blog.dto.FileUploadConfigDTO;

import java.util.List;

/**
 * 系统配置服务接口
 */
public interface SystemConfigService {

    /**
     * 获取系统配置
     * @param configKey 配置键
     * @return 系统配置信息
     */
    Result<SystemConfigDTO> getSystemConfig(String configKey);

    /**
     * 获取所有系统配置
     * @return 系统配置列表
     */
    Result<List<SystemConfigDTO>> getAllSystemConfigs();

    /**
     * 根据配置类型获取配置
     * @param configType 配置类型
     * @return 系统配置列表
     */
    Result<List<SystemConfigDTO>> getSystemConfigsByType(String configType);

    /**
     * 更新系统配置
     * @param systemConfigDTO 系统配置DTO
     * @return 操作结果
     */
    Result<Void> updateSystemConfig(SystemConfigDTO systemConfigDTO);

    /**
     * 批量更新系统配置
     * @param systemConfigDTOList 系统配置DTO列表
     * @return 操作结果
     */
    Result<Void> batchUpdateSystemConfigs(List<SystemConfigDTO> systemConfigDTOList);

    /**
     * 获取网站配置
     * @return 网站配置信息
     */
    Result<WebsiteConfigDTO> getWebsiteConfig();

    /**
     * 更新网站配置
     * @param websiteConfigDTO 网站配置DTO
     * @return 操作结果
     */
    Result<Void> updateWebsiteConfig(WebsiteConfigDTO websiteConfigDTO);

    /**
     * 获取邮件配置
     * @return 邮件配置信息
     */
    Result<EmailConfigDTO> getEmailConfig();

    /**
     * 更新邮件配置
     * @param emailConfigDTO 邮件配置DTO
     * @return 操作结果
     */
    Result<Void> updateEmailConfig(EmailConfigDTO emailConfigDTO);

    /**
     * 获取文件上传配置
     * @return 文件上传配置信息
     */
    Result<FileUploadConfigDTO> getFileUploadConfig();

    /**
     * 更新文件上传配置
     * @param fileUploadConfigDTO 文件上传配置DTO
     * @return 操作结果
     */
    Result<Void> updateFileUploadConfig(FileUploadConfigDTO fileUploadConfigDTO);
}