package com.blog.service.impl;

import com.blog.dto.ModerationResult;
import com.blog.entity.ArticleModerationLog;
import com.blog.mapper.ArticleModerationLogMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;
import com.fasterxml.jackson.databind.ObjectMapper;

class ArticleModerationLogServiceImplTest {

    private final ArticleModerationLogServiceImpl service = new ArticleModerationLogServiceImpl();

    @Test
    void saveModerationLog_contentTooLong_shouldTruncate() throws Exception {
        ArticleModerationLogMapper mapper = mock(ArticleModerationLogMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        when(mapper.insert(any())).thenReturn(1);
        setField(service, "moderationLogMapper", mapper);
        setField(service, "objectMapper", objectMapper);

        String longContent = "x".repeat(600);
        ModerationResult result = new ModerationResult();
        result.setPassed(true);
        result.setViolationType("none");
        result.setConfidence(0.99);
        result.setReasons(List.of());

        var response = service.saveModerationLog(1L, "title", longContent, result);

        assertThat(response.isSuccess()).isTrue();
        verify(mapper, times(1)).insert(argThat(log -> log.getContent().length() == 500));
    }

    @Test
    void saveModerationLog_withReasons_shouldSerializeJson() throws Exception {
        ArticleModerationLogMapper mapper = mock(ArticleModerationLogMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        when(mapper.insert(any())).thenReturn(1);
        setField(service, "moderationLogMapper", mapper);
        setField(service, "objectMapper", objectMapper);

        ModerationResult result = new ModerationResult(false, "porn", List.of("reason1", "reason2"), 0.8, "suggestion");

        var response = service.saveModerationLog(1L, "title", "content", result);

        assertThat(response.isSuccess()).isTrue();
        ArgumentCaptor<ArticleModerationLog> captor = ArgumentCaptor.forClass(ArticleModerationLog.class);
        verify(mapper, times(1)).insert(captor.capture());
        ArticleModerationLog saved = captor.getValue();
        assertThat(saved.getReasons()).isNotNull();
        assertThat(saved.getReasons()).contains("reason1");
        assertThat(saved.getReasons()).contains("reason2");
    }

    @Test
    void getLatestLog_found_shouldReturnLog() throws Exception {
        ArticleModerationLogMapper mapper = mock(ArticleModerationLogMapper.class);
        ArticleModerationLog log = new ArticleModerationLog();
        log.setId(10L);
        when(mapper.selectOne(any())).thenReturn(log);
        setField(service, "moderationLogMapper", mapper);

        var response = service.getLatestLog(1L);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getId()).isEqualTo(10L);
    }

    @Test
    void getLatestLog_notFound_shouldReturnError() throws Exception {
        ArticleModerationLogMapper mapper = mock(ArticleModerationLogMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        setField(service, "moderationLogMapper", mapper);

        var response = service.getLatestLog(1L);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("未找到审核记录");
    }

    private static void setField(ArticleModerationLogServiceImpl target, String fieldName, Object value) {
        try {
            var field = ArticleModerationLogServiceImpl.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
