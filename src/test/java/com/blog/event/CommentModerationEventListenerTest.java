package com.blog.event;

import com.blog.common.Result;
import com.blog.dto.ModerationResult;
import com.blog.entity.Comment;
import com.blog.entity.Notification;
import com.blog.mapper.CommentMapper;
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

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentModerationEventListener 单元测试")
class CommentModerationEventListenerTest {

    @Mock
    private ContentModerationService contentModerationService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private RedisDistributedLock redisDistributedLock;

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
    }

    @Test
    @DisplayName("审核通过时更新评论状态为已通过")
    void testHandleCommentModerationEvent_Passed() {
        // Arrange
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setStatus(1); // 待审核

        when(redisDistributedLock.tryLock(anyString(), anyLong(), any(TimeUnit.class), anyLong(), any(TimeUnit.class)))
                .thenReturn("lock-value");
        when(contentModerationService.moderateComment(anyString()))
                .thenReturn(Result.success(ModerationResult.pass()));
        when(commentMapper.selectById(1L)).thenReturn(comment);

        // Act
        listener.handleCommentModerationEvent(passedEvent);

        // Assert
        verify(commentMapper).updateById(comment);
        assertEquals(2, comment.getStatus()); // 已通过状态
        verify(redisDistributedLock).unlock(anyString(), eq("lock-value"));
    }

    @Test
    @DisplayName("审核未通过时更新评论状态为已拒绝并发送通知")
    void testHandleCommentModerationEvent_NotPassed() {
        // Arrange
        Comment comment = new Comment();
        comment.setId(2L);
        comment.setStatus(1); // 待审核

        ModerationResult failResult = ModerationResult.fail("spam", List.of("垃圾内容"), 0.9, "请修改");

        when(redisDistributedLock.tryLock(anyString(), anyLong(), any(TimeUnit.class), anyLong(), any(TimeUnit.class)))
                .thenReturn("lock-value");
        when(contentModerationService.moderateComment(anyString()))
                .thenReturn(Result.success(failResult));
        when(commentMapper.selectById(2L)).thenReturn(comment);

        // Act
        listener.handleCommentModerationEvent(failedEvent);

        // Assert
        verify(commentMapper).updateById(comment);
        assertEquals(3, comment.getStatus()); // 已拒绝状态

        verify(notificationService).createNotification(
                eq(200L), eq(0L), eq(Notification.TYPE_COMMENT_MODERATION_FAILED),
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
        verify(commentMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("AI服务异常时不更新评论状态")
    void testHandleCommentModerationEvent_Exception() {
        // Arrange
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setStatus(1);

        when(redisDistributedLock.tryLock(anyString(), anyLong(), any(TimeUnit.class), anyLong(), any(TimeUnit.class)))
                .thenReturn("lock-value");
        when(contentModerationService.moderateComment(anyString()))
                .thenThrow(new RuntimeException("AI服务不可用"));

        // Act
        listener.handleCommentModerationEvent(passedEvent);

        // Assert - 异常时不更新状态也不发通知
        verify(commentMapper, never()).updateById(any());
        verify(notificationService, never()).createNotification(anyLong(), anyLong(), anyInt(), anyLong(), anyInt(), anyString());
    }

    @Test
    @DisplayName("审核返回异常结果时不处理")
    void testHandleCommentModerationEvent_ModerationResultNull() {
        // Arrange
        when(redisDistributedLock.tryLock(anyString(), anyLong(), any(TimeUnit.class), anyLong(), any(TimeUnit.class)))
                .thenReturn("lock-value");
        when(contentModerationService.moderateComment(anyString()))
                .thenReturn(Result.success(null));

        // Act
        listener.handleCommentModerationEvent(passedEvent);

        // Assert
        verify(commentMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("评论不存在时不处理")
    void testHandleCommentModerationEvent_CommentNotFound() {
        // Arrange
        when(redisDistributedLock.tryLock(anyString(), anyLong(), any(TimeUnit.class), anyLong(), any(TimeUnit.class)))
                .thenReturn("lock-value");
        when(contentModerationService.moderateComment(anyString()))
                .thenReturn(Result.success(ModerationResult.pass()));
        when(commentMapper.selectById(1L)).thenReturn(null);

        // Act
        listener.handleCommentModerationEvent(passedEvent);

        // Assert
        verify(commentMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("审核未通过时通知内容包含原因")
    void testHandleCommentModerationEvent_NotificationContent() {
        // Arrange
        Comment comment = new Comment();
        comment.setId(2L);
        comment.setStatus(1);

        ModerationResult failResult = ModerationResult.fail("spam", List.of("广告内容"), 0.95, "禁止广告");

        when(redisDistributedLock.tryLock(anyString(), anyLong(), any(TimeUnit.class), anyLong(), any(TimeUnit.class)))
                .thenReturn("lock-value");
        when(contentModerationService.moderateComment(anyString()))
                .thenReturn(Result.success(failResult));
        when(commentMapper.selectById(2L)).thenReturn(comment);

        // Act
        listener.handleCommentModerationEvent(failedEvent);

        // Assert
        verify(notificationService).createNotification(
                eq(200L), eq(0L), eq(Notification.TYPE_COMMENT_MODERATION_FAILED),
                eq(2L), eq(Notification.TARGET_TYPE_COMMENT), contains("广告内容"));
    }

    @Test
    @DisplayName("长内容会被截断")
    void testHandleCommentModerationEvent_ContentTruncation() {
        // Arrange
        String longContent = "这是一条非常非常非常非常非常非常非常非常非常非常长的评论内容";
        CommentModerationEvent event = new CommentModerationEvent(
                this, 3L, 300L, longContent, "测试文章");

        Comment comment = new Comment();
        comment.setId(3L);
        comment.setStatus(1);

        ModerationResult failResult = ModerationResult.fail("spam", List.of("原因"), 0.9, "修改");

        when(redisDistributedLock.tryLock(anyString(), anyLong(), any(TimeUnit.class), anyLong(), any(TimeUnit.class)))
                .thenReturn("lock-value");
        when(contentModerationService.moderateComment(anyString()))
                .thenReturn(Result.success(failResult));
        when(commentMapper.selectById(3L)).thenReturn(comment);

        // Act
        listener.handleCommentModerationEvent(event);

        // Assert - 通知内容应该被截断
        verify(notificationService).createNotification(
                eq(300L), eq(0L), eq(Notification.TYPE_COMMENT_MODERATION_FAILED),
                eq(3L), eq(Notification.TARGET_TYPE_COMMENT), argThat(s -> s.length() <= 50));
    }
}