package com.blog.service.impl;

import com.blog.dto.ArticleDTO;
import com.blog.entity.Article;
import com.blog.mapper.ArticleMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SearchServiceImplTest {

    private final SearchServiceImpl service = new SearchServiceImpl();

    @Test
    void searchArticles_nullKeyword_shouldReturnError() {
        var result = service.searchArticles(null, 1, 10);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("搜索关键词不能为空");
    }

    @Test
    void searchArticles_emptyKeyword_shouldReturnError() {
        var result = service.searchArticles("   ", 1, 10);

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void searchArticles_validKeyword_shouldReturnResults() {
        ArticleMapper mapper = mock(ArticleMapper.class);
        Article article = new Article();
        article.setId(1L);
        article.setTitle("Spring Boot Tutorial");
        when(mapper.searchByKeyword(eq("spring"), any(), any())).thenReturn(List.of(article));
        setField(service, "articleMapper", mapper);

        var result = service.searchArticles("spring", 1, 10);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getTitle()).isEqualTo("Spring Boot Tutorial");
    }

    @Test
    void searchArticles_exception_shouldReturnError() {
        ArticleMapper mapper = mock(ArticleMapper.class);
        when(mapper.searchByKeyword(any(), any(), any())).thenThrow(new RuntimeException("db error"));
        setField(service, "articleMapper", mapper);

        var result = service.searchArticles("spring", 1, 10);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("搜索文章失败");
    }

    @Test
    void searchArticlesByCategory_nullCategoryId_shouldReturnError() {
        var result = service.searchArticlesByCategory(null, 1, 10);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("分类ID不能为空");
    }

    @Test
    void searchArticlesByTag_nullTagId_shouldReturnError() {
        var result = service.searchArticlesByTag(null, 1, 10);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("标签ID不能为空");
    }

    @Test
    void searchArticlesByAuthor_nullAuthorId_shouldReturnError() {
        var result = service.searchArticlesByAuthor(null, 1, 10);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("作者ID不能为空");
    }

    @Test
    void getSearchSuggestions_nullKeyword_shouldReturnEmpty() {
        var result = service.getSearchSuggestions(null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEmpty();
    }

    @Test
    void getSearchSuggestions_validKeyword_shouldReturnSuggestions() {
        ArticleMapper mapper = mock(ArticleMapper.class);
        when(mapper.getSearchSuggestions("spring")).thenReturn(List.of("spring boot", "spring mvc"));
        setField(service, "articleMapper", mapper);

        var result = service.getSearchSuggestions("spring");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).containsExactly("spring boot", "spring mvc");
    }

    @Test
    void getHotSearchKeywords_nullLimit_shouldDefaultTo10() {
        ArticleMapper mapper = mock(ArticleMapper.class);
        when(mapper.getHotSearchKeywords(10)).thenReturn(List.of("java", "python"));
        setField(service, "articleMapper", mapper);

        var result = service.getHotSearchKeywords(null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).containsExactly("java", "python");
    }

    @Test
    void advancedSearch_shouldPassParameters() {
        ArticleMapper mapper = mock(ArticleMapper.class);
        when(mapper.advancedSearch(anyString(), any(), any(), any(), anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(List.of());
        setField(service, "articleMapper", mapper);

        var result = service.advancedSearch("keyword", 1L, 2L, 3L, "2025-01-01", "2025-12-31", 1, 10);

        assertThat(result.isSuccess()).isTrue();
        verify(mapper, times(1)).advancedSearch(eq("keyword"), eq(1L), eq(2L), eq(3L),
                eq("all"), eq("time"), any(), any(), any(), any());
    }

    @Test
    void advancedSearch_withoutDates_shouldPassNull() {
        ArticleMapper mapper = mock(ArticleMapper.class);
        when(mapper.advancedSearch(anyString(), any(), any(), any(), anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(List.of());
        setField(service, "articleMapper", mapper);

        var result = service.advancedSearch("kw", null, null, null, null, null, 1, 10);

        assertThat(result.isSuccess()).isTrue();
        verify(mapper).advancedSearch(eq("kw"), isNull(), isNull(), isNull(),
                eq("all"), eq("time"), isNull(), isNull(), any(), any());
    }

    @Test
    void searchArticlesByCategory_success_shouldReturnResults() {
        ArticleMapper mapper = mock(ArticleMapper.class);
        Article article = new Article();
        article.setId(1L);
        article.setTitle("cat");
        when(mapper.selectByCategoryId(eq(3L), any(), any())).thenReturn(List.of(article));
        setField(service, "articleMapper", mapper);

        var result = service.searchArticlesByCategory(3L, 1, 10);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    void searchArticlesByCategory_exception_shouldReturnError() {
        ArticleMapper mapper = mock(ArticleMapper.class);
        when(mapper.selectByCategoryId(any(), any(), any())).thenThrow(new RuntimeException("db error"));
        setField(service, "articleMapper", mapper);

        var result = service.searchArticlesByCategory(1L, 1, 10);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("按分类搜索文章失败");
    }

    @Test
    void searchArticlesByTag_success_shouldReturnResults() {
        ArticleMapper mapper = mock(ArticleMapper.class);
        Article article = new Article();
        article.setId(1L);
        article.setTitle("tag");
        when(mapper.selectByTagId(eq(2L), any(), any())).thenReturn(List.of(article));
        setField(service, "articleMapper", mapper);

        var result = service.searchArticlesByTag(2L, 1, 10);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    void searchArticlesByTag_exception_shouldReturnError() {
        ArticleMapper mapper = mock(ArticleMapper.class);
        when(mapper.selectByTagId(any(), any(), any())).thenThrow(new RuntimeException("db error"));
        setField(service, "articleMapper", mapper);

        var result = service.searchArticlesByTag(1L, 1, 10);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("按标签搜索文章失败");
    }

    @Test
    void searchArticlesByAuthor_success_shouldReturnResults() {
        ArticleMapper mapper = mock(ArticleMapper.class);
        Article article = new Article();
        article.setId(1L);
        article.setTitle("author");
        when(mapper.selectByAuthorId(eq(5L), any(), any())).thenReturn(List.of(article));
        setField(service, "articleMapper", mapper);

        var result = service.searchArticlesByAuthor(5L, 1, 10);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    void searchArticlesByAuthor_exception_shouldReturnError() {
        ArticleMapper mapper = mock(ArticleMapper.class);
        when(mapper.selectByAuthorId(any(), any(), any())).thenThrow(new RuntimeException("db error"));
        setField(service, "articleMapper", mapper);

        var result = service.searchArticlesByAuthor(1L, 1, 10);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("按作者搜索文章失败");
    }

    @Test
    void getSearchSuggestions_exception_shouldReturnError() {
        ArticleMapper mapper = mock(ArticleMapper.class);
        when(mapper.getSearchSuggestions(anyString())).thenThrow(new RuntimeException("db error"));
        setField(service, "articleMapper", mapper);

        var result = service.getSearchSuggestions("spring");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("获取搜索建议失败");
    }

    @Test
    void getHotSearchKeywords_exception_shouldReturnError() {
        ArticleMapper mapper = mock(ArticleMapper.class);
        when(mapper.getHotSearchKeywords(anyInt())).thenThrow(new RuntimeException("db error"));
        setField(service, "articleMapper", mapper);

        var result = service.getHotSearchKeywords(5);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("获取热门搜索词失败");
    }

    private static void setField(SearchServiceImpl target, String fieldName, Object value) {
        try {
            var field = SearchServiceImpl.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
