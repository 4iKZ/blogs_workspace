package com.blog.event;

import com.blog.common.Result;
import com.blog.dto.ModerationResult;
import com.blog.entity.Article;
import com.blog.entity.Notification;
import com.blog.mapper.ArticleMapper;
import com.blog.service.ArticleModerationLogService;
import com.blog.service.ArticleRankService;
import com.blog.service.ContentModerationService;
import com.blog.service.NotificationService;
import com.blog.utils.RedisDistributedLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ModerationEventListener 单元测试")
class ModerationEventListenerTest {

    @Mock
    private ContentModerationService contentModerationService;

    @Mock
    private ArticleModerationLogService articleModerationLogService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private ArticleRankService articleRankService;

    @Mock
    private RedisDistributedLock redisDistributedLock;

    @InjectMocks
    private ModerationEventListener listener;

    private ModerationEvent passedEvent;
    private ModerationEvent failedEvent;

    @BeforeEach
    void setUp() {
        passedEvent = new ModerationEvent(
                this, 1L, "测试文章", "文章内容", 100L, ModerationEvent.ModerationType.NEW_PUBLISH);
        failedEvent = new ModerationEvent(
                this, 2L, "违规文章", "违规内容", 200L, ModerationEvent.ModerationType.NEW_PUBLISH);
    }

    @Test
    @DisplayName("审核通过时更新文章状态并发送通知")
    void testHandleModerationEvent_Passed() {
        // Arrange
        Article article = new Article();
        article.setId(1L);
        article.setStatus(1);

        when(redisDistributedLock.tryLock(anyString(), anyLong(), any(TimeUnit.class), anyLong(), any(TimeUnit.class)))
                .thenReturn("lock-value");
        when(contentModerationService.moderateArticle(anyString(), anyString()))
                .thenReturn(Result.success(ModerationResult.pass()));
        when(articleMapper.selectById(1L)).thenReturn(article);
        when(articleModerationLogService.saveModerationLog(anyLong(), anyString(), anyString(), any()))
                .thenReturn(Result.success(1L));

        // Act
        listener.handleModerationEvent(passedEvent);

        // Assert
        verify(articleMapper).updateById(article);
        assertEquals(2, article.getStatus()); // 已发布状态
        assertNotNull(article.getPublishTime());

        verify(notificationService).createNotification(
                eq(100L), eq(0L), eq(Notification.TYPE_ARTICLE_MODERATION_PASSED),
                eq(1L), eq(Notification.TARGET_TYPE_ARTICLE), anyString());

        verify(articleRankService).initializeArticle(1L);
        verify(redisDistributedLock).unlock(anyString(), eq("lock-value"));
    }

    @Test
    @DisplayName("审核未通过时保持草稿状态并发送通知")
    void testHandleModerationEvent_NotPassed() {
        // Arrange
        Article article = new Article();
        article.setId(2L);
        article.setStatus(1);

        ModerationResult failResult = ModerationResult.fail("violence", List.of("包含暴力内容"), 0.9, "请修改");

        when(redisDistributedLock.tryLock(anyString(), anyLong(), any(TimeUnit.class), anyLong(), any(TimeUnit.class)))
                .thenReturn("lock-value");
        when(contentModerationService.moderateArticle(anyString(), anyString()))
                .thenReturn(Result.success(failResult));

        // Act
        listener.handleModerationEvent(failedEvent);

        // Assert
        // 注意：审核未通过时只发通知，不更新文章状态
        verify(notificationService).createNotification(
                eq(200L), eq(0L), eq(Notification.TYPE_ARTICLE_MODERATION_FAILED),
                eq(2L), eq(Notification.TARGET_TYPE_ARTICLE), anyString());

        // 确认通知内容包含违规原因
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createNotification(
                eq(200L), eq(0L), eq(Notification.TYPE_ARTICLE_MODERATION_FAILED),
                eq(2L), eq(Notification.TARGET_TYPE_ARTICLE), contentCaptor.capture());
        assertTrue(contentCaptor.getValue().contains("暴力内容"));
    }

