package com.blog.service.impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.blog.dto.FileUploadConfigDTO;
import com.blog.dto.SystemConfigDTO;
import com.blog.entity.SystemConfig;
import com.blog.mapper.SystemConfigMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemConfigUploadLogRedactionTest {

    private final SystemConfigServiceImpl service = new SystemConfigServiceImpl();

    @Test
    void updateFileUploadConfig_shouldNotLogOssSecrets() {
        setField(service, "systemConfigMapper", mock(SystemConfigMapper.class));

        Logger logger = (Logger) LoggerFactory.getLogger(SystemConfigServiceImpl.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            FileUploadConfigDTO dto = new FileUploadConfigDTO();
            dto.setOssAccessKey("SENTINEL-ACCESS-KEY");
            dto.setOssSecretKey("SENTINEL-SECRET-KEY");

            var result = service.updateFileUploadConfig(dto);

            assertThat(result.isSuccess()).isTrue();
            assertThat(appender.list)
                    .allSatisfy(e -> assertThat(e.getFormattedMessage())
                            .doesNotContain("SENTINEL-ACCESS-KEY", "SENTINEL-SECRET-KEY"));
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void updateSystemConfig_shouldNotLogConfigValue() {
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.insert(any(SystemConfig.class))).thenReturn(1);
        setField(service, "systemConfigMapper", mapper);

        Logger logger = (Logger) LoggerFactory.getLogger(SystemConfigServiceImpl.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            SystemConfigDTO dto = new SystemConfigDTO();
            dto.setConfigKey("smtp_password");
            dto.setConfigValue("SENTINEL-CONFIG-VALUE");

            var result = service.updateSystemConfig(dto);

            assertThat(result.isSuccess()).isTrue();
            assertThat(appender.list)
                    .isNotEmpty()
                    .allSatisfy(e -> assertThat(e.getFormattedMessage())
                            .doesNotContain("SENTINEL-CONFIG-VALUE"))
                    .anySatisfy(e -> assertThat(e.getFormattedMessage()).contains("smtp_password"));
        } finally {
            logger.detachAppender(appender);
        }
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
