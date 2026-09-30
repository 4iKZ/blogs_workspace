package com.blog.service.impl;

import com.blog.entity.Article;
import com.blog.mapper.ArticleMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * searchByAuthor 修复（#10）的定向测试：
 * sortBy 必须透传给 selectByAuthorIdWithSort，pageSize 必须钳制在 100。
 */
class ArticleSearchServiceMediumFixTest {

    private final ArticleSearchServiceImpl service = new ArticleSearchServiceImpl();

    private ArticleMapper stubbedMapper() {
        ArticleMapper mapper = mock(ArticleMapper.class);
        when(mapper.selectByAuthorIdWithSort(any(), any(), any(), any())).thenReturn(List.of());
        setField(service, "articleMapper", mapper);
        com.blog.utils.RedisCacheUtils cacheUtils = mock(com.blog.utils.RedisCacheUtils.class);
        setField(service, "redisCacheUtils", cacheUtils);
        return mapper;
    }

    @Test
    void searchByAuthor_sortByView_shouldPassViewToMapper() {
        ArticleMapper mapper = stubbedMapper();

        var result = service.searchByAuthor(1L, 1, 10, "view");

        assertThat(result.isSuccess()).isTrue();
        verify(mapper, times(1)).selectByAuthorIdWithSort(eq(1L), eq(0), eq(10), eq("view"));
    }

    @Test
    void searchByAuthor_sortByOther_shouldPassThrough() {
        ArticleMapper mapper = stubbedMapper();

        var result = service.searchByAuthor(1L, 2, 10, "latest");

        assertThat(result.isSuccess()).isTrue();
        verify(mapper, times(1)).selectByAuthorIdWithSort(eq(1L), eq(10), eq(10), eq("latest"));
    }

    @Test
    void searchByAuthor_nullSortBy_shouldPassNullToMapper() {
        ArticleMapper mapper = stubbedMapper();

        var result = service.searchByAuthor(1L, 1, 10, null);

        assertThat(result.isSuccess()).isTrue();
        verify(mapper, times(1)).selectByAuthorIdWithSort(eq(1L), eq(0), eq(10), eq(null));
    }

    @Test
    void searchByAuthor_oversizedPage_shouldClampTo100() {
        ArticleMapper mapper = stubbedMapper();

        var result = service.searchByAuthor(1L, 1, 500, "view");

        assertThat(result.isSuccess()).isTrue();
        verify(mapper, times(1)).selectByAuthorIdWithSort(eq(1L), eq(0), eq(100), eq("view"));
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
