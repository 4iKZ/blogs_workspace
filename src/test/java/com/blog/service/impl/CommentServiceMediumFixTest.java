package com.blog.service.impl;

import com.blog.common.Result;
import com.blog.dto.CommentCreateDTO;
import com.blog.dto.CommentDTO;
import com.blog.entity.Article;
import com.blog.entity.Comment;
import com.blog.entity.Notification;
import com.blog.event.NotificationEvent;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CommentLikeMapper;
import com.blog.mapper.CommentMapper;
import com.blog.service.ArticleRankService;
import com.blog.service.ArticleStatisticsService;
import com.blog.service.SensitiveWordService;
import com.blog.utils.AuthUtils;
import com.blog.utils.CacheUtils;
import com.blog.utils.RedisCacheUtils;
import com.blog.utils.RedisDistributedLock;
import com.blog.utils.RedisUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Track A（#11/#12）回归测试。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommentServiceMediumFixTest {

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
    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CommentServiceImpl commentService;

    @BeforeEach
    void setUp() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 1L);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("#12 回复属于其他文章的评论应被拒绝")
    void createComment_replyToOtherArticleComment_shouldReject() {
        Article article = new Article();
        article.setStatus(2);
        article.setAuthorId(2L);
        when(articleMapper.selectById(anyLong())).thenReturn(article);
        when(sensitiveWordService.validateContent(anyString())).thenReturn(Result.success());

        Comment parent = new Comment();
        parent.setId(10L);
        parent.setParentId(0L);
        parent.setArticleId(2L); // 属于另一篇文章
        when(commentMapper.selectById(10L)).thenReturn(parent);

        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setArticleId(1L);
        dto.setUserId(1L);
        dto.setContent("reply");
        dto.setParentId(10L);
        dto.setReplyToCommentId(10L);

        Result<Long> result = commentService.createComment(dto);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("被回复的评论不属于该文章");
        verify(commentMapper, never()).insert(any(Comment.class));
    }

    private CommentDTO cachedDto(Integer status, Long ownerId) {
        CommentDTO dto = new CommentDTO();
        dto.setId(1L);
        dto.setContent("cached");
        dto.setStatus(status);
        dto.setUserId(ownerId);
        return dto;
    }

    @Test
    @DisplayName("#11 缓存命中未公开评论：匿名用户应被拒绝")
    void getCommentById_cacheHitNonPublic_anonymous_shouldReject() {
        when(redisCacheUtils.getCache(anyString())).thenReturn(cachedDto(0, 5L));
        RequestContextHolder.resetRequestAttributes(); // 匿名：无请求上下文

        Result<CommentDTO> result = commentService.getCommentById(1L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("评论不存在");
        verify(commentMapper, never()).selectById(anyLong());
    }

    @Test
    @DisplayName("#11 缓存命中未公开评论：非本人非管理员应被拒绝")
    void getCommentById_cacheHitNonPublic_otherUser_shouldReject() {
        when(redisCacheUtils.getCache(anyString())).thenReturn(cachedDto(0, 5L));

        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(99L);
            mocked.when(AuthUtils::isAdmin).thenReturn(false);

            Result<CommentDTO> result = commentService.getCommentById(1L);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("评论不存在");
        }
        verify(commentMapper, never()).selectById(anyLong());
    }

    @Test
    @DisplayName("#11 缓存命中未公开评论：本人可见")
    void getCommentById_cacheHitNonPublic_owner_shouldAllow() {
        // setUp 已设 userId=1L
        when(redisCacheUtils.getCache(anyString())).thenReturn(cachedDto(0, 1L));

        Result<CommentDTO> result = commentService.getCommentById(1L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getContent()).isEqualTo("cached");
    }

    @Test
    @DisplayName("#11 缓存命中未公开评论：管理员可见")
    void getCommentById_cacheHitNonPublic_admin_shouldAllow() {
        when(redisCacheUtils.getCache(anyString())).thenReturn(cachedDto(0, 5L));

        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(99L);
            mocked.when(AuthUtils::isAdmin).thenReturn(true);

            Result<CommentDTO> result = commentService.getCommentById(1L);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getContent()).isEqualTo("cached");
        }
    }

    // ==================== P1-2 楼中楼回复通知 ====================

    @Test
    @DisplayName("P1-2 三级回复通知应发给真实被回复者（replyToCommentId），而非根评论作者")
    void createComment_nestedReply_shouldNotifyReplyTargetRatherThanRootAuthor() {
        Article article = new Article();
        article.setId(1L);
        article.setStatus(2);
        article.setAuthorId(50L);
        when(articleMapper.selectById(anyLong())).thenReturn(article);
        when(sensitiveWordService.validateContent(anyString())).thenReturn(Result.success());

        // 根评论 A（作者 300）；评论 B 回复 A（作者 200）；当前用户 1 回复 B
        // createComment 会把 parentId 压平为根评论 A(id=10)，replyToCommentId 保留为 B(id=11)
        Comment commentB = new Comment();
        commentB.setId(11L);
        commentB.setParentId(10L);
        commentB.setArticleId(1L);
        commentB.setUserId(200L);
        when(commentMapper.selectById(11L)).thenReturn(commentB);
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
            dto.setContent("nested reply");
            dto.setParentId(10L); // 根评论 A
            dto.setReplyToCommentId(11L); // 真实回复目标 B

            Result<Long> result = commentService.createComment(dto);

            assertThat(result.isSuccess()).isTrue();
            ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
            verify(eventPublisher, atLeastOnce()).publishEvent(captor.capture());
            NotificationEvent replyEvent = captor.getAllValues().stream()
                    .filter(e -> Integer.valueOf(Notification.TYPE_COMMENT_REPLY).equals(e.getType()))
                    .findFirst().orElseThrow();
            // 接收者应为 B 的作者(200)，而不是根评论 A 的作者(300)
            assertThat(replyEvent.getUserId()).isEqualTo(200L);
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    // ==================== P1-3 被拒评论删除扣分 ====================

    @Test
    @DisplayName("P1-3 删除被拒评论(status=3)不应二次扣减热度分")
    void deleteComment_rejectedComment_shouldNotDoubleDecrementScore() {
        Article article = new Article();
        article.setAuthorId(1L);
        Comment root = new Comment();
        root.setId(1L);
        root.setUserId(5L);
        root.setArticleId(1L);
        root.setStatus(2); // 已通过审核，删除时应扣分
        when(commentMapper.selectById(1L)).thenReturn(root);
        when(articleMapper.selectById(1L)).thenReturn(article);
        when(redisDistributedLock.tryLockWithWatchdog(anyString(), anyLong(), any(), anyLong(), any()))
                .thenReturn("lock");

        Comment rejected = new Comment();
        rejected.setId(2L);
        rejected.setParentId(1L);
        rejected.setUserId(6L);
        rejected.setStatus(3); // 被拒：审核拒绝时已扣过分
        Comment approved = new Comment();
        approved.setId(3L);
        approved.setParentId(1L);
        approved.setUserId(7L);
        approved.setStatus(2); // 已通过，删除时应扣分
        when(commentMapper.selectChildCommentsByParentIds(List.of(1L), null))
                .thenReturn(List.of(rejected, approved));
        // BFS 第二层：2/3 均无子评论
        when(commentMapper.selectChildCommentsByParentIds(List.of(2L, 3L), null))
                .thenReturn(Collections.emptyList());

        TransactionSynchronizationManager.initSynchronization();
        try {
            Result<Void> result = commentService.deleteComment(1L);

            assertThat(result.isSuccess()).isTrue();
            // 热度分在事务提交后异步扣减，手动触发 afterCommit
            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCommit();
            }
            // 仅 root(status=2) + approved(status=2) 两条计入；rejected(status=3) 不重复扣分
            verify(articleRankService).decrementScore(1L, 20.0);
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }
}
