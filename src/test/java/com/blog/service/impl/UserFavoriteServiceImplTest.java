package com.blog.service.impl;

import com.blog.common.Result;
import com.blog.entity.Article;
import com.blog.entity.UserFavorite;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.UserFavoriteMapper;
import com.blog.service.ArticleStatisticsService;
import com.blog.utils.AuthUtils;
import com.blog.utils.CacheUtils;
import com.blog.utils.RedisCacheUtils;
import com.blog.utils.RedisDistributedLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserFavoriteServiceImplTest {

    @Mock
    private UserFavoriteMapper userFavoriteMapper;

    @Mock
    private ArticleStatisticsService articleStatisticsService;

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private RedisDistributedLock redisDistributedLock;

    @Mock
    private RedisCacheUtils redisCacheUtils;

    @Mock
    private CacheUtils cacheUtils;

    @InjectMocks
    private UserFavoriteServiceImpl userFavoriteService;

    @Test
    @DisplayName("收藏文章 - 文章不存在应返回错误")
    void favoriteArticle_articleNotFound_shouldReturnError() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any())).thenReturn("mock-lock");
            when(articleMapper.selectById(anyLong())).thenReturn(null);

            Result<Long> result = userFavoriteService.favoriteArticle(999L);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("文章不存在");
        }
    }

    @Test
    @DisplayName("收藏文章 - 文章未发布应返回错误")
    void favoriteArticle_articleNotPublished_shouldReturnError() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any())).thenReturn("mock-lock");
            Article article = new Article();
            article.setStatus(0);
            when(articleMapper.selectById(anyLong())).thenReturn(article);

            Result<Long> result = userFavoriteService.favoriteArticle(1L);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("无法收藏未发布的文章");
        }
    }

    @Test
    @DisplayName("取消收藏 - 无记录应返回错误")
    void unfavoriteArticle_noRecord_shouldReturnError() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any())).thenReturn("mock-lock");
            when(articleMapper.selectById(anyLong())).thenReturn(new Article());
            when(userFavoriteMapper.deleteByUserIdAndArticleId(anyLong(), anyLong())).thenReturn(0);

            Result<Void> result = userFavoriteService.unfavoriteArticle(1L);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("未找到收藏记录");
        }
    }

    @Test
    @DisplayName("获取收藏数量 - 应返回数量")
    void getUserFavoriteCount_shouldReturnCount() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(userFavoriteMapper.countByUserId(anyLong())).thenReturn(5);

            Result<Integer> result = userFavoriteService.getUserFavoriteCount();

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEqualTo(5);
        }
    }
}
