package com.blog.service.impl;

import com.blog.entity.Article;
import com.blog.mapper.ArticleMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
                any(), any(), any(), any(), any(), any()))
                .thenReturn(new Page<Article>(1, 10));

        articleQueryService.getArticleList(1, 10, "keyword", null, null,
                Article.STATUS_DRAFT, null, "latest");

        verify(articleMapper).selectPublishedByFulltext(
                any(), eq(Article.STATUS_PUBLISHED), eq("keyword"),
                eq(null), eq(null), eq(null));
    }
}
