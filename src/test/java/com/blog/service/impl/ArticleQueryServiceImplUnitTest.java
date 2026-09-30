package com.blog.service.impl;

import com.blog.entity.Article;
import com.blog.mapper.ArticleMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
public class ArticleQueryServiceImplUnitTest {

    @InjectMocks
    private ArticleQueryServiceImpl articleQueryService;

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private ArticleDtoAssembler articleDtoAssembler;

    @Test
    void publicArticleList_requestedDraftStatus_shouldStillQueryPublishedOnly() {
        when(articleMapper.selectPublishedByFulltext(
                any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new Page<Article>(1, 10));

        articleQueryService.getArticleList(1, 10, "keyword", null, null,
                Article.STATUS_DRAFT, null, "latest");

        verify(articleMapper).selectPublishedByFulltext(
                any(), eq(Article.STATUS_PUBLISHED), eq("keyword"),
                eq(null), eq(null), eq(null), eq("latest"));
    }

    @Test
    void publicArticleList_keywordWithPopularSort_shouldPassSortByToFulltext() {
        when(articleMapper.selectPublishedByFulltext(
                any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new Page<Article>(1, 10));

        // #20: 全文搜索分支 previously 丢弃 popular 排序，sortBy 必须透传给 mapper（SQL 内 choose 切到 view_count DESC）
        articleQueryService.getArticleList(1, 10, "keyword", null, null,
                Article.STATUS_PUBLISHED, null, "popular");

        ArgumentCaptor<String> sortByCaptor = ArgumentCaptor.forClass(String.class);
        verify(articleMapper).selectPublishedByFulltext(
                any(), any(), any(), any(), any(), any(), sortByCaptor.capture());
        assertThat(sortByCaptor.getValue()).isEqualTo("popular");
    }

    @Test
    void publicArticleList_keywordWithLatestSort_shouldPassSortByToFulltext() {
        when(articleMapper.selectPublishedByFulltext(
                any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new Page<Article>(1, 10));

        articleQueryService.getArticleList(1, 10, "keyword", null, null,
                Article.STATUS_PUBLISHED, null, "latest");

        ArgumentCaptor<String> sortByCaptor = ArgumentCaptor.forClass(String.class);
        verify(articleMapper).selectPublishedByFulltext(
                any(), any(), any(), any(), any(), any(), sortByCaptor.capture());
        assertThat(sortByCaptor.getValue()).isEqualTo("latest");
    }
}
