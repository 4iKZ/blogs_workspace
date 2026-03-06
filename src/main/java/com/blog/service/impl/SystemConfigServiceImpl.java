package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.ResultCode;
import com.blog.common.Result;
import com.blog.dto.SystemConfigDTO;
import com.blog.dto.WebsiteConfigDTO;
import com.blog.dto.EmailConfigDTO;
import com.blog.dto.FileUploadConfigDTO;
import com.blog.entity.SystemConfig;
import com.blog.mapper.SystemConfigMapper;
import com.blog.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 系统配置服务实现类
 */
@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    private static final Logger log = LoggerFactory.getLogger(SystemConfigServiceImpl.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String CFG_SITE_NAME = "site_name";
    private static final String CFG_SITE_DESCRIPTION = "site_description";
    private static final String CFG_SITE_KEYWORDS = "site_keywords";
    private static final String CFG_SITE_LOGO = "site_logo";
    private static final String CFG_SITE_FAVICON = "site_favicon";
    private static final String CFG_SITE_ICP = "site_icp";
    private static final String CFG_SITE_ANALYTICS = "site_analytics";
    private static final String CFG_SITE_STATUS = "site_status";
    private static final String CFG_CLOSE_MESSAGE = "close_message";
    private static final String CFG_ARTICLES_PER_PAGE = "articles_per_page";
    private static final String CFG_RSS_LIMIT = "rss_limit";
    private static final String CFG_ALLOW_COMMENT = "allow_comment";
    private static final String CFG_ALLOW_REGISTER = "allow_register";

    private static final String CFG_SMTP_HOST = "smtp_host";
    private static final String CFG_SMTP_PORT = "smtp_port";
    private static final String CFG_SMTP_USERNAME = "smtp_username";
    private static final String CFG_SMTP_PASSWORD = "smtp_password";
    private static final String CFG_SMTP_ENABLE_SSL = "smtp_enable_ssl";
    private static final String CFG_FROM_EMAIL = "from_email";
    private static final String CFG_FROM_NAME = "from_name";
    private static final String CFG_EMAIL_ENABLED = "email_enabled";

    private static final String CFG_MAX_FILE_SIZE = "max_file_size";
    private static final String CFG_ALLOWED_IMAGE_TYPES = "upload_allowed_types";
    private static final String CFG_ALLOWED_FILE_TYPES = "upload_allowed_file_types";
    private static final String CFG_IMAGE_UPLOAD_PATH = "image_upload_path";
    private static final String CFG_FILE_UPLOAD_PATH = "file_upload_path";
    private static final String CFG_ENABLE_LOCAL_STORAGE = "upload_local_enabled";
    private static final String CFG_ENABLE_OSS_STORAGE = "upload_oss_enabled";
    private static final String CFG_OSS_ACCESS_KEY = "oss_access_key";
    private static final String CFG_OSS_SECRET_KEY = "oss_secret_key";
    private static final String CFG_OSS_BUCKET_NAME = "oss_bucket_name";
    private static final String CFG_OSS_ENDPOINT = "oss_endpoint";

    private static final Set<String> WEBSITE_KEYS = Set.of(
            CFG_SITE_NAME,
            CFG_SITE_DESCRIPTION,
            CFG_SITE_KEYWORDS,
            CFG_SITE_LOGO,
            CFG_SITE_FAVICON,
            CFG_SITE_ICP,
            CFG_SITE_ANALYTICS,
            CFG_SITE_STATUS,
            CFG_CLOSE_MESSAGE,
            CFG_ARTICLES_PER_PAGE,
            CFG_RSS_LIMIT,
            CFG_ALLOW_COMMENT,
            CFG_ALLOW_REGISTER
    );

    private static final Set<String> EMAIL_KEYS = Set.of(
            CFG_SMTP_HOST,
            CFG_SMTP_PORT,
            CFG_SMTP_USERNAME,
            CFG_SMTP_PASSWORD,
            CFG_SMTP_ENABLE_SSL,
            CFG_FROM_EMAIL,
            CFG_FROM_NAME,
            CFG_EMAIL_ENABLED
    );

    private static final Set<String> FILE_UPLOAD_KEYS = Set.of(
            CFG_MAX_FILE_SIZE,
            CFG_ALLOWED_IMAGE_TYPES,
            CFG_ALLOWED_FILE_TYPES,
            CFG_IMAGE_UPLOAD_PATH,
            CFG_FILE_UPLOAD_PATH,
            CFG_ENABLE_LOCAL_STORAGE,
            CFG_ENABLE_OSS_STORAGE,
            CFG_OSS_ACCESS_KEY,
            CFG_OSS_SECRET_KEY,
            CFG_OSS_BUCKET_NAME,
            CFG_OSS_ENDPOINT
    );

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Override
    public Result<SystemConfigDTO> getSystemConfig(String configKey) {
        log.info("获取系统配置，配置键：{}", configKey);

        if (!StringUtils.hasText(configKey)) {
            return Result.error(ResultCode.BAD_REQUEST, "配置键不能为空");
        }

        SystemConfig config = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, configKey));

        if (config == null) {
            return Result.error(ResultCode.CONFIG_NOT_FOUND, "配置不存在: " + configKey);
        }

        return Result.success(toDTO(config));
    }

    @Override
    public Result<List<SystemConfigDTO>> getAllSystemConfigs() {
        log.info("获取所有系统配置");

        List<SystemConfig> configList = systemConfigMapper.selectList(new LambdaQueryWrapper<SystemConfig>()
                .orderByAsc(SystemConfig::getConfigKey));

        List<SystemConfigDTO> result = configList.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return Result.success(result);
    }

    @Override
    public Result<List<SystemConfigDTO>> getSystemConfigsByType(String configType) {
        log.info("根据配置类型获取配置，类型：{}", configType);

        if (!StringUtils.hasText(configType)) {
            return getAllSystemConfigs();
        }

        String normalizedType = configType.toLowerCase(Locale.ROOT);
        List<SystemConfig> configs;

        if ("website".equals(normalizedType)) {
            configs = selectByKeys(WEBSITE_KEYS);
        } else if ("email".equals(normalizedType)) {
            configs = selectByKeys(EMAIL_KEYS);
        } else if ("file".equals(normalizedType) || "file-upload".equals(normalizedType)) {
            configs = selectByKeys(FILE_UPLOAD_KEYS);
        } else {
            configs = systemConfigMapper.selectList(new LambdaQueryWrapper<SystemConfig>()
                    .eq(SystemConfig::getConfigType, configType)
                    .orderByAsc(SystemConfig::getConfigKey));
        }

        List<SystemConfigDTO> result = configs.stream().map(this::toDTO).collect(Collectors.toList());
        return Result.success(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateSystemConfig(SystemConfigDTO systemConfigDTO) {
        log.info("更新系统配置，配置信息：{}", systemConfigDTO);

        if (systemConfigDTO == null || !StringUtils.hasText(systemConfigDTO.getConfigKey())) {
            return Result.error(ResultCode.BAD_REQUEST, "配置键不能为空");
        }

        if (systemConfigDTO.getConfigValue() == null) {
            return Result.error(ResultCode.BAD_REQUEST, "配置值不能为空");
        }

        upsertConfig(
                systemConfigDTO.getConfigKey(),
                systemConfigDTO.getConfigValue(),
                StringUtils.hasText(systemConfigDTO.getConfigType()) ? systemConfigDTO.getConfigType() : "string",
                systemConfigDTO.getDescription()
        );

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> batchUpdateSystemConfigs(List<SystemConfigDTO> systemConfigDTOList) {
        int size = systemConfigDTOList == null ? 0 : systemConfigDTOList.size();
        log.info("批量更新系统配置，配置数量：{}", size);

        if (systemConfigDTOList == null || systemConfigDTOList.isEmpty()) {
            return Result.error(ResultCode.BAD_REQUEST, "配置列表不能为空");
        }

        for (SystemConfigDTO dto : systemConfigDTOList) {
            if (dto == null || !StringUtils.hasText(dto.getConfigKey())) {
                return Result.error(ResultCode.BAD_REQUEST, "配置键不能为空");
            }
            if (dto.getConfigValue() == null) {
                return Result.error(ResultCode.BAD_REQUEST, "配置值不能为空: " + dto.getConfigKey());
            }
            upsertConfig(
                    dto.getConfigKey(),
                    dto.getConfigValue(),
                    StringUtils.hasText(dto.getConfigType()) ? dto.getConfigType() : "string",
                    dto.getDescription()
            );
        }

        return Result.success();
    }

    @Override
    public Result<WebsiteConfigDTO> getWebsiteConfig() {
        log.info("获取网站配置");

        Map<String, String> configMap = selectValueMap(WEBSITE_KEYS);
        WebsiteConfigDTO dto = new WebsiteConfigDTO();
        dto.setWebsiteName(configMap.get(CFG_SITE_NAME));
        dto.setWebsiteDescription(configMap.get(CFG_SITE_DESCRIPTION));
        dto.setWebsiteKeywords(configMap.get(CFG_SITE_KEYWORDS));
        dto.setWebsiteLogo(configMap.get(CFG_SITE_LOGO));
        dto.setWebsiteFavicon(configMap.get(CFG_SITE_FAVICON));
        dto.setWebsiteIcp(configMap.get(CFG_SITE_ICP));
        dto.setWebsiteAnalytics(configMap.get(CFG_SITE_ANALYTICS));
        dto.setWebsiteStatus(parseInteger(configMap.get(CFG_SITE_STATUS)));
        dto.setCloseMessage(configMap.get(CFG_CLOSE_MESSAGE));
        dto.setPageSize(parseInteger(configMap.get(CFG_ARTICLES_PER_PAGE)));
        dto.setRssLimit(parseInteger(configMap.get(CFG_RSS_LIMIT)));
        dto.setCommentStatus(parseSwitch(configMap.get(CFG_ALLOW_COMMENT)));
        dto.setRegisterStatus(parseSwitch(configMap.get(CFG_ALLOW_REGISTER)));
        return Result.success(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateWebsiteConfig(WebsiteConfigDTO websiteConfigDTO) {
        log.info("更新网站配置，配置信息：{}", websiteConfigDTO);

        if (websiteConfigDTO == null) {
            return Result.error(ResultCode.BAD_REQUEST, "网站配置不能为空");
        }

        upsertConfig(CFG_SITE_NAME, websiteConfigDTO.getWebsiteName(), "string", "网站名称");
        upsertConfig(CFG_SITE_DESCRIPTION, websiteConfigDTO.getWebsiteDescription(), "string", "网站描述");
        upsertConfig(CFG_SITE_KEYWORDS, websiteConfigDTO.getWebsiteKeywords(), "string", "网站关键词");
        upsertConfig(CFG_SITE_LOGO, websiteConfigDTO.getWebsiteLogo(), "string", "网站Logo URL");
        upsertConfig(CFG_SITE_FAVICON, websiteConfigDTO.getWebsiteFavicon(), "string", "网站图标 URL");
        upsertConfig(CFG_SITE_ICP, websiteConfigDTO.getWebsiteIcp(), "string", "网站备案号");
        upsertConfig(CFG_SITE_ANALYTICS, websiteConfigDTO.getWebsiteAnalytics(), "string", "网站统计代码");
        upsertConfig(CFG_SITE_STATUS, toStringValue(websiteConfigDTO.getWebsiteStatus()), "number", "网站状态");
        upsertConfig(CFG_CLOSE_MESSAGE, websiteConfigDTO.getCloseMessage(), "string", "网站关闭提示");
        upsertConfig(CFG_ARTICLES_PER_PAGE, toStringValue(websiteConfigDTO.getPageSize()), "number", "每页文章数量");
        upsertConfig(CFG_RSS_LIMIT, toStringValue(websiteConfigDTO.getRssLimit()), "number", "RSS订阅数量");
        upsertConfig(CFG_ALLOW_COMMENT, toBooleanString(websiteConfigDTO.getCommentStatus()), "boolean", "是否允许评论");
        upsertConfig(CFG_ALLOW_REGISTER, toBooleanString(websiteConfigDTO.getRegisterStatus()), "boolean", "是否允许注册");

        return Result.success();
    }

    @Override
    public Result<EmailConfigDTO> getEmailConfig() {
        log.info("获取邮件配置");

        Map<String, String> configMap = selectValueMap(EMAIL_KEYS);
        EmailConfigDTO dto = new EmailConfigDTO();
        dto.setSmtpHost(configMap.get(CFG_SMTP_HOST));
        dto.setSmtpPort(parseInteger(configMap.get(CFG_SMTP_PORT)));
        dto.setSmtpUsername(configMap.get(CFG_SMTP_USERNAME));
        dto.setSmtpPassword(configMap.get(CFG_SMTP_PASSWORD));
        dto.setEnableSsl(parseSwitch(configMap.get(CFG_SMTP_ENABLE_SSL)));
        dto.setFromEmail(configMap.get(CFG_FROM_EMAIL));
        dto.setFromName(configMap.get(CFG_FROM_NAME));
        dto.setEmailEnabled(parseSwitch(configMap.get(CFG_EMAIL_ENABLED)));
        return Result.success(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateEmailConfig(EmailConfigDTO emailConfigDTO) {
        log.info("更新邮件配置，配置信息：{}", emailConfigDTO);

        if (emailConfigDTO == null) {
            return Result.error(ResultCode.BAD_REQUEST, "邮件配置不能为空");
        }

        upsertConfig(CFG_SMTP_HOST, emailConfigDTO.getSmtpHost(), "string", "SMTP服务器地址");
        upsertConfig(CFG_SMTP_PORT, toStringValue(emailConfigDTO.getSmtpPort()), "number", "SMTP服务器端口");
        upsertConfig(CFG_SMTP_USERNAME, emailConfigDTO.getSmtpUsername(), "string", "SMTP用户名");
        upsertConfig(CFG_SMTP_PASSWORD, emailConfigDTO.getSmtpPassword(), "string", "SMTP密码");
        upsertConfig(CFG_SMTP_ENABLE_SSL, toBooleanString(emailConfigDTO.getEnableSsl()), "boolean", "是否启用SSL");
        upsertConfig(CFG_FROM_EMAIL, emailConfigDTO.getFromEmail(), "string", "发件人邮箱");
        upsertConfig(CFG_FROM_NAME, emailConfigDTO.getFromName(), "string", "发件人名称");
        upsertConfig(CFG_EMAIL_ENABLED, toBooleanString(emailConfigDTO.getEmailEnabled()), "boolean", "是否启用邮件功能");

        return Result.success();
    }

    @Override
    public Result<FileUploadConfigDTO> getFileUploadConfig() {
        log.info("获取文件上传配置");

        Map<String, String> configMap = selectValueMap(FILE_UPLOAD_KEYS);
        FileUploadConfigDTO dto = new FileUploadConfigDTO();
        dto.setMaxFileSize(parseInteger(configMap.get(CFG_MAX_FILE_SIZE)));
        dto.setAllowedImageTypes(configMap.get(CFG_ALLOWED_IMAGE_TYPES));
        dto.setAllowedFileTypes(configMap.get(CFG_ALLOWED_FILE_TYPES));
        dto.setImageUploadPath(configMap.get(CFG_IMAGE_UPLOAD_PATH));
        dto.setFileUploadPath(configMap.get(CFG_FILE_UPLOAD_PATH));
        dto.setEnableLocalStorage(parseSwitch(configMap.get(CFG_ENABLE_LOCAL_STORAGE)));
        dto.setEnableOssStorage(parseSwitch(configMap.get(CFG_ENABLE_OSS_STORAGE)));
        dto.setOssAccessKey(configMap.get(CFG_OSS_ACCESS_KEY));
        dto.setOssSecretKey(configMap.get(CFG_OSS_SECRET_KEY));
        dto.setOssBucketName(configMap.get(CFG_OSS_BUCKET_NAME));
        dto.setOssEndpoint(configMap.get(CFG_OSS_ENDPOINT));
        return Result.success(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateFileUploadConfig(FileUploadConfigDTO fileUploadConfigDTO) {
        log.info("更新文件上传配置，配置信息：{}", fileUploadConfigDTO);

        if (fileUploadConfigDTO == null) {
            return Result.error(ResultCode.BAD_REQUEST, "文件上传配置不能为空");
        }

        upsertConfig(CFG_MAX_FILE_SIZE, toStringValue(fileUploadConfigDTO.getMaxFileSize()), "number", "上传文件最大大小(MB)");
        upsertConfig(CFG_ALLOWED_IMAGE_TYPES, fileUploadConfigDTO.getAllowedImageTypes(), "string", "允许上传的图片类型");
        upsertConfig(CFG_ALLOWED_FILE_TYPES, fileUploadConfigDTO.getAllowedFileTypes(), "string", "允许上传的文件类型");
        upsertConfig(CFG_IMAGE_UPLOAD_PATH, fileUploadConfigDTO.getImageUploadPath(), "string", "图片上传路径");
        upsertConfig(CFG_FILE_UPLOAD_PATH, fileUploadConfigDTO.getFileUploadPath(), "string", "文件上传路径");
        upsertConfig(CFG_ENABLE_LOCAL_STORAGE, toBooleanString(fileUploadConfigDTO.getEnableLocalStorage()), "boolean", "是否启用本地存储");
        upsertConfig(CFG_ENABLE_OSS_STORAGE, toBooleanString(fileUploadConfigDTO.getEnableOssStorage()), "boolean", "是否启用OSS存储");
        upsertConfig(CFG_OSS_ACCESS_KEY, fileUploadConfigDTO.getOssAccessKey(), "string", "OSS访问密钥");
        upsertConfig(CFG_OSS_SECRET_KEY, fileUploadConfigDTO.getOssSecretKey(), "string", "OSS密钥");
        upsertConfig(CFG_OSS_BUCKET_NAME, fileUploadConfigDTO.getOssBucketName(), "string", "OSS存储桶名称");
        upsertConfig(CFG_OSS_ENDPOINT, fileUploadConfigDTO.getOssEndpoint(), "string", "OSS访问域名");

        return Result.success();
    }

    private List<SystemConfig> selectByKeys(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        return systemConfigMapper.selectList(new LambdaQueryWrapper<SystemConfig>()
                .in(SystemConfig::getConfigKey, keys)
                .orderByAsc(SystemConfig::getConfigKey));
    }

    private Map<String, String> selectValueMap(Collection<String> keys) {
        List<SystemConfig> configs = selectByKeys(keys);
        Map<String, String> result = new HashMap<>();
        for (SystemConfig config : configs) {
            result.put(config.getConfigKey(), config.getConfigValue());
        }
        return result;
    }

    private void upsertConfig(String configKey, String configValue, String configType, String description) {
        if (!StringUtils.hasText(configKey) || configValue == null) {
            return;
        }

        SystemConfig existed = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, configKey)
                .last("LIMIT 1"));

        if (existed == null) {
            SystemConfig config = new SystemConfig();
            config.setConfigKey(configKey);
            config.setConfigValue(configValue);
            config.setConfigType(StringUtils.hasText(configType) ? configType : "string");
            config.setDescription(description);
            config.setIsPublic(0);
            systemConfigMapper.insert(config);
            return;
        }

        existed.setConfigValue(configValue);
        if (StringUtils.hasText(configType)) {
            existed.setConfigType(configType);
        }
        if (description != null) {
            existed.setDescription(description);
        }
        systemConfigMapper.updateById(existed);
    }

    private SystemConfigDTO toDTO(SystemConfig config) {
        SystemConfigDTO dto = new SystemConfigDTO();
        dto.setConfigId(config.getId());
        dto.setConfigKey(config.getConfigKey());
        dto.setConfigValue(config.getConfigValue());
        dto.setDescription(config.getDescription());
        dto.setConfigType(config.getConfigType());
        dto.setIsEditable(1);
        if (config.getCreateTime() != null) {
            dto.setCreatedAt(config.getCreateTime().format(DATE_TIME_FORMATTER));
        }
        if (config.getUpdateTime() != null) {
            dto.setUpdatedAt(config.getUpdateTime().format(DATE_TIME_FORMATTER));
        }
        return dto;
    }

    private Integer parseInteger(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            log.warn("系统配置值不是数字: {}", value);
            return null;
        }
    }

    private Integer parseSwitch(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("1".equals(normalized) || "true".equals(normalized) || "yes".equals(normalized) || "on".equals(normalized)) {
            return 1;
        }
        if ("0".equals(normalized) || "false".equals(normalized) || "no".equals(normalized) || "off".equals(normalized)) {
            return 0;
        }
        return parseInteger(value);
    }

    private String toStringValue(Integer value) {
        return value == null ? null : String.valueOf(value);
    }

    private String toBooleanString(Integer value) {
        if (value == null) {
            return null;
        }
        return value == 1 ? "true" : "false";
    }
}