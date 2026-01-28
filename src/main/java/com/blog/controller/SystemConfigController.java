package com.blog.controller;

import com.blog.common.Result;
import com.blog.dto.*;
import com.blog.service.SystemConfigService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 系统配置控制器
 */
@RestController
@RequestMapping("/api/system/config")
@Tag(name = "系统配置管理")
public class SystemConfigController {

    @Autowired
    private SystemConfigService systemConfigService;

    @GetMapping("/{configKey}")
    @Operation(summary = "获取系统配置")
    public Result<SystemConfigDTO> getSystemConfig(
            @Parameter(description = "配置键") @PathVariable String configKey) {
        return systemConfigService.getSystemConfig(configKey);
    }

    @GetMapping("/all")
    @Operation(summary = "获取所有系统配置")
    public Result<List<SystemConfigDTO>> getAllSystemConfigs() {
        return systemConfigService.getAllSystemConfigs();
    }

    @GetMapping("/type/{configType}")
    @Operation(summary = "根据配置类型获取配置")
    public Result<List<SystemConfigDTO>> getSystemConfigsByType(
            @Parameter(description = "配置类型") @PathVariable String configType) {
        return systemConfigService.getSystemConfigsByType(configType);
    }

    @PutMapping
    @Operation(summary = "更新系统配置")
    public Result<Void> updateSystemConfig(
            @Parameter(description = "系统配置信息") @Valid @RequestBody SystemConfigDTO systemConfigDTO) {
        return systemConfigService.updateSystemConfig(systemConfigDTO);
    }

    @PutMapping("/batch")
    @Operation(summary = "批量更新系统配置")
    public Result<Void> batchUpdateSystemConfigs(
            @Parameter(description = "系统配置列表") @Valid @RequestBody List<SystemConfigDTO> systemConfigDTOList) {
        return systemConfigService.batchUpdateSystemConfigs(systemConfigDTOList);
    }

    @GetMapping("/website")
    @Operation(summary = "获取网站配置")
    public Result<WebsiteConfigDTO> getWebsiteConfig() {
        return systemConfigService.getWebsiteConfig();
    }

    @PutMapping("/website")
    @Operation(summary = "更新网站配置")
    public Result<Void> updateWebsiteConfig(
            @Parameter(description = "网站配置信息") @Valid @RequestBody WebsiteConfigDTO websiteConfigDTO) {
        return systemConfigService.updateWebsiteConfig(websiteConfigDTO);
    }

    @GetMapping("/email")
    @Operation(summary = "获取邮件配置")
    public Result<EmailConfigDTO> getEmailConfig() {
        return systemConfigService.getEmailConfig();
    }

    @PutMapping("/email")
    @Operation(summary = "更新邮件配置")
    public Result<Void> updateEmailConfig(
            @Parameter(description = "邮件配置信息") @Valid @RequestBody EmailConfigDTO emailConfigDTO) {
        return systemConfigService.updateEmailConfig(emailConfigDTO);
    }

    @GetMapping("/file-upload")
    @Operation(summary = "获取文件上传配置")
    public Result<FileUploadConfigDTO> getFileUploadConfig() {
        return systemConfigService.getFileUploadConfig();
    }

    @PutMapping("/file-upload")
    @Operation(summary = "更新文件上传配置")
    public Result<Void> updateFileUploadConfig(
            @Parameter(description = "文件上传配置信息") @Valid @RequestBody FileUploadConfigDTO fileUploadConfigDTO) {
        return systemConfigService.updateFileUploadConfig(fileUploadConfigDTO);
    }
}