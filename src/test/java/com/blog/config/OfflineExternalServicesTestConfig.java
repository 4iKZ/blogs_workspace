package com.blog.config;

import com.blog.service.TOSService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Collections;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Configuration
@Profile("test")
public class OfflineExternalServicesTestConfig {

    @Bean
    @Primary
    public RedisTemplate<String, Object> offlineRedisTemplate() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, Object> template = mock(RedisTemplate.class, RETURNS_DEEP_STUBS);
        when(template.hasKey(any())).thenReturn(false);
        when(template.delete(any(String.class))).thenReturn(false);
        when(template.keys(any())).thenReturn(Collections.emptySet());
        return template;
    }

    @Bean
    @Primary
    public StringRedisTemplate offlineStringRedisTemplate() {
        return mock(StringRedisTemplate.class, RETURNS_DEEP_STUBS);
    }

    @Bean
    @Primary
    public TOSService offlineTosService() {
        TOSService service = mock(TOSService.class);
        when(service.uploadFile(any(), any())).thenReturn("https://mock.local/file");
        when(service.uploadFileWithStyle(any(), any(), any(Boolean.class))).thenReturn("https://mock.local/image");
        when(service.uploadFileWithStyleAtObjectKey(any(), any(), any(Boolean.class)))
                .thenReturn("https://mock.local/image");
        when(service.deleteFile(any())).thenReturn(true);
        return service;
    }

    @Bean
    @Primary
    public JavaMailSender offlineMailSender() {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        return sender;
    }
}
