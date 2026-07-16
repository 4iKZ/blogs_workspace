package com.blog.service.impl;

import com.blog.dto.SearchResultDTO;
import com.blog.dto.SearchStatisticsDTO;
import com.blog.entity.Article;
import com.blog.mapper.ArticleMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ArticleSearchServiceImplTest {

    private final ArticleSearchServiceImpl service = new ArticleSearchServiceImpl();

    @Test
    void getSearchSuggestions_blankKeyword_shouldReturnEmpty() {
        var result = service.getSearchSuggestions("", 5);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEmpty();
    }

    @Test
    void getSearchSuggestions_validKeyword_shouldReturnSuggestions() {
        ArticleMapper mapper = mock(ArticleMapper.class);
        when(mapper.getSearchSuggestions("spring")).thenReturn(List.of("spring boot", "spring mvc"));
        setField(service, "articleMapper", mapper);

        var result = service.getSearchSuggestions("spring", 5);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).containsExactly("spring boot", "spring mvc");
    }

    @Test
    void getHotKeywords_nullLimit_shouldDefaultTo10() {
        ArticleMapper mapper = mock(ArticleMapper.class);
        when(mapper.getHotSearchKeywords(10)).thenReturn(List.of("java", "python"));
        setField(service, "articleMapper", mapper);

        var result = service.getHotKeywords(null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).containsExactly("java", "python");
        verify(mapper, times(1)).getHotSearchKeywords(10);
    }

    @Test
    void getSearchStatistics_shouldReturnTotal() {
        ArticleMapper mapper = mock(ArticleMapper.class);
        when(mapper.countAdvancedSearch(anyString(), any(), any(), any(), anyString(), any(), any()))
                .thenReturn(42L);
        setField(service, "articleMapper", mapper);

        var result = service.getSearchStatistics("spring", 1L, List.of(10L));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getTotalResults()).isEqualTo(42L);
        assertThat(result.getData().getKeyword()).isEqualTo("spring");
    }

    @Test
    void searchByAuthor_nullAuthorId_shouldReturnError() {
        var result = service.searchByAuthor(null, 1, 10, "newest");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("作者ID不能为空");
    }

    @Test
    void searchByAuthor_invalidPage_shouldDefaultTo1() {
        ArticleMapper mapper = mock(ArticleMapper.class);
        when(mapper.selectByAuthorId(any(), any(), any())).thenReturn(List.of());
        setField(service, "articleMapper", mapper);

        service.searchByAuthor(1L, 0, 10, "newest");

        verify(mapper, times(1)).selectByAuthorId(eq(1L), eq(0), any());
    }

    @Test
    void rebuildSearchIndex_shouldReturnSuccess() {
        var result = service.rebuildSearchIndex();

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void addArticleToIndex_shouldReturnSuccess() {
        var result = service.addArticleToIndex(1L);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void removeArticleFromIndex_shouldReturnSuccess() {
        var result = service.removeArticleFromIndex(1L);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void updateArticleInIndex_shouldReturnSuccess() {
        var result = service.updateArticleInIndex(1L);

        assertThat(result.isSuccess()).isTrue();
    }

    private static void setField(ArticleSearchServiceImpl target, String fieldName, Object value) {
        try {
            var field = ArticleSearchServiceImpl.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
