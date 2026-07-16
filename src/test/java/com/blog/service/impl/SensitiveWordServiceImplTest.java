package com.blog.service.impl;

import com.blog.common.Result;
import com.blog.dto.SensitiveCheckResultDTO;
import com.blog.mapper.SensitiveWordMapper;
import com.blog.service.SensitiveWordService;
import com.blog.utils.SensitiveWordFilter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SensitiveWordServiceImplTest {

    @Test
    void checkContent_blank_shouldReturnPass() {
        SensitiveWordFilter filter = mock(SensitiveWordFilter.class);
        SensitiveWordServiceImpl service = new SensitiveWordServiceImpl();
        setField(service, "sensitiveWordFilter", filter);

        Result<SensitiveCheckResultDTO> result = service.checkContent("");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().isPassed()).isTrue();
    }

    @Test
    void checkContent_hitWords_shouldReturnFail() {
        SensitiveWordFilter filter = mock(SensitiveWordFilter.class);
        when(filter.getSensitiveWords("bad text")).thenReturn(Set.of("bad", "text"));

        SensitiveWordServiceImpl service = new SensitiveWordServiceImpl();
        setField(service, "sensitiveWordFilter", filter);

        Result<SensitiveCheckResultDTO> result = service.checkContent("bad text");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().isPassed()).isFalse();
        assertThat(result.getData().getHitWords()).containsExactlyInAnyOrder("bad", "text");
    }

    @Test
    void validateContent_blank_shouldReturnSuccess() {
        SensitiveWordFilter filter = mock(SensitiveWordFilter.class);
        SensitiveWordServiceImpl service = new SensitiveWordServiceImpl();
        setField(service, "sensitiveWordFilter", filter);

        Result<Void> result = service.validateContent("   ");

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void validateContent_hitWords_shouldReturnError() {
        SensitiveWordFilter filter = mock(SensitiveWordFilter.class);
        when(filter.getSensitiveWords("bad")).thenReturn(Set.of("bad"));

        SensitiveWordServiceImpl service = new SensitiveWordServiceImpl();
        setField(service, "sensitiveWordFilter", filter);

        Result<Void> result = service.validateContent("bad");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("敏感词");
    }

    @Test
    void getHitWords_blank_shouldReturnEmptyList() {
        SensitiveWordServiceImpl service = new SensitiveWordServiceImpl();
        setField(service, "sensitiveWordFilter", mock(SensitiveWordFilter.class));

        Result<java.util.List<String>> result = service.getHitWords(null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEmpty();
    }

    @Test
    void getHitWords_hit_shouldReturnList() {
        SensitiveWordFilter filter = mock(SensitiveWordFilter.class);
        when(filter.getSensitiveWords("hello bad world")).thenReturn(Set.of("bad"));

        SensitiveWordServiceImpl service = new SensitiveWordServiceImpl();
        setField(service, "sensitiveWordFilter", filter);

        Result<java.util.List<String>> result = service.getHitWords("hello bad world");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).containsExactly("bad");
    }

    @Test
    void replaceContent_blank_shouldReturnOriginal() {
        SensitiveWordServiceImpl service = new SensitiveWordServiceImpl();
        setField(service, "sensitiveWordFilter", mock(SensitiveWordFilter.class));

        Result<String> result = service.replaceContent("");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEmpty();
    }

    @Test
    void replaceContent_hit_shouldReturnReplaced() {
        SensitiveWordFilter filter = mock(SensitiveWordFilter.class);
        when(filter.replaceSensitiveWords("bad text")).thenReturn("* text");

        SensitiveWordServiceImpl service = new SensitiveWordServiceImpl();
        setField(service, "sensitiveWordFilter", filter);

        Result<String> result = service.replaceContent("bad text");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo("* text");
    }

    @Test
    void reloadCache_shouldReloadAndReturnSuccess() {
        SensitiveWordFilter filter = mock(SensitiveWordFilter.class);
        SensitiveWordServiceImpl service = new SensitiveWordServiceImpl();
        setField(service, "sensitiveWordFilter", filter);

        Result<Void> result = service.reloadCache();

        assertThat(result.isSuccess()).isTrue();
        verify(filter, times(1)).reloadSensitiveWords();
    }

    private static void setField(SensitiveWordServiceImpl target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = SensitiveWordServiceImpl.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
