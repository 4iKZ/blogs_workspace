package com.blog.service.impl;

import com.blog.common.Result;
import com.blog.dto.CommentCreateDTO;
import com.blog.dto.CommentDTO;
import com.blog.entity.Article;
import com.blog.entity.Comment;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
}
