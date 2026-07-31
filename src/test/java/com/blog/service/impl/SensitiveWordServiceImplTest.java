package com.blog.service.impl;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.dto.SensitiveCheckResultDTO;
import com.blog.dto.SensitiveWordCreateDTO;
import com.blog.dto.SensitiveWordDTO;
import com.blog.entity.SensitiveWord;
import com.blog.mapper.SensitiveWordMapper;
import com.blog.service.SensitiveWordService;
import com.blog.utils.SensitiveWordFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SensitiveWordServiceImplTest {

    @Mock
    private SensitiveWordMapper sensitiveWordMapper;

    @Mock
    private SensitiveWordFilter sensitiveWordFilter;

    @InjectMocks
    private SensitiveWordServiceImpl sensitiveWordService;

    @BeforeEach
    void setUp() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 1L);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    // ==================== checkContent / validateContent / getHitWords / replaceContent / reloadCache ====================

    @Test
    @DisplayName("checkContent - 空内容应返回通过")
    void checkContent_blank_shouldReturnPass() {
        Result<SensitiveCheckResultDTO> result = sensitiveWordService.checkContent("");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().isPassed()).isTrue();
    }

    @Test
    @DisplayName("checkContent - 命中敏感词应返回失败")
    void checkContent_hitWords_shouldReturnFail() {
        when(sensitiveWordFilter.getSensitiveWords("bad text")).thenReturn(Set.of("bad", "text"));

        Result<SensitiveCheckResultDTO> result = sensitiveWordService.checkContent("bad text");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().isPassed()).isFalse();
        assertThat(result.getData().getHitWords()).containsExactlyInAnyOrder("bad", "text");
    }

    @Test
    @DisplayName("validateContent - 空内容应返回成功")
    void validateContent_blank_shouldReturnSuccess() {
        Result<Void> result = sensitiveWordService.validateContent("   ");

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("validateContent - 命中敏感词应返回错误")
    void validateContent_hitWords_shouldReturnError() {
        when(sensitiveWordFilter.getSensitiveWords("bad")).thenReturn(Set.of("bad"));

        Result<Void> result = sensitiveWordService.validateContent("bad");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("敏感词");
    }

    @Test
    @DisplayName("getHitWords - 空内容应返回空列表")
    void getHitWords_blank_shouldReturnEmptyList() {
        Result<List<String>> result = sensitiveWordService.getHitWords(null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEmpty();
    }

    @Test
    @DisplayName("getHitWords - 命中敏感词应返回列表")
    void getHitWords_hit_shouldReturnList() {
        when(sensitiveWordFilter.getSensitiveWords("hello bad world")).thenReturn(Set.of("bad"));

        Result<List<String>> result = sensitiveWordService.getHitWords("hello bad world");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).containsExactly("bad");
    }

    @Test
    @DisplayName("replaceContent - 空内容应返回原文")
    void replaceContent_blank_shouldReturnOriginal() {
        Result<String> result = sensitiveWordService.replaceContent("");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEmpty();
    }

    @Test
    @DisplayName("replaceContent - 命中敏感词应返回替换后内容")
    void replaceContent_hit_shouldReturnReplaced() {
        when(sensitiveWordFilter.replaceSensitiveWords("bad text")).thenReturn("* text");

        Result<String> result = sensitiveWordService.replaceContent("bad text");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo("* text");
    }

    @Test
    @DisplayName("reloadCache - 应重载缓存并返回成功")
    void reloadCache_shouldReloadAndReturnSuccess() {
        Result<Void> result = sensitiveWordService.reloadCache();

        assertThat(result.isSuccess()).isTrue();
        verify(sensitiveWordFilter, times(1)).reloadSensitiveWords();
    }

    // ==================== addWord ====================

    @Test
    @DisplayName("添加敏感词 - 已存在应返回错误")
    void addWord_alreadyExists_shouldReturnError() {
        SensitiveWordCreateDTO dto = new SensitiveWordCreateDTO();
        dto.setWord("bad");
        when(sensitiveWordMapper.existsSensitiveWord("bad")).thenReturn(true);

        Result<Long> result = sensitiveWordService.addWord(dto);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("敏感词已存在");
        verify(sensitiveWordMapper, never()).insert(any());
    }

    @Test
    @DisplayName("添加敏感词 - 插入成功应返回ID")
    void addWord_insertSuccess_shouldReturnId() {
        SensitiveWordCreateDTO dto = new SensitiveWordCreateDTO();
        dto.setWord("newbad");
        when(sensitiveWordMapper.existsSensitiveWord("newbad")).thenReturn(false);
        when(sensitiveWordMapper.insert(any(SensitiveWord.class))).thenAnswer(invocation -> {
            SensitiveWord w = invocation.getArgument(0);
            w.setId(1L);
            return 1;
        });

        Result<Long> result = sensitiveWordService.addWord(dto);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(1L);
        verify(sensitiveWordFilter, times(1)).reloadSensitiveWords();
    }

    @Test
    @DisplayName("添加敏感词 - 插入失败应返回错误")
    void addWord_insertFailed_shouldReturnError() {
        SensitiveWordCreateDTO dto = new SensitiveWordCreateDTO();
        dto.setWord("newbad");
        when(sensitiveWordMapper.existsSensitiveWord("newbad")).thenReturn(false);
        when(sensitiveWordMapper.insert(any(SensitiveWord.class))).thenReturn(0);

        Result<Long> result = sensitiveWordService.addWord(dto);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("添加敏感词失败");
    }

    @Test
    @DisplayName("添加敏感词 - 发生异常应返回错误")
    void addWord_exception_shouldReturnError() {
        SensitiveWordCreateDTO dto = new SensitiveWordCreateDTO();
        dto.setWord("newbad");
        when(sensitiveWordMapper.existsSensitiveWord("newbad")).thenThrow(new RuntimeException("db error"));

        Result<Long> result = sensitiveWordService.addWord(dto);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("添加敏感词失败");
    }

    // ==================== deleteWord ====================

    @Test
    @DisplayName("删除敏感词 - 不存在应返回错误")
    void deleteWord_notExists_shouldReturnError() {
        when(sensitiveWordMapper.selectById(99L)).thenReturn(null);

        Result<Void> result = sensitiveWordService.deleteWord(99L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("敏感词不存在");
        verify(sensitiveWordMapper, never()).deleteById(any());
    }

    @Test
    @DisplayName("删除敏感词 - 删除成功应重载缓存")
    void deleteWord_deleted_shouldReloadCache() {
        SensitiveWord word = new SensitiveWord();
        word.setId(1L);
        when(sensitiveWordMapper.selectById(1L)).thenReturn(word);
        when(sensitiveWordMapper.deleteById(1L)).thenReturn(1);

        Result<Void> result = sensitiveWordService.deleteWord(1L);

        assertThat(result.isSuccess()).isTrue();
        verify(sensitiveWordFilter, times(1)).reloadSensitiveWords();
    }

    @Test
    @DisplayName("删除敏感词 - 删除失败应返回错误")
    void deleteWord_deleteFailed_shouldReturnError() {
        SensitiveWord word = new SensitiveWord();
        word.setId(1L);
        when(sensitiveWordMapper.selectById(1L)).thenReturn(word);
        when(sensitiveWordMapper.deleteById(1L)).thenReturn(0);

        Result<Void> result = sensitiveWordService.deleteWord(1L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("删除敏感词失败");
    }

    // ==================== batchDeleteWords ====================

    @Test
    @DisplayName("批量删除敏感词 - 空列表应返回成功")
    void batchDeleteWords_empty_shouldReturnSuccess() {
        Result<Void> result = sensitiveWordService.batchDeleteWords(Collections.emptyList());

        assertThat(result.isSuccess()).isTrue();
        verify(sensitiveWordMapper, never()).deleteBatchIds(any());
    }

    @Test
    @DisplayName("批量删除敏感词 - 删除成功应重载缓存")
    void batchDeleteWords_deleted_shouldReloadCache() {
        when(sensitiveWordMapper.deleteBatchIds(anyList())).thenReturn(3);

        Result<Void> result = sensitiveWordService.batchDeleteWords(List.of(1L, 2L, 3L));

        assertThat(result.isSuccess()).isTrue();
        verify(sensitiveWordFilter, times(1)).reloadSensitiveWords();
    }

    @Test
    @DisplayName("批量删除敏感词 - 删除失败应返回错误")
    void batchDeleteWords_deleteFailed_shouldReturnError() {
        when(sensitiveWordMapper.deleteBatchIds(anyList())).thenReturn(0);

        Result<Void> result = sensitiveWordService.batchDeleteWords(List.of(1L, 2L));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("批量删除敏感词失败");
    }

    // ==================== updateWord ====================

    @Test
    @DisplayName("更新敏感词 - 不存在应返回错误")
    void updateWord_notExists_shouldReturnError() {
        SensitiveWordCreateDTO dto = new SensitiveWordCreateDTO();
        dto.setWord("newbad");
        when(sensitiveWordMapper.selectById(99L)).thenReturn(null);

        Result<Void> result = sensitiveWordService.updateWord(99L, dto);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("敏感词不存在");
    }

    @Test
    @DisplayName("更新敏感词 - 与其他词同名应返回错误")
    void updateWord_duplicateName_shouldReturnError() {
        SensitiveWordCreateDTO dto = new SensitiveWordCreateDTO();
        dto.setWord("dup");
        SensitiveWord word = new SensitiveWord();
        word.setId(1L);
        when(sensitiveWordMapper.selectById(1L)).thenReturn(word);
        when(sensitiveWordMapper.exists(any())).thenReturn(true);

        Result<Void> result = sensitiveWordService.updateWord(1L, dto);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("敏感词已存在");
    }

    @Test
    @DisplayName("更新敏感词 - 更新成功应重载缓存")
    void updateWord_updated_shouldReloadCache() {
        SensitiveWordCreateDTO dto = new SensitiveWordCreateDTO();
        dto.setWord("newbad");
        SensitiveWord word = new SensitiveWord();
        word.setId(1L);
        when(sensitiveWordMapper.selectById(1L)).thenReturn(word);
        when(sensitiveWordMapper.exists(any())).thenReturn(false);
        when(sensitiveWordMapper.updateById(any(SensitiveWord.class))).thenReturn(1);

        Result<Void> result = sensitiveWordService.updateWord(1L, dto);

        assertThat(result.isSuccess()).isTrue();
        verify(sensitiveWordFilter, times(1)).reloadSensitiveWords();
    }

    @Test
    @DisplayName("更新敏感词 - 更新失败应返回错误")
    void updateWord_updateFailed_shouldReturnError() {
        SensitiveWordCreateDTO dto = new SensitiveWordCreateDTO();
        dto.setWord("newbad");
        SensitiveWord word = new SensitiveWord();
        word.setId(1L);
        when(sensitiveWordMapper.selectById(1L)).thenReturn(word);
        when(sensitiveWordMapper.exists(any())).thenReturn(false);
        when(sensitiveWordMapper.updateById(any(SensitiveWord.class))).thenReturn(0);

        Result<Void> result = sensitiveWordService.updateWord(1L, dto);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("更新敏感词失败");
    }

    // ==================== getWordList ====================

    @Test
    @DisplayName("查询敏感词列表 - 应返回分页结果")
    void getWordList_shouldReturnPagedResult() {
        when(sensitiveWordMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            com.baomidou.mybatisplus.core.metadata.IPage<SensitiveWord> page = invocation.getArgument(0);
            page.setRecords(new ArrayList<>());
            page.setTotal(0L);
            return page;
        });

        Result<PageResult<SensitiveWordDTO>> result = sensitiveWordService.getWordList(1, 10, null, null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
    }

    @Test
    @DisplayName("查询敏感词列表 - 发生异常应返回错误")
    void getWordList_exception_shouldReturnError() {
        when(sensitiveWordMapper.selectPage(any(), any())).thenThrow(new RuntimeException("db error"));

        Result<PageResult<SensitiveWordDTO>> result = sensitiveWordService.getWordList(1, 10, null, null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("查询敏感词列表失败");
    }

    // ==================== batchImport ====================

    @Test
    @DisplayName("批量导入敏感词 - 空列表应返回0")
    void batchImport_empty_shouldReturnZero() {
        Result<Integer> result = sensitiveWordService.batchImport(Collections.emptyList(), "cat", 1);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(0);
        verify(sensitiveWordMapper, never()).insert(any());
    }

    @Test
    @DisplayName("批量导入敏感词 - 应跳过空白和重复词")
    void batchImport_shouldSkipBlankAndDuplicates() {
        when(sensitiveWordMapper.existsSensitiveWord("dup")).thenReturn(true);
        when(sensitiveWordMapper.existsSensitiveWord("new1")).thenReturn(false);
        when(sensitiveWordMapper.insert(any(SensitiveWord.class))).thenAnswer(invocation -> {
            SensitiveWord w = invocation.getArgument(0);
            w.setId(1L);
            return 1;
        });

        Result<Integer> result = sensitiveWordService.batchImport(List.of("dup", " ", "new1"), "cat", 2);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(1);
    }

    @Test
    @DisplayName("批量导入敏感词 - 发生异常应返回错误")
    void batchImport_exception_shouldReturnError() {
        when(sensitiveWordMapper.existsSensitiveWord(anyString())).thenThrow(new RuntimeException("db error"));

        Result<Integer> result = sensitiveWordService.batchImport(List.of("bad"), "cat", 1);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("批量导入失败");
    }
}
