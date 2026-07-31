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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.TransactionStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserLikeServiceImpl userLikeService;

    @BeforeEach
    void setUp() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
        TransactionSynchronizationManager.clear();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 1L);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
        TransactionSynchronizationManager.clear();
    }

    // ==================== likeArticle ====================

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
    @DisplayName("点赞文章 - 获取锁失败应返回错误")
    void likeArticle_lockFailed_shouldReturnError() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any())).thenReturn(null);

            Result<Long> result = userLikeService.likeArticle(1L);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("操作过于频繁，请稍后重试");
        }
    }

    @Test
    @DisplayName("点赞文章 - 已点赞应返回现有ID")
    void likeArticle_alreadyLiked_shouldReturnExistingId() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            Article article = new Article();
            article.setStatus(2);
            article.setAuthorId(2L);
            when(articleMapper.selectById(anyLong())).thenReturn(article);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any())).thenReturn("mock-lock-value");
            UserLike existing = new UserLike();
            existing.setId(10L);
            when(userLikeMapper.findByUserIdAndArticleId(1L, 1L)).thenReturn(existing);

            Result<Long> result = userLikeService.likeArticle(1L);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEqualTo(10L);
            verify(userLikeMapper, never()).insert(any());
        }
    }

    @Test
    @DisplayName("点赞文章 - 正常点赞应成功")
    void likeArticle_success_shouldReturnId() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            Article article = new Article();
            article.setStatus(2);
            article.setAuthorId(2L);
            when(articleMapper.selectById(anyLong())).thenReturn(article);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any())).thenReturn("mock-lock-value");
            when(userLikeMapper.findByUserIdAndArticleId(1L, 1L)).thenReturn(null);
            when(userLikeMapper.insert(any(UserLike.class))).thenAnswer(invocation -> {
                UserLike ul = invocation.getArgument(0);
                ul.setId(20L);
                return 1;
            });

            TransactionSynchronizationManager.initSynchronization();
            try {
                Result<Long> result = userLikeService.likeArticle(1L);

                assertThat(result.isSuccess()).isTrue();
                assertThat(result.getData()).isEqualTo(20L);
                verify(articleStatisticsService).incrementLikeCount(1L);
                verify(cacheUtils).deleteCacheWithDoubleDelete(anyString());
            } finally {
                TransactionSynchronizationManager.clear();
            }
        }
    }

    @Test
    @DisplayName("点赞文章 - 插入异常应返回错误")
    void likeArticle_exception_shouldReturnError() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            Article article = new Article();
            article.setStatus(2);
            article.setAuthorId(2L);
            when(articleMapper.selectById(anyLong())).thenReturn(article);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any())).thenReturn("mock-lock-value");
            when(userLikeMapper.findByUserIdAndArticleId(1L, 1L)).thenReturn(null);
            when(userLikeMapper.insert(any(UserLike.class))).thenThrow(new RuntimeException("db error"));

            try (MockedStatic<TransactionAspectSupport> txMocked = Mockito.mockStatic(TransactionAspectSupport.class)) {
                TransactionStatus mockStatus = mock(TransactionStatus.class);
                txMocked.when(TransactionAspectSupport::currentTransactionStatus).thenReturn(mockStatus);

                Result<Long> result = userLikeService.likeArticle(1L);

                assertThat(result.isSuccess()).isFalse();
                assertThat(result.getMessage()).isEqualTo("点赞文章失败");
            }
        }
    }

    @Test
    @DisplayName("点赞文章 - 自点赞应成功")
    void likeArticle_selfLike_shouldSucceed() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            Article article = new Article();
            article.setStatus(2);
            article.setAuthorId(1L);
            when(articleMapper.selectById(anyLong())).thenReturn(article);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any())).thenReturn("mock-lock-value");
            when(userLikeMapper.findByUserIdAndArticleId(1L, 1L)).thenReturn(null);
            when(userLikeMapper.insert(any(UserLike.class))).thenAnswer(invocation -> {
                UserLike ul = invocation.getArgument(0);
                ul.setId(20L);
                return 1;
            });

            TransactionSynchronizationManager.initSynchronization();
            try {
                Result<Long> result = userLikeService.likeArticle(1L);

                assertThat(result.isSuccess()).isTrue();
                assertThat(result.getData()).isEqualTo(20L);
            } finally {
                TransactionSynchronizationManager.clear();
            }
        }
    }

    @Test
    @DisplayName("点赞文章 - authorId为null应成功")
    void likeArticle_authorIdNull_shouldSucceed() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            Article article = new Article();
            article.setStatus(2);
            article.setAuthorId(null);
            when(articleMapper.selectById(anyLong())).thenReturn(article);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any())).thenReturn("mock-lock-value");
            when(userLikeMapper.findByUserIdAndArticleId(1L, 1L)).thenReturn(null);
            when(userLikeMapper.insert(any(UserLike.class))).thenAnswer(invocation -> {
                UserLike ul = invocation.getArgument(0);
                ul.setId(20L);
                return 1;
            });

            TransactionSynchronizationManager.initSynchronization();
            try {
                Result<Long> result = userLikeService.likeArticle(1L);

                assertThat(result.isSuccess()).isTrue();
                assertThat(result.getData()).isEqualTo(20L);
            } finally {
                TransactionSynchronizationManager.clear();
            }
        }
    }

    @Test
    @DisplayName("点赞文章 - 通知事件发布失败不应影响主流程")
    void likeArticle_notificationPublishFailure_shouldStillSucceed() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            Article article = new Article();
            article.setStatus(2);
            article.setAuthorId(2L);
            when(articleMapper.selectById(anyLong())).thenReturn(article);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any())).thenReturn("mock-lock-value");
            when(userLikeMapper.findByUserIdAndArticleId(1L, 1L)).thenReturn(null);
            when(userLikeMapper.insert(any(UserLike.class))).thenAnswer(invocation -> {
                UserLike ul = invocation.getArgument(0);
                ul.setId(20L);
                return 1;
            });
            doThrow(new RuntimeException("event error")).when(eventPublisher).publishEvent(any());

            TransactionSynchronizationManager.initSynchronization();
            try {
                Result<Long> result = userLikeService.likeArticle(1L);

                assertThat(result.isSuccess()).isTrue();
                assertThat(result.getData()).isEqualTo(20L);
            } finally {
                TransactionSynchronizationManager.clear();
            }
        }
    }

    // ==================== unlikeArticle ====================

    @Test
    @DisplayName("取消点赞 - 获取锁失败应返回错误")
    void unlikeArticle_lockFailed_shouldReturnError() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any())).thenReturn(null);

            Result<Void> result = userLikeService.unlikeArticle(1L);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("操作过于频繁，请稍后重试");
        }
    }

    @Test
    @DisplayName("取消点赞 - 文章不存在也应成功（幂等）")
    void unlikeArticle_articleNotFound_shouldReturnSuccess() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any())).thenReturn("mock-lock-value");
            when(articleMapper.selectById(anyLong())).thenReturn(null);
            when(userLikeMapper.deleteByUserIdAndArticleId(1L, 1L)).thenReturn(0);

            Result<Void> result = userLikeService.unlikeArticle(1L);

            assertThat(result.isSuccess()).isTrue();
        }
    }

    @Test
    @DisplayName("取消点赞 - 删除成功应递减统计")
    void unlikeArticle_deleted_shouldDecrement() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            Article article = new Article();
            article.setAuthorId(2L);
            when(articleMapper.selectById(anyLong())).thenReturn(article);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any())).thenReturn("mock-lock-value");
            when(userLikeMapper.deleteByUserIdAndArticleId(1L, 1L)).thenReturn(1);

            TransactionSynchronizationManager.initSynchronization();
            try {
                Result<Void> result = userLikeService.unlikeArticle(1L);

                assertThat(result.isSuccess()).isTrue();
                verify(articleStatisticsService).decrementLikeCount(1L);
                verify(cacheUtils).deleteCacheWithDoubleDelete(anyString());
            } finally {
                TransactionSynchronizationManager.clear();
            }
        }
    }

    @Test
    @DisplayName("取消点赞 - 未找到记录应返回成功（幂等）")
    void unlikeArticle_noRecord_shouldReturnSuccess() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            Article article = new Article();
            article.setAuthorId(2L);
            when(articleMapper.selectById(anyLong())).thenReturn(article);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any())).thenReturn("mock-lock-value");
            when(userLikeMapper.deleteByUserIdAndArticleId(1L, 1L)).thenReturn(0);

            Result<Void> result = userLikeService.unlikeArticle(1L);

            assertThat(result.isSuccess()).isTrue();
            verify(articleStatisticsService, never()).decrementLikeCount(anyLong());
        }
    }

    @Test
    @DisplayName("取消点赞 - 删除异常应返回错误")
    void unlikeArticle_exception_shouldReturnError() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            Article article = new Article();
            article.setAuthorId(2L);
            when(articleMapper.selectById(anyLong())).thenReturn(article);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any())).thenReturn("mock-lock-value");
            when(userLikeMapper.deleteByUserIdAndArticleId(1L, 1L)).thenThrow(new RuntimeException("db error"));

            try (MockedStatic<TransactionAspectSupport> txMocked = Mockito.mockStatic(TransactionAspectSupport.class)) {
                TransactionStatus mockStatus = mock(TransactionStatus.class);
                txMocked.when(TransactionAspectSupport::currentTransactionStatus).thenReturn(mockStatus);

                Result<Void> result = userLikeService.unlikeArticle(1L);

                assertThat(result.isSuccess()).isFalse();
                assertThat(result.getMessage()).isEqualTo("取消点赞失败");
            }
        }
    }

    @Test
    @DisplayName("取消点赞 - 自己取消点赞自己的文章应成功")
    void unlikeArticle_selfUnlike_shouldSucceed() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            Article article = new Article();
            article.setAuthorId(1L);
            when(articleMapper.selectById(anyLong())).thenReturn(article);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any())).thenReturn("mock-lock-value");
            when(userLikeMapper.deleteByUserIdAndArticleId(1L, 1L)).thenReturn(1);

            TransactionSynchronizationManager.initSynchronization();
            try {
                Result<Void> result = userLikeService.unlikeArticle(1L);

                assertThat(result.isSuccess()).isTrue();
                verify(articleStatisticsService).decrementLikeCount(1L);
                verify(cacheUtils).deleteCacheWithDoubleDelete(anyString());
            } finally {
                TransactionSynchronizationManager.clear();
            }
        }
    }

    // ==================== isArticleLiked ====================

    @Test
    @DisplayName("检查是否点赞 - 缓存命中应直接返回")
    void isArticleLiked_cacheHit_shouldReturnCachedValue() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(redisCacheUtils.getCache(anyString())).thenReturn(Boolean.TRUE);

            Result<Boolean> result = userLikeService.isArticleLiked(1L);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isTrue();
            verify(userLikeMapper, never()).countByUserIdAndArticleId(anyLong(), anyLong());
        }
    }

    @Test
    @DisplayName("检查是否点赞 - 缓存命中 false 应直接返回")
    void isArticleLiked_cacheHitFalse_shouldReturnFalseDirectly() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(redisCacheUtils.getCache(anyString())).thenReturn(Boolean.FALSE);

            Result<Boolean> result = userLikeService.isArticleLiked(1L);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isFalse();
            verify(userLikeMapper, never()).countByUserIdAndArticleId(anyLong(), anyLong());
        }
    }

    @Test
    @DisplayName("检查是否点赞 - 未点赞应返回 false")
    void isArticleLiked_notLiked_shouldReturnFalse() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(redisCacheUtils.getCache(anyString())).thenReturn(null);
            when(userLikeMapper.countByUserIdAndArticleId(1L, 1L)).thenReturn(0);

            Result<Boolean> result = userLikeService.isArticleLiked(1L);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isFalse();
            verify(redisCacheUtils).setCache(anyString(), eq(false), anyLong(), any());
        }
    }

    @Test
    @DisplayName("检查是否点赞 - 发生异常应返回错误")
    void isArticleLiked_exception_shouldReturnError() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(redisCacheUtils.getCache(anyString())).thenThrow(new RuntimeException("redis error"));

            Result<Boolean> result = userLikeService.isArticleLiked(1L);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("检查点赞状态失败");
        }
    }

    // ==================== getUserLikes ====================

    @Test
    @DisplayName("获取点赞列表 - 参数过小应使用默认值")
    void getUserLikes_invalidPageSize_shouldUseDefault() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(userLikeMapper.selectByUserId(anyLong(), anyInt(), anyInt())).thenReturn(Collections.emptyList());
            when(userLikeMapper.countByUserId(anyLong())).thenReturn(0L);

            Result<PageResult<UserLikeDTO>> result = userLikeService.getUserLikes(0, 0);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isNotNull();
            verify(userLikeMapper).selectByUserId(eq(1L), eq(0), eq(10));
        }
    }

    @Test
    @DisplayName("获取点赞列表 - size过大应限制为100")
    void getUserLikes_sizeTooLarge_shouldCapTo100() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(userLikeMapper.selectByUserId(anyLong(), anyInt(), anyInt())).thenReturn(Collections.emptyList());
            when(userLikeMapper.countByUserId(anyLong())).thenReturn(0L);

            Result<PageResult<UserLikeDTO>> result = userLikeService.getUserLikes(1, 200);

            assertThat(result.isSuccess()).isTrue();
            verify(userLikeMapper).selectByUserId(eq(1L), eq(0), eq(100));
        }
    }

    @Test
    @DisplayName("获取点赞列表 - 发生异常应返回错误")
    void getUserLikes_exception_shouldReturnError() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(userLikeMapper.selectByUserId(anyLong(), anyInt(), anyInt())).thenThrow(new RuntimeException("db error"));

            Result<PageResult<UserLikeDTO>> result = userLikeService.getUserLikes(1, 10);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("获取点赞列表失败");
        }
    }

    @Test
    @DisplayName("获取点赞列表 - 有数据时应返回正确分页结果")
    void getUserLikes_successWithData_shouldReturnCorrectPageResult() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            UserLike userLike = new UserLike();
            userLike.setId(10L);
            userLike.setArticleId(100L);
            when(userLikeMapper.selectByUserId(eq(1L), anyInt(), anyInt())).thenReturn(List.of(userLike));
            when(userLikeMapper.countByUserId(1L)).thenReturn(1L);

            Result<PageResult<UserLikeDTO>> result = userLikeService.getUserLikes(1, 10);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isNotNull();
            assertThat(result.getData().getTotal()).isEqualTo(1);
            assertThat(result.getData().getItems()).hasSize(1);
        }
    }

    // ==================== getUserLikeCount ====================

    @Test
    @DisplayName("获取点赞数量 - 应返回统计值")
    void getUserLikeCount_shouldReturnCount() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(userLikeMapper.countByUserId(1L)).thenReturn(7L);

            Result<Integer> result = userLikeService.getUserLikeCount();

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEqualTo(7);
        }
    }

    @Test
    @DisplayName("获取点赞数量 - 发生异常应返回错误")
    void getUserLikeCount_exception_shouldReturnError() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(userLikeMapper.countByUserId(anyLong())).thenThrow(new RuntimeException("db error"));

            Result<Integer> result = userLikeService.getUserLikeCount();

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("获取点赞数量失败");
        }
    }
}