    @Test
    @DisplayName("AI服务异常时降级处理 - 自动发布文章")
    void testHandleModerationEvent_Exception() {
        // Arrange
        Article article = new Article();
        article.setId(1L);
        article.setStatus(1);

        when(redisDistributedLock.tryLock(anyString(), anyLong(), any(TimeUnit.class), anyLong(), any(TimeUnit.class)))
                .thenReturn("lock-value");
        when(contentModerationService.moderateArticle(anyString(), anyString()))
                .thenThrow(new RuntimeException("AI服务不可用"));
        when(articleMapper.selectById(1L)).thenReturn(article);

        // Act
        listener.handleModerationEvent(passedEvent);

        // Assert
        verify(articleMapper).updateById(article);
        assertEquals(2, article.getStatus()); // 降级为已发布

        // 发送通知告知用户
        verify(notificationService).createNotification(
                eq(100L), eq(0L), eq(Notification.TYPE_ARTICLE_MODERATION_PASSED),
                eq(1L), eq(Notification.TARGET_TYPE_ARTICLE), anyString());
    }

    @Test
    @DisplayName("获取锁失败时跳过重复审核")
    void testHandleModerationEvent_LockFailed() {
        // Arrange
        when(redisDistributedLock.tryLock(anyString(), anyLong(), any(TimeUnit.class), anyLong(), any(TimeUnit.class)))
                .thenReturn(null); // 获取锁失败

        // Act
        listener.handleModerationEvent(passedEvent);

        // Assert
        verify(contentModerationService, never()).moderateArticle(anyString(), anyString());
        verify(articleMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("审核返回异常结果时不处理")
    void testHandleModerationEvent_ModerationResultNull() {
        // Arrange
        when(redisDistributedLock.tryLock(anyString(), anyLong(), any(TimeUnit.class), anyLong(), any(TimeUnit.class)))
                .thenReturn("lock-value");
        when(contentModerationService.moderateArticle(anyString(), anyString()))
                .thenReturn(Result.success(null));

        // Act
        listener.handleModerationEvent(passedEvent);

        // Assert
        verify(articleMapper, never()).updateById(any());
        verify(notificationService, never()).createNotification(anyLong(), anyLong(), anyInt(), anyLong(), anyInt(), anyString());
    }

    @Test
    @DisplayName("文章不存在时不处理")
    void testHandleModerationEvent_ArticleNotFound() {
        // Arrange
        when(redisDistributedLock.tryLock(anyString(), anyLong(), any(TimeUnit.class), anyLong(), any(TimeUnit.class)))
                .thenReturn("lock-value");
        when(contentModerationService.moderateArticle(anyString(), anyString()))
                .thenReturn(Result.success(ModerationResult.pass()));
        when(articleMapper.selectById(1L)).thenReturn(null);

        // Act
        listener.handleModerationEvent(passedEvent);

        // Assert
        verify(articleMapper, never()).updateById(any());
        verify(notificationService, never()).createNotification(anyLong(), anyLong(), anyInt(), anyLong(), anyInt(), anyString());
    }

    @Test
    @DisplayName("审核通过时保存审核记录")
    void testHandleModerationEvent_SavesModerationLog() {
        // Arrange
        Article article = new Article();
        article.setId(1L);
        article.setStatus(1);

        when(redisDistributedLock.tryLock(anyString(), anyLong(), any(TimeUnit.class), anyLong(), any(TimeUnit.class)))
                .thenReturn("lock-value");
        when(contentModerationService.moderateArticle(eq("测试文章"), eq("文章内容")))
                .thenReturn(Result.success(ModerationResult.pass()));
        when(articleMapper.selectById(1L)).thenReturn(article);

        // Act
        listener.handleModerationEvent(passedEvent);

        // Assert
        verify(articleModerationLogService).saveModerationLog(
                eq(1L), eq("测试文章"), eq("文章内容"), any(ModerationResult.class));
    }
}