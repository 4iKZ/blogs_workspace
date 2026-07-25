package com.blog.service.impl;

import com.blog.common.Result;
import com.blog.dto.ArticleCreateDTO;
import com.blog.entity.Article;
import com.blog.mapper.ArticleMapper;
import com.blog.service.UserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
public class ArticleServiceImplUnitTest {

    @InjectMocks
    private ArticleServiceImpl articleService;

    @Mock
    private UserService userService;

    @Mock
    private ArticleMapper articleMapper;

    @Test
    public void testPublishArticle_InvalidAuthor_ShouldFail() {
        // Arrange
        ArticleCreateDTO createDTO = new ArticleCreateDTO();
        createDTO.setTitle("Test Title");
        Long invalidAuthorId = 99999L;

        // Mock userService to return null for this ID
        when(userService.getUserById(invalidAuthorId)).thenReturn(null);

        // Act
        Result<Long> result = articleService.publishArticle(createDTO, invalidAuthorId);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("作者不存在", result.getMessage());
    }

    @Test
    void publicArticleList_requestedDraftStatus_shouldStillQueryPublishedOnly() {
        when(articleMapper.selectPublishedByFulltext(
                any(), any(), any(), any(), any(), any()))
                .thenReturn(new Page<Article>(1, 10));

        articleService.getArticleList(1, 10, "keyword", null, null,
                Article.STATUS_DRAFT, null, "latest");

        verify(articleMapper).selectPublishedByFulltext(
                any(), eq(Article.STATUS_PUBLISHED), eq("keyword"),
                eq(null), eq(null), eq(null));
    }
}
