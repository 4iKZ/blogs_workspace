package com.blog.service.impl;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.dto.ArticleDTO;
import com.blog.dto.UserLikeDTO;
import com.blog.entity.Article;
import com.blog.entity.UserLike;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.UserLikeMapper;
import com.blog.service.ArticleRankService;
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

import java.util.List;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class UserLikeServiceImplTest {

    @Mock
    private UserLikeMapper userLikeMapper;

    @Mock
    private ArticleStatisticsService articleStatisticsService;

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private ArticleRankService articleRankService;

    @Mock
    private RedisDistributedLock redisDistributedLock;

    @Mock
    private RedisCacheUtils redisCacheUtils;

    @Mock
    private CacheUtils cacheUtils;

    @InjectMocks
    private UserLikeServiceImpl userLikeService;

    @Test
    @DisplayName("点赞文章 - 文章不存在应返回错误")
    void likeArticle_articleNotFound_shouldReturnError() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any())).thenReturn("mock-lock-value");
            when(articleMapper.selectById(anyLong())).thenReturn(null);

            Result<Long> result = userLikeService.likeArticle(999L);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("文章不存在");
        }
    }

    @Test
    @DisplayName("点赞文章 - 文章未发布应返回错误")
    void likeArticle_articleNotPublished_shouldReturnError() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            Article article = new Article();
            article.setStatus(0);
            when(articleMapper.selectById(anyLong())).thenReturn(article);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any())).thenReturn("mock-lock-value");

            Result<Long> result = userLikeService.likeArticle(1L);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("文章未发布，无法点赞");
        }
    }

    @Test
    @DisplayName("检查是否已点赞 - 未点赞应返回 false")
    void isArticleLiked_notLiked_shouldReturnFalse() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(userLikeMapper.countByUserIdAndArticleId(anyLong(), anyLong())).thenReturn(0);

            Result<Boolean> result = userLikeService.isArticleLiked(1L);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isFalse();
        }
    }

    @Test
    @DisplayName("获取点赞列表 - 应返回分页结果")
    void getUserLikes_shouldReturnPagedResult() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(userLikeMapper.selectByUserId(anyLong(), anyInt(), anyInt())).thenReturn(Collections.emptyList());
            when(userLikeMapper.countByUserId(anyLong())).thenReturn(0L);

            Result<PageResult<UserLikeDTO>> result = userLikeService.getUserLikes(1, 10);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isNotNull();
        }
    }
}
