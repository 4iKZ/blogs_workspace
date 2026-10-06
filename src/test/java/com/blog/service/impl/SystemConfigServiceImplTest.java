package com.blog.service.impl;

import com.blog.entity.SystemConfig;
import com.blog.mapper.SystemConfigMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SystemConfigServiceImplTest {

    private final SystemConfigServiceImpl service = new SystemConfigServiceImpl();

    @Test
    void getWebsiteConfig_emptyMap_shouldReturnNullFields() {
        setField(service, "systemConfigMapper", mock(SystemConfigMapper.class));

        var result = service.getWebsiteConfig();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getWebsiteName()).isNull();
        assertThat(result.getData().getWebsiteStatus()).isNull();
    }

    @Test
    void getEmailConfig_emptyMap_shouldReturnNullFields() {
        setField(service, "systemConfigMapper", mock(SystemConfigMapper.class));

        var result = service.getEmailConfig();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getSmtpHost()).isNull();
        assertThat(result.getData().getSmtpPort()).isNull();
        assertThat(result.getData().getEnableSsl()).isNull();
    }

    @Test
    void getEmailConfig_storedPassword_shouldNeverExposePassword() {
        SystemConfig password = new SystemConfig();
        password.setConfigKey("smtp_password");
        password.setConfigValue("stored-secret");
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(password));
        setField(service, "systemConfigMapper", mapper);

        var result = service.getEmailConfig();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getSmtpPassword()).isNull();
    }

    @Test
    void updateEmailConfig_blankPassword_shouldPreserveStoredPassword() {
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        List<SystemConfig> inserted = new ArrayList<>();
        when(mapper.insert(any())).thenAnswer(invocation -> {
            inserted.add(invocation.getArgument(0));
            return 1;
        });
        setField(service, "systemConfigMapper", mapper);

        var dto = new com.blog.dto.EmailConfigDTO();
        dto.setSmtpHost("smtp.example.com");
        dto.setSmtpPassword(" ");
        var result = service.updateEmailConfig(dto);

        assertThat(result.isSuccess()).isTrue();
        assertThat(inserted)
                .extracting(SystemConfig::getConfigKey)
                .doesNotContain("smtp_password");
    }

    @Test
    void getFileUploadConfig_emptyMap_shouldReturnNullFields() {
        setField(service, "systemConfigMapper", mock(SystemConfigMapper.class));

        var result = service.getFileUploadConfig();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getMaxFileSize()).isNull();
        assertThat(result.getData().getEnableLocalStorage()).isNull();
    }

    @Test
    void getSystemConfig_blankKey_shouldReturnError() {
        var result = service.getSystemConfig("");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("配置键不能为空");
    }

    @Test
    void updateSystemConfig_nullDto_shouldReturnError() {
        var result = service.updateSystemConfig(null);

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void updateSystemConfig_blankKey_shouldReturnError() {
        var dto = new com.blog.dto.SystemConfigDTO();
        dto.setConfigKey("");
        dto.setConfigValue("value");

        var result = service.updateSystemConfig(dto);

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void updateSystemConfig_nullValue_shouldReturnError() {
        var dto = new com.blog.dto.SystemConfigDTO();
        dto.setConfigKey("key");
        dto.setConfigValue(null);

        var result = service.updateSystemConfig(dto);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("配置值不能为空");
    }

    @Test
    void batchUpdateSystemConfigs_nullList_shouldReturnError() {
        var result = service.batchUpdateSystemConfigs(null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("配置列表不能为空");
    }

    @Test
    void batchUpdateSystemConfigs_emptyList_shouldReturnError() {
        var result = service.batchUpdateSystemConfigs(List.of());

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void getAllSystemConfigs_empty_shouldReturnEmptyList() {
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of());
        setField(service, "systemConfigMapper", mapper);

        var result = service.getAllSystemConfigs();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEmpty();
    }

    @Test
    void getAllSystemConfigs_withData_shouldReturnList() {
        SystemConfig config = new SystemConfig();
        config.setId(1L);
        config.setConfigKey("key");
        config.setConfigValue("value");
        config.setConfigType("string");
        LocalDateTime now = LocalDateTime.now();
        config.setCreateTime(now);
        config.setUpdateTime(now);

        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(config));
        setField(service, "systemConfigMapper", mapper);

        var result = service.getAllSystemConfigs();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getConfigKey()).isEqualTo("key");
        assertThat(result.getData().get(0).getConfigValue()).isEqualTo("value");
        assertThat(result.getData().get(0).getIsEditable()).isEqualTo(1);
        assertThat(result.getData().get(0).getCreatedAt()).isNotNull();
    }

    @Test
    void getSystemConfigsByType_blankType_shouldReturnAll() {
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of());
        setField(service, "systemConfigMapper", mapper);

        var result = service.getSystemConfigsByType("");

        assertThat(result.isSuccess()).isTrue();
        verify(mapper, times(1)).selectList(any());
    }

    // ==================== 开关/数值解析分支补充 ====================

    private SystemConfig cfg(String key, String value) {
        SystemConfig c = new SystemConfig();
        c.setConfigKey(key);
        c.setConfigValue(value);
        return c;
    }

    @Test
    void getWebsiteConfig_trueFalseSwitches_shouldParseCorrectly() {
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(
                cfg("allow_comment", "true"),
                cfg("allow_register", "no"),
                cfg("site_status", "1"),
                cfg("articles_per_page", "not-a-number")));
        setField(service, "systemConfigMapper", mapper);

        var result = service.getWebsiteConfig();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getCommentStatus()).isEqualTo(1);
        assertThat(result.getData().getRegisterStatus()).isEqualTo(0);
        assertThat(result.getData().getWebsiteStatus()).isEqualTo(1);
        assertThat(result.getData().getPageSize()).isNull();
    }

    @Test
    void updateWebsiteConfig_shouldWriteBooleanStrings() {
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        List<SystemConfig> inserted = new ArrayList<>();
        when(mapper.insert(any())).thenAnswer(invocation -> {
            inserted.add(invocation.getArgument(0));
            return 1;
        });
        setField(service, "systemConfigMapper", mapper);

        var dto = new com.blog.dto.WebsiteConfigDTO();
        dto.setWebsiteName("Blog");
        dto.setCommentStatus(1);
        dto.setRegisterStatus(0);

        var result = service.updateWebsiteConfig(dto);

        assertThat(result.isSuccess()).isTrue();
        assertThat(inserted).extracting(SystemConfig::getConfigKey).contains("allow_comment", "allow_register");
        assertThat(inserted.stream()
                .filter(c -> "allow_comment".equals(c.getConfigKey()))
                .findFirst().orElseThrow().getConfigValue()).isEqualTo("true");
    }

    @Test
    void updateEmailConfig_existingPassword_shouldUpdate() {
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        when(mapper.selectOne(any())).thenReturn(new SystemConfig());
        when(mapper.updateById(any())).thenReturn(1);
        setField(service, "systemConfigMapper", mapper);

        var dto = new com.blog.dto.EmailConfigDTO();
        dto.setSmtpHost("smtp.example.com");
        dto.setSmtpPort(587);
        dto.setSmtpPassword("secret");

        var result = service.updateEmailConfig(dto);

        assertThat(result.isSuccess()).isTrue();
        verify(mapper, atLeastOnce()).updateById(any());
    }

    @Test
    void getEmailConfig_trueFalseSwitches_shouldParse() {
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(
                cfg("smtp_enable_ssl", "yes"),
                cfg("email_enabled", "off")));
        setField(service, "systemConfigMapper", mapper);

        var result = service.getEmailConfig();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getEnableSsl()).isEqualTo(1);
        assertThat(result.getData().getEmailEnabled()).isEqualTo(0);
    }

    @Test
    void getAllSystemConfigs_shouldMaskSmtpPassword() {
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(
                cfg("smtp_password", "stored-secret"),
                cfg("smtp_host", "smtp.example.com")));
        setField(service, "systemConfigMapper", mapper);

        var result = service.getAllSystemConfigs();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).filteredOn(d -> "smtp_password".equals(d.getConfigKey()))
                .singleElement().extracting(d -> d.getConfigValue()).isNull();
        assertThat(result.getData()).filteredOn(d -> "smtp_host".equals(d.getConfigKey()))
                .singleElement().extracting(d -> d.getConfigValue()).isEqualTo("smtp.example.com");
    }

    @Test
    void getSystemConfigsByType_email_shouldMaskSmtpPassword() {
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(
                cfg("smtp_password", "stored-secret"),
                cfg("smtp_host", "smtp.example.com")));
        setField(service, "systemConfigMapper", mapper);

        var result = service.getSystemConfigsByType("email");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).filteredOn(d -> "smtp_password".equals(d.getConfigKey()))
                .singleElement().extracting(d -> d.getConfigValue()).isNull();
    }

    // ==================== OSS 密钥脱敏（P2-3） ====================

    @Test
    void getFileUploadConfig_shouldMaskOssSecrets() {
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(
                cfg("oss_access_key", "AK-SECRET"),
                cfg("oss_secret_key", "SK-SECRET"),
                cfg("oss_bucket_name", "my-bucket"),
                cfg("max_file_size", "10")));
        setField(service, "systemConfigMapper", mapper);

        var result = service.getFileUploadConfig();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getOssAccessKey()).isNull();
        assertThat(result.getData().getOssSecretKey()).isNull();
        // 非 secret 配置仍正常回显
        assertThat(result.getData().getOssBucketName()).isEqualTo("my-bucket");
        assertThat(result.getData().getMaxFileSize()).isEqualTo(10);
    }

    @Test
    void getAllSystemConfigs_shouldMaskOssAndSmtpSecrets() {
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(
                cfg("oss_access_key", "AK-SECRET"),
                cfg("oss_secret_key", "SK-SECRET"),
                cfg("smtp_password", "stored-secret"),
                cfg("oss_bucket_name", "bucket")));
        setField(service, "systemConfigMapper", mapper);

        var result = service.getAllSystemConfigs();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).filteredOn(d -> "oss_access_key".equals(d.getConfigKey()))
                .singleElement().extracting(d -> d.getConfigValue()).isNull();
        assertThat(result.getData()).filteredOn(d -> "oss_secret_key".equals(d.getConfigKey()))
                .singleElement().extracting(d -> d.getConfigValue()).isNull();
        assertThat(result.getData()).filteredOn(d -> "smtp_password".equals(d.getConfigKey()))
                .singleElement().extracting(d -> d.getConfigValue()).isNull();
        assertThat(result.getData()).filteredOn(d -> "oss_bucket_name".equals(d.getConfigKey()))
                .singleElement().extracting(d -> d.getConfigValue()).isEqualTo("bucket");
    }

    @Test
    void getSystemConfigsByType_file_shouldMaskOssSecrets() {
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(
                cfg("oss_access_key", "AK-SECRET"),
                cfg("oss_secret_key", "SK-SECRET")));
        setField(service, "systemConfigMapper", mapper);

        var result = service.getSystemConfigsByType("file");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).allSatisfy(d -> assertThat(d.getConfigValue()).isNull());
    }

    @Test
    void updateFileUploadConfig_nullOssSecrets_shouldNotUpsertThoseKeys() {
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        List<SystemConfig> inserted = new ArrayList<>();
        when(mapper.insert(any())).thenAnswer(invocation -> {
            inserted.add(invocation.getArgument(0));
            return 1;
        });
        setField(service, "systemConfigMapper", mapper);

        var dto = new com.blog.dto.FileUploadConfigDTO();
        dto.setMaxFileSize(10);
        dto.setOssAccessKey(null);
        dto.setOssSecretKey(null);

        var result = service.updateFileUploadConfig(dto);

        // 脱敏字段回传 null 时必须跳过更新（upsertConfig 的 null 语义），避免清空已存密钥
        assertThat(result.isSuccess()).isTrue();
        assertThat(inserted).extracting(SystemConfig::getConfigKey)
                .contains("max_file_size")
                .doesNotContain("oss_access_key", "oss_secret_key");
    }

    private static void setField(SystemConfigServiceImpl target, String fieldName, Object value) {
        try {
            var field = SystemConfigServiceImpl.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
