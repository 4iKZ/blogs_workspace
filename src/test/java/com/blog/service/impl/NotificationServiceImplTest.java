package com.blog.service.impl;

import com.blog.dto.NotificationDTO;
import com.blog.entity.Article;
import com.blog.entity.Comment;
import com.blog.entity.Notification;
import com.blog.entity.User;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.NotificationMapper;
import com.blog.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationServiceImplTest {

    private final NotificationServiceImpl service = new NotificationServiceImpl();

    @Test
    void createNotification_selfNotification_shouldReturnZero() {
        NotificationMapper mapper = mock(NotificationMapper.class);
        setField(service, "notificationMapper", mapper);

        var result = service.createNotification(1L, 1L, 1, 10L, 1, "content");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(0L);
        verify(mapper, never()).insert(any());
    }

    @Test
    void createNotification_existingUnread_shouldSkip() {
        NotificationMapper mapper = mock(NotificationMapper.class);
        when(mapper.countExistingNotification(1L, 2L, 1, 10L, 1)).thenReturn(1);
        setField(service, "notificationMapper", mapper);

        var result = service.createNotification(1L, 2L, 1, 10L, 1, "content");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(0L);
        verify(mapper, never()).insert(any());
    }

    @Test
    void createNotification_valid_shouldInsertAndReturnId() {
        NotificationMapper mapper = mock(NotificationMapper.class);
        when(mapper.countExistingNotification(1L, 2L, 1, 10L, 1)).thenReturn(0);
        when(mapper.insert(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            setField(n, "id", 99L);
            return 1;
        });
        setField(service, "notificationMapper", mapper);

        var result = service.createNotification(1L, 2L, 1, 10L, 1, "content");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(99L);
        verify(mapper, times(1)).insert(any(Notification.class));
    }

    private static void setField(Notification target, String fieldName, Object value) {
        try {
            var field = Notification.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void createNotification_exception_shouldReturnError() {
        NotificationMapper mapper = mock(NotificationMapper.class);
        when(mapper.countExistingNotification(any(), any(), any(), any(), any())).thenThrow(new RuntimeException("db error"));
        setField(service, "notificationMapper", mapper);

        var result = service.createNotification(1L, 2L, 1, 10L, 1, "content");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("创建通知失败");
    }

    @Test
    void getUnreadCount_shouldReturnCount() {
        NotificationMapper mapper = mock(NotificationMapper.class);
        when(mapper.countUnreadByUserId(1L)).thenReturn(5);
        setField(service, "notificationMapper", mapper);

        var result = service.getUnreadCount(1L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(5);
    }

    @Test
    void getUnreadCount_exception_shouldReturnError() {
        NotificationMapper mapper = mock(NotificationMapper.class);
        when(mapper.countUnreadByUserId(any())).thenThrow(new RuntimeException("db error"));
        setField(service, "notificationMapper", mapper);

        var result = service.getUnreadCount(1L);

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void markAsRead_rowsUpdated_shouldReturnSuccess() {
        NotificationMapper mapper = mock(NotificationMapper.class);
        when(mapper.markAsRead(10L, 1L)).thenReturn(1);
        setField(service, "notificationMapper", mapper);

        var result = service.markAsRead(10L, 1L);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void markAsRead_noRows_shouldReturnNotFound() {
        NotificationMapper mapper = mock(NotificationMapper.class);
        when(mapper.markAsRead(10L, 1L)).thenReturn(0);
        setField(service, "notificationMapper", mapper);

        var result = service.markAsRead(10L, 1L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("消息不存在");
    }

    @Test
    void deleteNotification_rowsDeleted_shouldReturnSuccess() {
        NotificationMapper mapper = mock(NotificationMapper.class);
        when(mapper.deleteByIdAndUserId(10L, 1L)).thenReturn(1);
        setField(service, "notificationMapper", mapper);

        var result = service.deleteNotification(10L, 1L);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void deleteNotification_noRows_shouldReturnNotFound() {
        NotificationMapper mapper = mock(NotificationMapper.class);
        when(mapper.deleteByIdAndUserId(10L, 1L)).thenReturn(0);
        setField(service, "notificationMapper", mapper);

        var result = service.deleteNotification(10L, 1L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("消息不存在");
    }

    @Test
    void getNotificationList_empty_shouldReturnEmptyPage() {
        NotificationMapper notificationMapper = mock(NotificationMapper.class);
        when(notificationMapper.selectByUserId(any(), any(), any())).thenReturn(List.of());
        when(notificationMapper.selectCount(any())).thenReturn(0L);
        setField(service, "notificationMapper", notificationMapper);
        setField(service, "userMapper", mock(UserMapper.class));
        setField(service, "articleMapper", mock(ArticleMapper.class));
        setField(service, "commentMapper", mock(CommentMapper.class));

        var result = service.getNotificationList(1L, 1, 10);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getItems()).isEmpty();
        assertThat(result.getData().getTotal()).isEqualTo(0);
    }

    @Test
    void markAllAsRead_shouldCallMapper() {
        NotificationMapper mapper = mock(NotificationMapper.class);
        setField(service, "notificationMapper", mapper);

        var result = service.markAllAsRead(1L);

        assertThat(result.isSuccess()).isTrue();
        verify(mapper, times(1)).markAllAsRead(1L);
    }

    @Test
    void markAllAsRead_exception_shouldReturnError() {
        NotificationMapper mapper = mock(NotificationMapper.class);
        doThrow(new RuntimeException("db error")).when(mapper).markAllAsRead(any());
        setField(service, "notificationMapper", mapper);

        var result = service.markAllAsRead(1L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("标记所有消息已读失败");
    }

    private static void setField(NotificationServiceImpl target, String fieldName, Object value) {
        try {
            var field = NotificationServiceImpl.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
