package com.blog.service.impl;

import com.blog.common.Result;
import com.blog.dto.ArticleStatisticsDTO;
import com.blog.entity.Article;
import com.blog.mapper.ArticleMapper;
import com.blog.utils.AuthUtils;
import com.blog.utils.RedisCacheUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ArticleStatisticsStatusGateTest {

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private RedisCacheUtils redisCacheUtils;

    @InjectMocks
    private ArticleStatisticsServiceImpl service;

    private Article draftArticle() {
        Article article = new Article();
        article.setId(1L);
        article.setAuthorId(7L);
        article.setStatus(Article.STATUS_DRAFT);
        article.setViewCount(100);
        return article;
    }

    @Test
    void getArticleStatistics_draftArticle_anonymous_shouldBeRejected() {
        when(articleMapper.selectById(1L)).thenReturn(draftArticle());

        Result<ArticleStatisticsDTO> result = service.getArticleStatistics(1L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("文章未发布或已删除");
    }

    @Test
    void getArticleStatistics_draftArticle_author_shouldBeAllowed() {
        when(articleMapper.selectById(1L)).thenReturn(draftArticle());
        when(redisCacheUtils.getArticleRedisViewCount(1L)).thenReturn(0);
        try (MockedStatic<AuthUtils> auth = Mockito.mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserIdOptional).thenReturn(7L);
            auth.when(AuthUtils::isAdmin).thenReturn(false);

            Result<ArticleStatisticsDTO> result = service.getArticleStatistics(1L);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isNotNull();
        }
    }

    @Test
    void getArticleStatistics_draftArticle_admin_shouldBeAllowed() {
        when(articleMapper.selectById(1L)).thenReturn(draftArticle());
        when(redisCacheUtils.getArticleRedisViewCount(1L)).thenReturn(0);
        try (MockedStatic<AuthUtils> auth = Mockito.mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserIdOptional).thenReturn(99L);
            auth.when(AuthUtils::isAdmin).thenReturn(true);

            Result<ArticleStatisticsDTO> result = service.getArticleStatistics(1L);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isNotNull();
        }
    }
}
