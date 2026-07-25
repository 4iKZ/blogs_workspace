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
