package com.blog.service.impl;

import com.blog.common.Result;
import com.blog.dto.CommentCreateDTO;
import com.blog.dto.CommentDTO;
import com.blog.entity.Article;
import com.blog.entity.Comment;
import com.blog.exception.BusinessException;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.CommentLikeMapper;
import com.blog.service.ArticleStatisticsService;
import com.blog.service.SensitiveWordService;
import com.blog.utils.AuthUtils;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
    private ArticleStatisticsService articleStatisticsService;

    @InjectMocks
    private CommentServiceImpl commentService;

    @Test
    @DisplayName("发表评论 - 文章不存在应返回错误")
    void createComment_articleNotFound_shouldReturnError() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(articleMapper.selectById(anyLong())).thenReturn(null);

            CommentCreateDTO dto = new CommentCreateDTO();
            dto.setArticleId(999L);
            dto.setContent("test");

            Result<Long> result = commentService.createComment(dto);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("文章不存在");
        }
    }

    @Test
    @DisplayName("发表评论 - 未发布文章应返回错误")
    void createComment_articleNotPublished_shouldReturnError() {
        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
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
    }

    @Test
    @DisplayName("检查敏感词 - 应返回检测结果")
    void checkSensitiveWords_shouldReturnResult() {
        when(sensitiveWordService.checkContent(anyString()))
                .thenReturn(Result.success(new com.blog.dto.SensitiveCheckResultDTO(true, null)));

        Result<Boolean> result = commentService.checkSensitiveWords("hello world");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isFalse();
    }

    @Test
    @DisplayName("获取文章评论数 - 应返回缓存或数据库值")
    void getArticleCommentCount_shouldReturnCount() {
        when(commentMapper.selectCommentsByArticleId(anyLong(), anyInt()))
                .thenReturn(Collections.emptyList());

        Result<Integer> result = commentService.getArticleCommentCount(1L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(0);
    }

    @Test
    @DisplayName("删除评论 - 非评论作者、文章作者或管理员应被拒绝")
    void deleteComment_unrelatedUser_shouldRejectBeforeMutation() {
        Comment comment = new Comment();
        comment.setId(10L);
        comment.setArticleId(20L);
        comment.setUserId(1L);
        Article article = new Article();
        article.setId(20L);
        article.setAuthorId(2L);
        when(redisDistributedLock.tryLockWithWatchdog(anyString(), anyLong(), any(), anyLong(), any()))
                .thenReturn("lock-token");
        when(commentMapper.selectById(10L)).thenReturn(comment);
        when(articleMapper.selectById(20L)).thenReturn(article);

        try (MockedStatic<AuthUtils> mocked = Mockito.mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(99L);
            mocked.when(AuthUtils::isAdmin).thenReturn(false);

            org.assertj.core.api.Assertions.assertThatThrownBy(() -> commentService.deleteComment(10L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("权限");
        }

        verify(commentMapper, never()).deleteById(anyLong());
        verify(commentLikeMapper, never()).deleteByCommentId(anyLong());
    }
}
