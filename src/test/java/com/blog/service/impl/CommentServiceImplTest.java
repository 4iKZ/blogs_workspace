package com.blog.service.impl;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.common.ResultCode;
import com.blog.dto.CommentCreateDTO;
import com.blog.dto.CommentDTO;
import com.blog.dto.SensitiveCheckResultDTO;
import com.blog.entity.Article;
import com.blog.entity.Comment;
import com.blog.entity.CommentLike;
import com.blog.exception.BusinessException;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CommentLikeMapper;
import com.blog.mapper.CommentMapper;
import com.blog.service.ArticleStatisticsService;
import com.blog.service.ArticleRankService;
import com.blog.service.SensitiveWordService;
import com.blog.utils.AuthUtils;
import com.blog.utils.BusinessUtils;
import com.blog.utils.CacheUtils;
import com.blog.utils.DTOConverter;
import com.blog.utils.PageUtils;
import com.blog.utils.RedisCacheUtils;
import com.blog.utils.RedisDistributedLock;
import com.blog.utils.RedisUtils;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommentServiceImplTest {

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private CommentLikeMapper commentLikeMapper;

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private SensitiveWordService sensitiveWordService;

    @Mock
    private RedisCacheUtils redisCacheUtils;

    @Mock
    private RedisDistributedLock redisDistributedLock;

    @Mock
    private RedisUtils redisUtils;

    @Mock
    private ArticleStatisticsService articleStatisticsService;

    @Mock
    private ArticleRankService articleRankService;

    @Mock
    private CacheUtils cacheUtils;

    @InjectMocks
    private CommentServiceImpl commentService;

    @BeforeEach
    void setUp() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
        resetTransactionSynchronization();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 1L);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
        resetTransactionSynchronization();
    }

    private void resetTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clear();
        }
    }

    // ==================== createComment ====================

    @Test
    @DisplayName("发表评论 - 文章不存在应返回错误")
    void createComment_articleNotFound_shouldReturnError() {
        when(articleMapper.selectById(anyLong())).thenReturn(null);

        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setArticleId(999L);
        dto.setContent("test");

        Result<Long> result = commentService.createComment(dto);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("关联的文章不存在");
    }

    @Test
    @DisplayName("发表评论 - 未发布文章应返回错误")
    void createComment_articleNotPublished_shouldReturnError() {
        Article article = new Article();
        article.setStatus(0);
        when(articleMapper.selectById(anyLong())).thenReturn(article);

        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setArticleId(1L);
        dto.setContent("test");

        Result<Long> result = commentService.createComment(dto);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("未发布");
    }

    @Test
    @DisplayName("发表评论 - 敏感词检测失败应返回错误")
    void createComment_sensitiveWordCheckFailed_shouldReturnError() {
        Article article = new Article();
        article.setStatus(2);
        article.setAuthorId(2L);
        when(articleMapper.selectById(anyLong())).thenReturn(article);
        SensitiveCheckResultDTO sensitiveResult = new SensitiveCheckResultDTO();
        sensitiveResult.setPassed(false);
        when(sensitiveWordService.validateContent(anyString()))
                .thenReturn(Result.error("包含敏感词"));

        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setArticleId(1L);
        dto.setUserId(1L);
        dto.setContent("敏感内容");

        Result<Long> result = commentService.createComment(dto);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("评论");
    }

    @Test
    @DisplayName("发表评论 - 用户未登录应返回错误")
    void createComment_userNotLogin_shouldReturnError() {
        Article article = new Article();
        article.setStatus(2);
        article.setAuthorId(2L);
        when(articleMapper.selectById(anyLong())).thenReturn(article);
        when(sensitiveWordService.validateContent(anyString())).thenReturn(Result.success());

        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setArticleId(1L);
        dto.setUserId(null);
        dto.setContent("test");

        Result<Long> result = commentService.createComment(dto);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("用户未登录");
    }

    @Test
    @DisplayName("发表评论 - 成功发表应返回ID并发送通知")
    void createComment_success_shouldReturnIdAndNotify() {
        Article article = new Article();
        article.setStatus(2);
        article.setAuthorId(2L);
        when(articleMapper.selectById(anyLong())).thenReturn(article);
        when(sensitiveWordService.validateContent(anyString())).thenReturn(Result.success());
        when(commentMapper.insert(any(Comment.class))).thenAnswer(invocation -> {
            Comment c = invocation.getArgument(0);
            c.setId(100L);
            return 1;
        });

        TransactionSynchronizationManager.initSynchronization();
        try {
            CommentCreateDTO dto = new CommentCreateDTO();
            dto.setArticleId(1L);
            dto.setUserId(1L);
            dto.setContent("good");
            dto.setParentId(0L);

            Result<Long> result = commentService.createComment(dto);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEqualTo(100L);
            verify(articleStatisticsService).incrementCommentCount(1L);
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    @DisplayName("发表评论 - 插入失败应返回错误")
    void createComment_insertFailed_shouldReturnError() {
        Article article = new Article();
        article.setStatus(2);
        article.setAuthorId(2L);
        when(articleMapper.selectById(anyLong())).thenReturn(article);
        when(sensitiveWordService.validateContent(anyString())).thenReturn(Result.success());
        when(commentMapper.insert(any(Comment.class))).thenReturn(0);

        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setArticleId(1L);
        dto.setUserId(1L);
        dto.setContent("good");

        Result<Long> result = commentService.createComment(dto);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("发表评论失败");
    }

    // ==================== getCommentList ====================

    @Test
    @DisplayName("获取评论列表 - 缓存命中应直接返回")
    void getCommentList_cacheHit_shouldReturnCachedList() {
        when(redisCacheUtils.getCache(anyString())).thenReturn(Collections.emptyList());

        Result<List<CommentDTO>> result = commentService.getCommentList(1L, 1, 10, 2, "time", null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEmpty();
        verify(commentMapper, never()).selectTopLevelCommentsWithPagination(anyLong(), anyInt(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("获取评论列表 - 缓存类型异常应降级查询数据库")
    void getCommentList_cacheClassCast_shouldFallback() {
        when(redisCacheUtils.getCache(anyString())).thenReturn("bad-type");

        Result<List<CommentDTO>> result = commentService.getCommentList(1L, 1, 10, 2, "time", null);

        assertThat(result.isSuccess()).isTrue();
        verify(commentMapper).selectTopLevelCommentsWithPagination(anyLong(), anyInt(), anyInt(), anyInt());
    }

    // ==================== deleteComment ====================

    @Test
    @DisplayName("删除评论 - 无权限应抛出异常")
    void deleteComment_noPermission_shouldThrow() {
        Article article = new Article();
        article.setAuthorId(2L);
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setUserId(2L);
        comment.setArticleId(1L);
        when(commentMapper.selectById(1L)).thenReturn(comment);
        when(articleMapper.selectById(1L)).thenReturn(article);
        when(redisDistributedLock.tryLockWithWatchdog(anyString(), anyLong(), any(), anyLong(), any()))
                .thenReturn("lock");
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(99L);
            mocked.when(AuthUtils::isAdmin).thenReturn(false);
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setAttribute("userId", 99L);
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            assertThatThrownBy(() -> commentService.deleteComment(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("无权限");
        }
    }

    @Test
    @DisplayName("删除评论 - 成功删除应清除缓存并扣减统计")
    void deleteComment_success_shouldClearCacheAndDecrement() {
        Article article = new Article();
        article.setAuthorId(1L);
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setUserId(1L);
        comment.setArticleId(1L);
        when(commentMapper.selectById(1L)).thenReturn(comment);
        when(articleMapper.selectById(1L)).thenReturn(article);
        when(redisDistributedLock.tryLockWithWatchdog(anyString(), anyLong(), any(), anyLong(), any()))
                .thenReturn("lock");
        when(commentMapper.selectDirectChildComments(1L)).thenReturn(Collections.emptyList());

        TransactionSynchronizationManager.initSynchronization();
        try {
            Result<Void> result = commentService.deleteComment(1L);

            assertThat(result.isSuccess()).isTrue();
            verify(articleStatisticsService).decrementCommentCount(1L, 1);
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    // ==================== likeComment / unlikeComment ====================

    @Test
    @DisplayName("点赞评论 - 用户未登录应返回错误")
    void likeComment_userNotLogin_shouldReturnError() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(null);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any(), anyLong(), any())).thenReturn("lock");

            Result<Void> result = commentService.likeComment(1L);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("用户未登录");
        }
    }

    @Test
    @DisplayName("取消点赞评论 - 缓存未命中且数据库不存在应返回成功")
    void unlikeComment_cacheMissAndNotExist_shouldReturnSuccess() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any(), anyLong(), any())).thenReturn("lock");
            when(redisCacheUtils.getCache(anyString())).thenReturn(null);
            when(commentLikeMapper.checkUserLikedComment(anyLong(), anyLong())).thenReturn(false);
            when(commentLikeMapper.deleteByCommentIdAndUserId(anyLong(), anyLong())).thenReturn(0);

            Result<Void> result = commentService.unlikeComment(1L);

            assertThat(result.isSuccess()).isTrue();
        }
    }

    @Test
    @DisplayName("检查评论点赞状态 - 缓存命中应直接返回")
    void checkCommentLikeStatus_cacheHit_shouldReturnCachedValue() {
        when(redisCacheUtils.getCache(anyString())).thenReturn(Boolean.TRUE);

        Result<Boolean> result = commentService.checkCommentLikeStatus(1L, 1L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isTrue();
        verify(commentLikeMapper, never()).checkUserLikedComment(anyLong(), anyLong());
    }

    @Test
    @DisplayName("批量检查点赞状态 - 空参数应返回空Map")
    void batchCheckCommentLikeStatus_nullInput_shouldReturnEmptyMap() {
        Result<Map<Long, Boolean>> result = commentService.batchCheckCommentLikeStatus(null, 1L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEmpty();
    }

    // ==================== getArticleCommentCount / getUserComments /
    // getHotComments / getChildComments ====================

    @Test
    @DisplayName("获取文章评论数量 - 缓存命中应直接返回")
    void getArticleCommentCount_cacheHit_shouldReturnCachedCount() {
        when(redisCacheUtils.getCache(anyString())).thenReturn(5);

        Result<Integer> result = commentService.getArticleCommentCount(1L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(5);
        verify(commentMapper, never()).selectCommentsByArticleId(anyLong(), anyInt());
    }

    @Test
    @DisplayName("获取文章评论数量 - 发生异常应返回错误")
    void getArticleCommentCount_exception_shouldReturnError() {
        when(redisCacheUtils.getCache(anyString())).thenThrow(new RuntimeException("redis error"));

        Result<Integer> result = commentService.getArticleCommentCount(1L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("获取文章评论数量失败");
    }

    @Test
    @DisplayName("获取用户评论列表 - 应返回分页结果")
    void getUserComments_shouldReturnPagedList() {
        when(commentMapper.selectCommentsByUserIdWithPagination(anyLong(), any(), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());

        Result<List<CommentDTO>> result = commentService.getUserComments(1L, 1, 10);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEmpty();
    }

    @Test
    @DisplayName("获取热门评论 - 缓存命中应直接返回")
    void getHotComments_cacheHit_shouldReturnCachedList() {
        when(redisCacheUtils.getCache(anyString())).thenReturn(Collections.emptyList());

        Result<List<CommentDTO>> result = commentService.getHotComments(1L, 5);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEmpty();
        verify(commentMapper, never()).selectCommentsByArticleId(anyLong(), anyInt());
    }

    @Test
    @DisplayName("获取子评论 - 应返回分页结果")
    void getChildComments_shouldReturnPagedResult() {
        when(commentMapper.selectChildCommentsByParentIds(anyList(), anyInt())).thenReturn(Collections.emptyList());

        Result<List<CommentDTO>> result = commentService.getChildComments(1L, 1, 10);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEmpty();
    }

    @Test
    @DisplayName("获取子评论 - 发生异常应返回错误")
    void getChildComments_exception_shouldReturnError() {
        when(commentMapper.selectChildCommentsByParentIds(anyList(), anyInt()))
                .thenThrow(new RuntimeException("db error"));

        Result<List<CommentDTO>> result = commentService.getChildComments(1L, 1, 10);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("获取子评论失败");
    }

    // ==================== createComment 补充场景 ====================

    @Test
    @DisplayName("发表评论 - 回复不存在的评论应返回错误")
    void createComment_replyToCommentNotFound_shouldReturnError() {
        Article article = new Article();
        article.setStatus(2);
        article.setAuthorId(2L);
        when(articleMapper.selectById(anyLong())).thenReturn(article);
        when(sensitiveWordService.validateContent(anyString())).thenReturn(Result.success());

        Comment parent = new Comment();
        parent.setId(10L);
        parent.setParentId(0L);
        when(commentMapper.selectById(10L)).thenReturn(parent);
        when(commentMapper.selectById(999L)).thenReturn(null);

        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setArticleId(1L);
        dto.setUserId(1L);
        dto.setContent("reply");
        dto.setParentId(10L);
        dto.setReplyToCommentId(999L);

        Result<Long> result = commentService.createComment(dto);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("被回复的评论不存在");
    }

    @Test
    @DisplayName("发表评论 - 回复已有父评论应成功")
    void createComment_replySuccess_shouldReturnId() {
        Article article = new Article();
        article.setStatus(2);
        article.setAuthorId(2L);
        when(articleMapper.selectById(anyLong())).thenReturn(article);
        when(sensitiveWordService.validateContent(anyString())).thenReturn(Result.success());

        Comment parent = new Comment();
        parent.setId(10L);
        parent.setParentId(0L);
        when(commentMapper.selectById(10L)).thenReturn(parent);

        when(commentMapper.insert(any(Comment.class))).thenAnswer(invocation -> {
            Comment c = invocation.getArgument(0);
            c.setId(100L);
            return 1;
        });

        TransactionSynchronizationManager.initSynchronization();
        try {
            CommentCreateDTO dto = new CommentCreateDTO();
            dto.setArticleId(1L);
            dto.setUserId(1L);
            dto.setContent("reply");
            dto.setParentId(10L);
            dto.setReplyToCommentId(10L);

            Result<Long> result = commentService.createComment(dto);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEqualTo(100L);
            verify(articleStatisticsService).incrementCommentCount(1L);
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    @DisplayName("发表评论 - 通知异常不应影响主流程")
    void createComment_notificationThrows_shouldStillSucceed() {
        Article article = new Article();
        article.setStatus(2);
        article.setAuthorId(2L);
        when(articleMapper.selectById(1L)).thenReturn(article);
        when(sensitiveWordService.validateContent(anyString())).thenReturn(Result.success());

        when(commentMapper.insert(any(Comment.class))).thenAnswer(invocation -> {
            Comment c = invocation.getArgument(0);
            c.setId(100L);
            return 1;
        });

        when(articleMapper.selectById(anyLong())).thenReturn(article).thenThrow(new RuntimeException("event error"));

        TransactionSynchronizationManager.initSynchronization();
        try {
            CommentCreateDTO dto = new CommentCreateDTO();
            dto.setArticleId(1L);
            dto.setUserId(1L);
            dto.setContent("good");
            dto.setParentId(0L);

            Result<Long> result = commentService.createComment(dto);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEqualTo(100L);
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    // ==================== getCommentList 补充场景 ====================

    @Test
    @DisplayName("获取评论列表 - 有用户ID时应跳过缓存并查询数据库")
    void getCommentList_userIdNotNull_shouldQueryDb() {
        when(redisCacheUtils.getCache(anyString())).thenReturn(null);
        when(commentMapper.selectTopLevelCommentsWithPagination(anyLong(), anyInt(), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());

        Result<List<CommentDTO>> result = commentService.getCommentList(1L, 1, 10, 2, "time", 1L);

        assertThat(result.isSuccess()).isTrue();
        verify(commentMapper).selectTopLevelCommentsWithPagination(anyLong(), anyInt(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("获取评论列表 - 不同sortBy应走数据库查询")
    void getCommentList_differentSortBy_shouldQueryDb() {
        when(redisCacheUtils.getCache(anyString())).thenReturn(null);
        when(commentMapper.selectTopLevelCommentsWithPagination(anyLong(), anyInt(), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());

        Result<List<CommentDTO>> result = commentService.getCommentList(1L, 1, 10, 2, "hot", null);

        assertThat(result.isSuccess()).isTrue();
        verify(commentMapper).selectTopLevelCommentsWithPagination(anyLong(), anyInt(), anyInt(), anyInt());
    }

    // ==================== deleteComment 补充场景 ====================

    @Test
    @DisplayName("删除评论 - 评论不存在应抛出异常")
    void deleteComment_commentNotFound_shouldThrow() {
        when(commentMapper.selectById(anyLong())).thenReturn(null);
        when(redisDistributedLock.tryLockWithWatchdog(anyString(), anyLong(), any(), anyLong(), any()))
                .thenReturn("lock");

        assertThatThrownBy(() -> commentService.deleteComment(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("评论不存在");
    }

    @Test
    @DisplayName("删除评论 - 文章不存在且无权限应抛出异常")
    void deleteComment_articleNotFound_shouldThrowPermissionDenied() {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setUserId(88L);
        comment.setArticleId(1L);
        when(commentMapper.selectById(1L)).thenReturn(comment);
        when(articleMapper.selectById(1L)).thenReturn(null);
        when(redisDistributedLock.tryLockWithWatchdog(anyString(), anyLong(), any(), anyLong(), any()))
                .thenReturn("lock");

        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(99L);
            mocked.when(AuthUtils::isAdmin).thenReturn(false);
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setAttribute("userId", 99L);
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            assertThatThrownBy(() -> commentService.deleteComment(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("无权限");
        }
    }

    @Test
    @DisplayName("删除评论 - 存在子评论应递归删除")
    void deleteComment_withChildComments_shouldDeleteAll() {
        Article article = new Article();
        article.setAuthorId(1L);
        Comment parent = new Comment();
        parent.setId(1L);
        parent.setUserId(1L);
        parent.setArticleId(1L);
        when(commentMapper.selectById(1L)).thenReturn(parent);
        when(articleMapper.selectById(1L)).thenReturn(article);
        when(redisDistributedLock.tryLockWithWatchdog(anyString(), anyLong(), any(), anyLong(), any()))
                .thenReturn("lock");

        Comment child = new Comment();
        child.setId(2L);
        child.setParentId(1L);
        when(commentMapper.selectDirectChildComments(1L)).thenReturn(List.of(child));
        when(commentMapper.selectDirectChildComments(2L)).thenReturn(Collections.emptyList());

        TransactionSynchronizationManager.initSynchronization();
        try {
            Result<Void> result = commentService.deleteComment(1L);

            assertThat(result.isSuccess()).isTrue();
            verify(commentMapper).deleteById(1L);
            verify(commentMapper).deleteById(2L);
            verify(commentLikeMapper).deleteByCommentId(1L);
            verify(commentLikeMapper).deleteByCommentId(2L);
            verify(articleStatisticsService).decrementCommentCount(1L, 2);
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    // ==================== likeComment 补充场景 ====================

    @Test
    @DisplayName("点赞评论 - 评论不存在应返回错误")
    void likeComment_commentNotFound_shouldReturnError() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any(), anyLong(), any())).thenReturn("lock");
            when(commentMapper.selectById(anyLong())).thenReturn(null);

            Result<Void> result = commentService.likeComment(999L);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("评论不存在");
        }
    }

    @Test
    @DisplayName("点赞评论 - 缓存已点赞应直接返回错误")
    void likeComment_alreadyLiked_cacheHit_shouldReturnError() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any(), anyLong(), any())).thenReturn("lock");

            String likeCacheKey = RedisCacheUtils.generateCommentLikeKey(1L, 1L);
            when(redisCacheUtils.getCache(likeCacheKey)).thenReturn(Boolean.TRUE);

            Comment comment = new Comment();
            comment.setId(1L);
            when(commentMapper.selectById(1L)).thenReturn(comment);

            Result<Void> result = commentService.likeComment(1L);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("已点赞");
            verify(commentLikeMapper, never()).checkUserLikedComment(anyLong(), anyLong());
        }
    }

    @Test
    @DisplayName("点赞评论 - 数据库已点赞应返回错误")
    void likeComment_alreadyLiked_databaseHit_shouldReturnError() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any(), anyLong(), any())).thenReturn("lock");

            String likeCacheKey = RedisCacheUtils.generateCommentLikeKey(1L, 1L);
            when(redisCacheUtils.getCache(likeCacheKey)).thenReturn(null);

            Comment comment = new Comment();
            comment.setId(1L);
            when(commentMapper.selectById(1L)).thenReturn(comment);
            when(commentLikeMapper.checkUserLikedComment(1L, 1L)).thenReturn(true);

            Result<Void> result = commentService.likeComment(1L);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("已点赞");
            verify(redisCacheUtils).setCache(eq(likeCacheKey), eq(true), anyLong(), any());
        }
    }

    @Test
    @DisplayName("点赞评论 - 成功点赞应更新数据库和缓存")
    void likeComment_success_withTransaction_shouldReturnSuccess() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
                mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
                when(redisDistributedLock.tryLock(anyString(), anyLong(), any(), anyLong(), any())).thenReturn("lock");

                String likeCacheKey = RedisCacheUtils.generateCommentLikeKey(1L, 1L);
                String commentDetailKey = RedisCacheUtils.generateCommentDetailKey(1L);
                when(redisCacheUtils.getCache(likeCacheKey)).thenReturn(null);

                Comment comment = new Comment();
                comment.setId(1L);
                when(commentMapper.selectById(1L)).thenReturn(comment);
                when(commentLikeMapper.checkUserLikedComment(1L, 1L)).thenReturn(false);
                when(commentLikeMapper.insert(any(CommentLike.class))).thenReturn(1);
                when(commentMapper.incrementLikeCount(1L)).thenReturn(1);

                Result<Void> result = commentService.likeComment(1L);

                assertThat(result.isSuccess()).isTrue();
                verify(commentLikeMapper).insert(any(CommentLike.class));
                verify(commentMapper).incrementLikeCount(1L);
            }
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    // ==================== unlikeComment 补充场景 ====================

    @Test
    @DisplayName("取消点赞评论 - 获取锁失败应返回错误")
    void unlikeComment_lockAcquisitionFailed_shouldReturnError() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any(), anyLong(), any())).thenReturn(null);

            Result<Void> result = commentService.unlikeComment(1L);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("操作过于频繁");
        }
    }

    @Test
    @DisplayName("取消点赞评论 - 缓存显示已点赞应成功取消")
    void unlikeComment_cacheShowsLiked_shouldReturnSuccess() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any(), anyLong(), any())).thenReturn("lock");

            String likeCacheKey = RedisCacheUtils.generateCommentLikeKey(1L, 1L);
            when(redisCacheUtils.getCache(likeCacheKey)).thenReturn(Boolean.TRUE);
            when(commentLikeMapper.deleteByCommentIdAndUserId(1L, 1L)).thenReturn(1);
            when(commentMapper.decrementLikeCount(1L)).thenReturn(1);

            TransactionSynchronizationManager.initSynchronization();
            try {
                Result<Void> result = commentService.unlikeComment(1L);

                assertThat(result.isSuccess()).isTrue();
                verify(commentLikeMapper).deleteByCommentIdAndUserId(1L, 1L);
                verify(commentMapper).decrementLikeCount(1L);
            } finally {
                TransactionSynchronizationManager.clear();
            }
        }
    }

    @Test
    @DisplayName("取消点赞评论 - 数据库删除失败应抛出异常")
    void unlikeComment_deleteFails_shouldThrowException() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(redisDistributedLock.tryLock(anyString(), anyLong(), any(), anyLong(), any())).thenReturn("lock");

            String likeCacheKey = RedisCacheUtils.generateCommentLikeKey(1L, 1L);
            when(redisCacheUtils.getCache(likeCacheKey)).thenReturn(Boolean.TRUE);
            when(commentLikeMapper.deleteByCommentIdAndUserId(1L, 1L)).thenReturn(0);

            assertThatThrownBy(() -> commentService.unlikeComment(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("取消评论点赞失败");
        }
    }

    // ==================== checkCommentLikeStatus / batchCheckCommentLikeStatus
    // 补充场景 ====================

    @Test
    @DisplayName("检查评论点赞状态 - 缓存未命中应查询数据库")
    void checkCommentLikeStatus_cacheMiss_shouldQueryDb() {
        String likeCacheKey = RedisCacheUtils.generateCommentLikeKey(1L, 1L);
        when(redisCacheUtils.getCache(likeCacheKey)).thenReturn(null);
        when(commentLikeMapper.checkUserLikedComment(1L, 1L)).thenReturn(true);

        Result<Boolean> result = commentService.checkCommentLikeStatus(1L, 1L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isTrue();
        verify(commentLikeMapper).checkUserLikedComment(1L, 1L);
        verify(redisCacheUtils).setCache(eq(likeCacheKey), eq(true), anyLong(), any());
    }

    @Test
    @DisplayName("批量检查点赞状态 - 正常场景应返回Map")
    void batchCheckCommentLikeStatus_normalCase_shouldReturnMap() {
        when(commentLikeMapper.batchCheckUserLikedComments(anyList(), anyLong())).thenReturn(List.of(1L, 3L));

        Result<Map<Long, Boolean>> result = commentService.batchCheckCommentLikeStatus(List.of(1L, 2L, 3L), 1L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(3);
        assertThat(result.getData().get(1L)).isTrue();
        assertThat(result.getData().get(2L)).isFalse();
        assertThat(result.getData().get(3L)).isTrue();
        verify(redisCacheUtils, times(3)).setCache(anyString(), anyBoolean(), anyLong(), any());
    }

    // ==================== getArticleCommentCount / getUserComments /
    // getHotComments 补充场景 ====================

    @Test
    @DisplayName("获取文章评论数量 - 缓存未命中应查询数据库")
    void getArticleCommentCount_cacheMiss_shouldQueryDb() {
        when(redisCacheUtils.getCache(anyString())).thenReturn(null);
        when(commentMapper.selectCommentsByArticleId(anyLong(), anyInt()))
                .thenReturn(List.of(new Comment(), new Comment()));

        Result<Integer> result = commentService.getArticleCommentCount(1L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(2);
        verify(commentMapper).selectCommentsByArticleId(1L, 2);
        verify(redisCacheUtils).setCache(anyString(), eq(2), anyLong(), any());
    }

    @Test
    @DisplayName("获取用户评论列表 - 异常应返回错误")
    void getUserComments_exception_shouldReturnError() {
        when(commentMapper.selectCommentsByUserIdWithPagination(anyLong(), any(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("db error"));

        Result<List<CommentDTO>> result = commentService.getUserComments(1L, 1, 10);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("获取用户评论列表失败");
    }

    @Test
    @DisplayName("获取热门评论 - 数据库查询应按点赞数排序")
    void getHotComments_databaseQuery_shouldSortByLikeCount() {
        when(redisCacheUtils.getCache(anyString())).thenReturn(null);

        Comment c1 = new Comment();
        c1.setId(1L);
        c1.setLikeCount(5);
        Comment c2 = new Comment();
        c2.setId(2L);
        c2.setLikeCount(10);
        when(commentMapper.selectCommentsByArticleId(anyLong(), anyInt())).thenReturn(List.of(c1, c2));

        Result<List<CommentDTO>> result = commentService.getHotComments(1L, 2);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(2);
        assertThat(result.getData().get(0).getId()).isEqualTo(2L);
        assertThat(result.getData().get(1).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("获取热门评论 - 数据库异常应返回错误")
    void getHotComments_exception_shouldReturnError() {
        when(redisCacheUtils.getCache(anyString())).thenReturn(null);
        when(commentMapper.selectCommentsByArticleId(anyLong(), anyInt())).thenThrow(new RuntimeException("db error"));

        Result<List<CommentDTO>> result = commentService.getHotComments(1L, 5);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("获取热门评论失败");
    }
}
