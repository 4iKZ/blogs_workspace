package com.blog.service.impl;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.dto.UserFavoriteDTO;
import com.blog.entity.Article;
import com.blog.entity.UserFavorite;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.UserFavoriteMapper;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserFavoriteServiceImplTest {

    @Mock
    private UserFavoriteMapper userFavoriteMapper;

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
    private UserFavoriteServiceImpl userFavoriteService;

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

    // ==================== favoriteArticle ====================

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
    @DisplayName("收藏文章 - 获取锁失败应返回错误")
    void favoriteArticle_lockFailed_shouldReturnError() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any())).thenReturn(null);

            Result<Long> result = userFavoriteService.favoriteArticle(1L);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("操作过于频繁，请稍后重试");
        }
    }

    @Test
    @DisplayName("收藏文章 - 已收藏应返回现有ID")
    void favoriteArticle_alreadyFavorited_shouldReturnExistingId() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            Article article = new Article();
            article.setStatus(2);
            article.setAuthorId(2L);
            when(articleMapper.selectById(anyLong())).thenReturn(article);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any())).thenReturn("mock-lock");
            when(userFavoriteMapper.countByUserIdAndArticleId(1L, 1L)).thenReturn(1);
            UserFavorite existing = new UserFavorite();
            existing.setId(10L);
            when(userFavoriteMapper.selectByUserAndArticle(1L, 1L)).thenReturn(existing);

            Result<Long> result = userFavoriteService.favoriteArticle(1L);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEqualTo(10L);
            verify(userFavoriteMapper, never()).insert(any());
        }
    }

    @Test
    @DisplayName("收藏文章 - 正常收藏应成功并注册事务同步")
    void favoriteArticle_success_shouldFavoriteAndRegisterAfterCommit() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            Article article = new Article();
            article.setStatus(2);
            article.setAuthorId(2L);
            when(articleMapper.selectById(anyLong())).thenReturn(article);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any())).thenReturn("mock-lock");
            when(userFavoriteMapper.countByUserIdAndArticleId(1L, 1L)).thenReturn(0);
            when(userFavoriteMapper.insert(any(UserFavorite.class))).thenAnswer(invocation -> {
                UserFavorite uf = invocation.getArgument(0);
                uf.setId(20L);
                return 1;
            });

            TransactionSynchronizationManager.initSynchronization();
            try {
                Result<Long> result = userFavoriteService.favoriteArticle(1L);

                assertThat(result.isSuccess()).isTrue();
                assertThat(result.getData()).isEqualTo(20L);
                verify(articleStatisticsService).incrementFavoriteCount(1L);
                verify(cacheUtils).deleteCacheWithDoubleDelete(anyString());
            } finally {
                TransactionSynchronizationManager.clear();
            }
        }
    }

    @Test
    @DisplayName("收藏文章 - 自己收藏自己的文章应成功")
    void favoriteArticle_selfFavorite_shouldSucceed() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            Article article = new Article();
            article.setStatus(2);
            article.setAuthorId(1L);
            when(articleMapper.selectById(anyLong())).thenReturn(article);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any())).thenReturn("mock-lock");
            when(userFavoriteMapper.countByUserIdAndArticleId(1L, 1L)).thenReturn(0);
            when(userFavoriteMapper.insert(any(UserFavorite.class))).thenAnswer(invocation -> {
                UserFavorite uf = invocation.getArgument(0);
                uf.setId(20L);
                return 1;
            });

            TransactionSynchronizationManager.initSynchronization();
            try {
                Result<Long> result = userFavoriteService.favoriteArticle(1L);

                assertThat(result.isSuccess()).isTrue();
                assertThat(result.getData()).isEqualTo(20L);
                verify(articleStatisticsService).incrementFavoriteCount(1L);
                verify(cacheUtils).deleteCacheWithDoubleDelete(anyString());
            } finally {
                TransactionSynchronizationManager.clear();
            }
        }
    }

    @Test
    @DisplayName("收藏文章 - afterCommit更新热度分数异常应被捕获且主流程成功")
    void favoriteArticle_afterCommitIncrementFavoriteScoreThrows_shouldCatchAndNotFailMainFlow() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            Article article = new Article();
            article.setStatus(2);
            article.setAuthorId(2L);
            when(articleMapper.selectById(anyLong())).thenReturn(article);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any())).thenReturn("mock-lock");
            when(userFavoriteMapper.countByUserIdAndArticleId(1L, 1L)).thenReturn(0);
            when(userFavoriteMapper.insert(any(UserFavorite.class))).thenAnswer(invocation -> {
                UserFavorite uf = invocation.getArgument(0);
                uf.setId(20L);
                return 1;
            });

            TransactionSynchronizationManager.initSynchronization();
            try {
                Result<Long> result = userFavoriteService.favoriteArticle(1L);

                assertThat(result.isSuccess()).isTrue();
                assertThat(result.getData()).isEqualTo(20L);

                List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
                assertThat(synchronizations).hasSize(1);
                doThrow(new RuntimeException("rank error")).when(articleRankService).incrementFavoriteScore(any(Long.class), any(Long.class), any(Long.class));
                synchronizations.get(0).afterCommit();

                verify(articleStatisticsService).incrementFavoriteCount(1L);
                verify(cacheUtils).deleteCacheWithDoubleDelete(anyString());
            } finally {
                TransactionSynchronizationManager.clear();
            }
        }
    }

    @Test
    @DisplayName("收藏文章 - 发生异常应返回错误")
    void favoriteArticle_exception_shouldReturnError() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            Article article = new Article();
            article.setStatus(2);
            when(articleMapper.selectById(anyLong())).thenReturn(article);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any())).thenReturn("mock-lock");
            when(userFavoriteMapper.countByUserIdAndArticleId(1L, 1L)).thenThrow(new RuntimeException("db error"));

            Result<Long> result = userFavoriteService.favoriteArticle(1L);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("收藏文章失败");
        }
    }

    // ==================== unfavoriteArticle ====================

    @Test
    @DisplayName("取消收藏 - 获取锁失败应返回错误")
    void unfavoriteArticle_lockFailed_shouldReturnError() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any())).thenReturn(null);

            Result<Void> result = userFavoriteService.unfavoriteArticle(1L);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("操作过于频繁，请稍后重试");
        }
    }

    @Test
    @DisplayName("取消收藏 - 文章不存在且无记录应返回错误")
    void unfavoriteArticle_articleNotFound_shouldReturnError() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any())).thenReturn("mock-lock");
            when(articleMapper.selectById(999L)).thenReturn(null);
            when(userFavoriteMapper.deleteByUserIdAndArticleId(1L, 999L)).thenReturn(0);

            Result<Void> result = userFavoriteService.unfavoriteArticle(999L);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("未找到收藏记录");
        }
    }

    @Test
    @DisplayName("取消收藏 - 删除成功应递减统计并注册事务同步")
    void unfavoriteArticle_deleted_shouldDecrementAndRegisterAfterCommit() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            Article article = new Article();
            article.setAuthorId(2L);
            when(articleMapper.selectById(1L)).thenReturn(article);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any())).thenReturn("mock-lock");
            when(userFavoriteMapper.deleteByUserIdAndArticleId(1L, 1L)).thenReturn(1);

            TransactionSynchronizationManager.initSynchronization();
            try {
                Result<Void> result = userFavoriteService.unfavoriteArticle(1L);

                assertThat(result.isSuccess()).isTrue();
                verify(articleStatisticsService).decrementFavoriteCount(1L);
                verify(cacheUtils).deleteCacheWithDoubleDelete(anyString());
            } finally {
                TransactionSynchronizationManager.clear();
            }
        }
    }

    @Test
    @DisplayName("取消收藏 - 自己取消收藏自己的文章应成功")
    void unfavoriteArticle_selfUnfavorite_shouldSucceed() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            Article article = new Article();
            article.setAuthorId(1L);
            when(articleMapper.selectById(1L)).thenReturn(article);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any())).thenReturn("mock-lock");
            when(userFavoriteMapper.deleteByUserIdAndArticleId(1L, 1L)).thenReturn(1);

            TransactionSynchronizationManager.initSynchronization();
            try {
                Result<Void> result = userFavoriteService.unfavoriteArticle(1L);

                assertThat(result.isSuccess()).isTrue();
                verify(articleStatisticsService).decrementFavoriteCount(1L);
                verify(cacheUtils).deleteCacheWithDoubleDelete(anyString());
            } finally {
                TransactionSynchronizationManager.clear();
            }
        }
    }

    @Test
    @DisplayName("取消收藏 - afterCommit更新热度分数异常应被捕获且主流程成功")
    void unfavoriteArticle_afterCommitDecrementFavoriteScoreThrows_shouldCatchAndNotFailMainFlow() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            Article article = new Article();
            article.setAuthorId(2L);
            when(articleMapper.selectById(1L)).thenReturn(article);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any())).thenReturn("mock-lock");
            when(userFavoriteMapper.deleteByUserIdAndArticleId(1L, 1L)).thenReturn(1);

            TransactionSynchronizationManager.initSynchronization();
            try {
                Result<Void> result = userFavoriteService.unfavoriteArticle(1L);

                assertThat(result.isSuccess()).isTrue();

                List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
                assertThat(synchronizations).hasSize(1);
                doThrow(new RuntimeException("rank error")).when(articleRankService).decrementFavoriteScore(anyLong(), anyLong(), anyLong());
                synchronizations.get(0).afterCommit();

                verify(articleStatisticsService).decrementFavoriteCount(1L);
                verify(cacheUtils).deleteCacheWithDoubleDelete(anyString());
            } finally {
                TransactionSynchronizationManager.clear();
            }
        }
    }

    @Test
    @DisplayName("取消收藏 - 未找到记录应返回错误")
    void unfavoriteArticle_noRecord_shouldReturnError() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            Article article = new Article();
            article.setAuthorId(2L);
            when(articleMapper.selectById(1L)).thenReturn(article);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any())).thenReturn("mock-lock");
            when(userFavoriteMapper.deleteByUserIdAndArticleId(1L, 1L)).thenReturn(0);

            Result<Void> result = userFavoriteService.unfavoriteArticle(1L);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("未找到收藏记录");
        }
    }

    @Test
    @DisplayName("取消收藏 - 发生异常应返回错误")
    void unfavoriteArticle_exception_shouldReturnError() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            Article article = new Article();
            article.setAuthorId(2L);
            when(articleMapper.selectById(1L)).thenReturn(article);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any())).thenReturn("mock-lock");
            when(userFavoriteMapper.deleteByUserIdAndArticleId(1L, 1L)).thenThrow(new RuntimeException("db error"));

            Result<Void> result = userFavoriteService.unfavoriteArticle(1L);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("取消收藏失败");
        }
    }

    // ==================== getUserFavorites ====================

    @Test
    @DisplayName("获取收藏列表 - 应返回分页结果")
    void getUserFavorites_shouldReturnPagedResult() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(userFavoriteMapper.countByUserId(1L)).thenReturn(2);
            when(userFavoriteMapper.selectByUserId(anyLong(), anyInt(), anyInt())).thenReturn(Collections.emptyList());

            Result<PageResult<UserFavoriteDTO>> result = userFavoriteService.getUserFavorites(1, 10);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isNotNull();
            assertThat(result.getData().getTotal()).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("获取收藏列表 - 有数据时应返回正确的分页结果")
    void getUserFavorites_withData_shouldReturnCorrectPageResult() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(userFavoriteMapper.countByUserId(1L)).thenReturn(2);

            UserFavorite favorite1 = new UserFavorite();
            favorite1.setId(10L);
            favorite1.setUserId(1L);
            favorite1.setArticleId(100L);
            favorite1.setCreateTime(LocalDateTime.now());

            UserFavorite favorite2 = new UserFavorite();
            favorite2.setId(11L);
            favorite2.setUserId(1L);
            favorite2.setArticleId(101L);
            favorite2.setCreateTime(LocalDateTime.now());

            when(userFavoriteMapper.selectByUserId(1L, 0, 10)).thenReturn(List.of(favorite1, favorite2));

            Article article1 = new Article();
            article1.setId(100L);
            article1.setTitle("Article 1");
            article1.setStatus(2);
            article1.setAuthorId(2L);

            Article article2 = new Article();
            article2.setId(101L);
            article2.setTitle("Article 2");
            article2.setStatus(2);
            article2.setAuthorId(3L);

            when(articleMapper.selectById(100L)).thenReturn(article1);
            when(articleMapper.selectById(101L)).thenReturn(article2);

            Result<PageResult<UserFavoriteDTO>> result = userFavoriteService.getUserFavorites(1, 10);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isNotNull();
            assertThat(result.getData().getTotal()).isEqualTo(2);
            assertThat(result.getData().getItems()).hasSize(2);
            assertThat(result.getData().getItems().get(0).getFavoriteId()).isEqualTo(10L);
            assertThat(result.getData().getItems().get(0).getArticle()).isNotNull();
            assertThat(result.getData().getItems().get(0).getArticle().getTitle()).isEqualTo("Article 1");
        }
    }

    @Test
    @DisplayName("获取收藏列表 - 空列表应返回空items但正确的total")
    void getUserFavorites_emptyList_shouldReturnEmptyItemsWithCorrectTotal() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(userFavoriteMapper.countByUserId(1L)).thenReturn(0);
            when(userFavoriteMapper.selectByUserId(1L, 0, 10)).thenReturn(Collections.emptyList());

            Result<PageResult<UserFavoriteDTO>> result = userFavoriteService.getUserFavorites(1, 10);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isNotNull();
            assertThat(result.getData().getTotal()).isEqualTo(0);
            assertThat(result.getData().getItems()).isEmpty();
        }
    }

    @Test
    @DisplayName("获取收藏列表 - 发生异常应返回错误")
    void getUserFavorites_exception_shouldReturnError() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(userFavoriteMapper.countByUserId(anyLong())).thenThrow(new RuntimeException("db error"));

            Result<PageResult<UserFavoriteDTO>> result = userFavoriteService.getUserFavorites(1, 10);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("获取收藏列表失败");
        }
    }

    @Test
    @DisplayName("获取收藏列表 - 文章不存在应优雅处理并返回DTO")
    void getUserFavorites_articleNotFound_shouldHandleGracefully() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(userFavoriteMapper.countByUserId(1L)).thenReturn(1);

            UserFavorite favorite = new UserFavorite();
            favorite.setId(10L);
            favorite.setUserId(1L);
            favorite.setArticleId(100L);
            favorite.setCreateTime(LocalDateTime.now());
            when(userFavoriteMapper.selectByUserId(1L, 0, 10)).thenReturn(List.of(favorite));
            when(articleMapper.selectById(100L)).thenReturn(null);

            Result<PageResult<UserFavoriteDTO>> result = userFavoriteService.getUserFavorites(1, 10);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isNotNull();
            assertThat(result.getData().getItems()).hasSize(1);
            assertThat(result.getData().getItems().get(0).getFavoriteId()).isEqualTo(10L);
            assertThat(result.getData().getItems().get(0).getArticle()).isNull();
        }
    }

    @Test
    @DisplayName("获取收藏列表 - 文章存在应在DTO中设置文章信息")
    void getUserFavorites_articleFound_shouldSetArticleInfoInDTO() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(userFavoriteMapper.countByUserId(1L)).thenReturn(1);

            UserFavorite favorite = new UserFavorite();
            favorite.setId(10L);
            favorite.setUserId(1L);
            favorite.setArticleId(100L);
            favorite.setCreateTime(LocalDateTime.now());
            when(userFavoriteMapper.selectByUserId(1L, 0, 10)).thenReturn(List.of(favorite));

            Article article = new Article();
            article.setId(100L);
            article.setTitle("Test Article");
            article.setStatus(2);
            article.setAuthorId(2L);
            when(articleMapper.selectById(100L)).thenReturn(article);

            Result<PageResult<UserFavoriteDTO>> result = userFavoriteService.getUserFavorites(1, 10);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isNotNull();
            assertThat(result.getData().getItems()).hasSize(1);
            assertThat(result.getData().getItems().get(0).getArticle()).isNotNull();
            assertThat(result.getData().getItems().get(0).getArticle().getTitle()).isEqualTo("Test Article");
        }
    }

    // ==================== isArticleFavorited ====================

    @Test
    @DisplayName("检查是否收藏 - 缓存命中应直接返回")
    void isArticleFavorited_cacheHit_shouldReturnCachedValue() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(redisCacheUtils.getCache(anyString())).thenReturn(Boolean.TRUE);

            Result<Boolean> result = userFavoriteService.isArticleFavorited(1L);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isTrue();
            verify(userFavoriteMapper, never()).countByUserIdAndArticleId(anyLong(), anyLong());
        }
    }

    @Test
    @DisplayName("检查是否收藏 - 缓存命中false应直接返回")
    void isArticleFavorited_cacheHitFalse_shouldReturnFalseDirectly() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(redisCacheUtils.getCache(anyString())).thenReturn(Boolean.FALSE);

            Result<Boolean> result = userFavoriteService.isArticleFavorited(1L);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isFalse();
            verify(userFavoriteMapper, never()).countByUserIdAndArticleId(anyLong(), anyLong());
        }
    }

    @Test
    @DisplayName("检查是否收藏 - 未收藏应返回 false")
    void isArticleFavorited_notFavorited_shouldReturnFalse() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(redisCacheUtils.getCache(anyString())).thenReturn(null);
            when(userFavoriteMapper.countByUserIdAndArticleId(1L, 1L)).thenReturn(0);

            Result<Boolean> result = userFavoriteService.isArticleFavorited(1L);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isFalse();
            verify(redisCacheUtils).setCache(anyString(), eq(false), anyLong(), any());
        }
    }

    @Test
    @DisplayName("检查是否收藏 - 发生异常应返回错误")
    void isArticleFavorited_exception_shouldReturnError() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(redisCacheUtils.getCache(anyString())).thenThrow(new RuntimeException("redis error"));

            Result<Boolean> result = userFavoriteService.isArticleFavorited(1L);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("检查收藏状态失败");
        }
    }

    // ==================== getUserFavoriteCount ====================

    @Test
    @DisplayName("获取收藏数量 - 应返回统计值")
    void getUserFavoriteCount_shouldReturnCount() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(userFavoriteMapper.countByUserId(1L)).thenReturn(3);

            Result<Integer> result = userFavoriteService.getUserFavoriteCount();

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEqualTo(3);
        }
    }

    @Test
    @DisplayName("获取收藏数量 - 发生异常应返回错误")
    void getUserFavoriteCount_exception_shouldReturnError() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(userFavoriteMapper.countByUserId(anyLong())).thenThrow(new RuntimeException("db error"));

            Result<Integer> result = userFavoriteService.getUserFavoriteCount();

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("获取收藏数量失败");
        }
    }
}
