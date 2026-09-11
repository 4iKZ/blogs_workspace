package com.blog.service;

import com.blog.entity.Article;
import com.blog.exception.BusinessException;
import com.blog.mapper.ArticleMapper;
import com.blog.utils.RedisUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleStatusTransitionServiceTest {
    @Mock private ArticleMapper articleMapper;
    @Mock private ArticleRankService articleRankService;
    @Mock private RedisUtils redisUtils;
    @InjectMocks private ArticleStatusTransitionService service;

    @Test
    void initializeDraftSetsDraftStatus() {
        Article article = new Article();
        article.setStatus(Article.STATUS_PUBLISHED);

        service.initializeDraft(article);

        assertThat(article.getStatus()).isEqualTo(Article.STATUS_DRAFT);
        verifyNoInteractions(articleMapper, articleRankService, redisUtils);
    }

    @Test
    void restoreStatusRestoresOriginalStatus() {
        Article article = new Article();
        article.setStatus(Article.STATUS_PUBLISHED);

        service.restoreStatus(article, Article.STATUS_DRAFT);

        assertThat(article.getStatus()).isEqualTo(Article.STATUS_DRAFT);
        verifyNoInteractions(articleMapper, articleRankService, redisUtils);
    }

    @Test
    void publishSetsPublishedStatusAndInitializesRank() {
        Article article = new Article();
        article.setId(7L);
        when(articleMapper.updateById(article)).thenReturn(1);

        service.publish(article);

        assertThat(article.getStatus()).isEqualTo(Article.STATUS_PUBLISHED);
        assertThat(article.getPublishTime()).isNotNull();
        verify(articleRankService).initializeArticle(7L);
        verifyNoInteractions(redisUtils);
    }

    @Test
    void publishWhenUpdateFailsThrowsAndSkipsRank() {
        Article article = new Article();
        article.setId(7L);
        when(articleMapper.updateById(article)).thenReturn(0);

        assertThatThrownBy(() -> service.publish(article))
                .isInstanceOf(BusinessException.class)
                .hasMessage("应用审核快照失败");

        verifyNoInteractions(articleRankService, redisUtils);
    }

    @Test
    void revertToDraftSetsDraftAndIgnoresUpdateResult() {
        Article article = new Article();
        article.setId(7L);
        article.setStatus(Article.STATUS_PUBLISHED);
        when(articleMapper.updateById(article)).thenReturn(0);

        service.revertToDraft(article);

        assertThat(article.getStatus()).isEqualTo(Article.STATUS_DRAFT);
        verify(articleMapper).updateById(article);
        verifyNoInteractions(articleRankService, redisUtils);
    }

    @Test
    void changeStatusByAdminRejectsPublishWithoutLoadingArticle() {
        assertThatThrownBy(() -> service.changeStatusByAdmin(1L, Article.STATUS_PUBLISHED))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文章发布必须通过审核决定");

        verifyNoInteractions(articleMapper, articleRankService, redisUtils);
    }

    @Test
    void changeStatusByAdminRejectsNullStatus() {
        assertThatThrownBy(() -> service.changeStatusByAdmin(1L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("无效的文章状态");

        verifyNoInteractions(articleMapper, articleRankService, redisUtils);
    }

    @Test
    void changeStatusByAdminRejectsUnknownStatus() {
        assertThatThrownBy(() -> service.changeStatusByAdmin(1L, 99))
                .isInstanceOf(BusinessException.class)
                .hasMessage("无效的文章状态");

        verifyNoInteractions(articleMapper, articleRankService, redisUtils);
    }

    @Test
    void changeStatusByAdminWhenArticleMissingThrows() {
        when(articleMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> service.changeStatusByAdmin(1L, Article.STATUS_DRAFT))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文章不存在");

        verifyNoInteractions(articleRankService, redisUtils);
    }

    @Test
    void changeStatusByAdminWhenUpdateFailsThrows() {
        Article article = new Article();
        article.setId(1L);
        article.setStatus(Article.STATUS_PUBLISHED);
        when(articleMapper.selectById(1L)).thenReturn(article);
        when(articleMapper.updateById(article)).thenReturn(0);

        assertThatThrownBy(() -> service.changeStatusByAdmin(1L, Article.STATUS_DRAFT))
                .isInstanceOf(BusinessException.class)
                .hasMessage("修改文章状态失败");

        verifyNoInteractions(articleRankService, redisUtils);
    }

    @Test
    void changeStatusByAdminToDraftRemovesRankAndClearsRecommendedCache() {
        Article article = new Article();
        article.setId(1L);
        article.setStatus(Article.STATUS_PUBLISHED);
        when(articleMapper.selectById(1L)).thenReturn(article);
        when(articleMapper.updateById(article)).thenReturn(1);
        Set<String> keys = Set.of("recommended:articles:1");
        when(redisUtils.scanKeys("recommended:articles:*")).thenReturn(keys);

        service.changeStatusByAdmin(1L, Article.STATUS_DRAFT);

        assertThat(article.getStatus()).isEqualTo(Article.STATUS_DRAFT);
        verify(articleRankService).removeFromRank(1L);
        verify(redisUtils).delete(keys);
    }

    @Test
    void changeStatusByAdminToDeletedRemovesRankAndClearsRecommendedCache() {
        Article article = new Article();
        article.setId(1L);
        article.setStatus(Article.STATUS_PUBLISHED);
        when(articleMapper.selectById(1L)).thenReturn(article);
        when(articleMapper.updateById(article)).thenReturn(1);
        Set<String> keys = Set.of("recommended:articles:2");
        when(redisUtils.scanKeys("recommended:articles:*")).thenReturn(keys);

        service.changeStatusByAdmin(1L, Article.STATUS_DELETED);

        assertThat(article.getStatus()).isEqualTo(Article.STATUS_DELETED);
        verify(articleRankService).removeFromRank(1L);
        verify(redisUtils).delete(keys);
    }

    @Test
    void changeStatusByAdminWhenRankFailsStillClearsRecommendedCache() {
        Article article = new Article();
        article.setId(1L);
        article.setStatus(Article.STATUS_PUBLISHED);
        when(articleMapper.selectById(1L)).thenReturn(article);
        when(articleMapper.updateById(article)).thenReturn(1);
        doThrow(new RuntimeException("redis down")).when(articleRankService).removeFromRank(1L);
        Set<String> keys = Set.of("recommended:articles:1");
        when(redisUtils.scanKeys("recommended:articles:*")).thenReturn(keys);

        service.changeStatusByAdmin(1L, Article.STATUS_DELETED);

        verify(redisUtils).delete(keys);
    }

    @Test
    void changeStatusByAdminWhenCacheClearFailsStillSucceeds() {
        Article article = new Article();
        article.setId(1L);
        article.setStatus(Article.STATUS_PUBLISHED);
        when(articleMapper.selectById(1L)).thenReturn(article);
        when(articleMapper.updateById(article)).thenReturn(1);
        when(redisUtils.scanKeys(anyString())).thenThrow(new RuntimeException("redis down"));

        service.changeStatusByAdmin(1L, Article.STATUS_DRAFT);

        assertThat(article.getStatus()).isEqualTo(Article.STATUS_DRAFT);
        verify(articleRankService).removeFromRank(1L);
    }

    @Test
    void changeStatusByAdminWithNullKeySetDoesNotDelete() {
        Article article = new Article();
        article.setId(1L);
        article.setStatus(Article.STATUS_PUBLISHED);
        when(articleMapper.selectById(1L)).thenReturn(article);
        when(articleMapper.updateById(article)).thenReturn(1);
        when(redisUtils.scanKeys("recommended:articles:*")).thenReturn(null);

        service.changeStatusByAdmin(1L, Article.STATUS_DRAFT);

        verify(redisUtils, never()).delete(anyCollection());
    }
}
