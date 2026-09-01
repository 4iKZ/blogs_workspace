package com.blog.event;

import com.blog.common.Result;
import com.blog.dto.ModerationResult;
import com.blog.entity.Notification;
import com.blog.service.CommentService;
import com.blog.service.ContentModerationService;
import com.blog.service.NotificationService;
import com.blog.utils.RedisDistributedLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CommentModerationEventListener 单元测试")
class CommentModerationEventListenerTest {

    @Mock
    private ContentModerationService contentModerationService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private RedisDistributedLock redisDistributedLock;

    @Mock
    private CommentService commentService;

    @InjectMocks
    private CommentModerationEventListener listener;

    private CommentModerationEvent passedEvent;
    private CommentModerationEvent failedEvent;

    @BeforeEach
    void setUp() {
        passedEvent = new CommentModerationEvent(
                this, 1L, 100L, "这是一个正常评论", "测试文章");
        failedEvent = new CommentModerationEvent(
                this, 2L, 200L, "这是一个违规评论", "测试文章");

        when(redisDistributedLock.tryLock(anyString(), anyLong(), any(TimeUnit.class), anyLong(), any(TimeUnit.class)))
                .thenReturn("lock-value");
    }

    @Test
    @DisplayName("审核通过时事务性落库为通过状态，且不发送通知")
    void testHandleCommentModerationEvent_Passed() {
        // Arrange
        when(contentModerationService.moderateComment(anyString()))
                .thenReturn(Result.success(ModerationResult.pass()));

        // Act
        listener.handleCommentModerationEvent(passedEvent);

        // Assert
        verify(commentService).applyModerationResult(1L, true);
        verify(notificationService, never()).createNotification(
                anyLong(), anyLong(), anyInt(), anyLong(), anyInt(), anyString());
        verify(redisDistributedLock).unlock(anyString(), eq("lock-value"));
    }

    @Test
    @DisplayName("审核未通过时事务性落库为拒绝状态并发送通知")
    void testHandleCommentModerationEvent_NotPassed() {
        // Arrange
        ModerationResult failResult = ModerationResult.fail("spam", List.of("垃圾内容"), 0.9, "请修改");
        when(contentModerationService.moderateComment(anyString()))
                .thenReturn(Result.success(failResult));

        // Act
        listener.handleCommentModerationEvent(failedEvent);

        // Assert
        verify(commentService).applyModerationResult(2L, false);
        verify(notificationService).createNotification(
                eq(200L), eq((Long) null), eq(Notification.TYPE_COMMENT_MODERATION_FAILED),
                eq(2L), eq(Notification.TARGET_TYPE_COMMENT), anyString());
    }

    @Test
    @DisplayName("获取锁失败时跳过重复审核")
    void testHandleCommentModerationEvent_LockFailed() {
        // Arrange
        when(redisDistributedLock.tryLock(anyString(), anyLong(), any(TimeUnit.class), anyLong(), any(TimeUnit.class)))
                .thenReturn(null);

        // Act
        listener.handleCommentModerationEvent(passedEvent);

        // Assert
        verify(contentModerationService, never()).moderateComment(anyString());
        verify(commentService, never()).applyModerationResult(anyLong(), anyBoolean());
        verify(redisDistributedLock, never()).unlock(anyString(), anyString());
    }

    @Test
    @DisplayName("AI服务异常时不落库也不发通知")
    void testHandleCommentModerationEvent_Exception() {
        // Arrange
        when(contentModerationService.moderateComment(anyString()))
                .thenThrow(new RuntimeException("AI服务不可用"));

        // Act
        listener.handleCommentModerationEvent(passedEvent);

        // Assert - 异常时不落库也不发通知
        verify(commentService, never()).applyModerationResult(anyLong(), anyBoolean());
        verify(notificationService, never()).createNotification(anyLong(), anyLong(), anyInt(), anyLong(), anyInt(), anyString());
    }

    @Test
    @DisplayName("审核返回异常结果时不处理")
    void testHandleCommentModerationEvent_ModerationResultNull() {
        // Arrange
        when(contentModerationService.moderateComment(anyString()))
                .thenReturn(Result.success(null));

        // Act
        listener.handleCommentModerationEvent(passedEvent);

        // Assert
        verify(commentService, never()).applyModerationResult(anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("审核未通过时通知内容包含原因")
    void testHandleCommentModerationEvent_NotificationContent() {
        // Arrange
        ModerationResult failResult = ModerationResult.fail("spam", List.of("广告内容"), 0.95, "禁止广告");
        when(contentModerationService.moderateComment(anyString()))
                .thenReturn(Result.success(failResult));

        // Act
        listener.handleCommentModerationEvent(failedEvent);

        // Assert
        verify(notificationService).createNotification(
                eq(200L), eq((Long) null), eq(Notification.TYPE_COMMENT_MODERATION_FAILED),
                eq(2L), eq(Notification.TARGET_TYPE_COMMENT), contains("广告内容"));
    }

    @Test
    @DisplayName("长内容会被截断")
    void testHandleCommentModerationEvent_ContentTruncation() {
        // Arrange
        String longContent = "这是一条非常非常非常非常非常非常非常非常非常非常长的评论内容";
        CommentModerationEvent event = new CommentModerationEvent(
                this, 3L, 300L, longContent, "测试文章");

        ModerationResult failResult = ModerationResult.fail("spam", List.of("原因"), 0.9, "修改");
        when(contentModerationService.moderateComment(anyString()))
                .thenReturn(Result.success(failResult));

        // Act
        listener.handleCommentModerationEvent(event);

        // Assert - 通知内容应该被截断
        verify(notificationService).createNotification(
                eq(300L), eq((Long) null), eq(Notification.TYPE_COMMENT_MODERATION_FAILED),
                eq(3L), eq(Notification.TARGET_TYPE_COMMENT), argThat(s -> s.length() <= 50));
    }
}
