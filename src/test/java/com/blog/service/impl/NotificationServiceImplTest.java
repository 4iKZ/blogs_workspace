package com.blog.service.impl;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.common.ResultCode;
import com.blog.dto.NotificationDTO;
import com.blog.entity.Article;
import com.blog.entity.Comment;
import com.blog.entity.Notification;
import com.blog.entity.User;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.NotificationMapper;
import com.blog.mapper.UserMapper;
import com.blog.service.NotificationService;
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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceImplTest {

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private CommentMapper commentMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

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

    // ==================== createNotification ====================

    @Test
    @DisplayName("创建通知 - 自己给自己发送应返回0")
    void createNotification_selfNotification_shouldReturnZero() {
        var result = notificationService.createNotification(1L, 1L, 1, 10L, 1, "content");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(0L);
        verify(notificationMapper, never()).insert(any());
    }

    @Test
    @DisplayName("创建通知 - 已存在相同未读通知应跳过")
    void createNotification_existingUnread_shouldSkip() {
        when(notificationMapper.countExistingNotification(1L, 2L, 1, 10L, 1)).thenReturn(1);

        var result = notificationService.createNotification(1L, 2L, 1, 10L, 1, "content");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(0L);
        verify(notificationMapper, never()).insert(any());
    }

    @Test
    @DisplayName("创建通知 - 有效通知应插入并返回ID")
    void createNotification_valid_shouldInsertAndReturnId() {
        when(notificationMapper.countExistingNotification(1L, 2L, 1, 10L, 1)).thenReturn(0);
        Notification inserted = new Notification();
        inserted.setId(99L);
        when(notificationMapper.insert(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(99L);
            return 1;
        });

        var result = notificationService.createNotification(1L, 2L, 1, 10L, 1, "content");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(99L);
        verify(notificationMapper, times(1)).insert(any(Notification.class));
    }

    @Test
    @DisplayName("创建通知 - 发生异常应返回错误")
    void createNotification_exception_shouldReturnError() {
        when(notificationMapper.countExistingNotification(any(), any(), any(), any(), any())).thenThrow(new RuntimeException("db error"));

        var result = notificationService.createNotification(1L, 2L, 1, 10L, 1, "content");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("创建通知失败");
    }

    // ==================== getUnreadCount ====================

    @Test
    @DisplayName("获取未读数量 - 应返回计数")
    void getUnreadCount_shouldReturnCount() {
        when(notificationMapper.countUnreadByUserId(1L)).thenReturn(5);

        var result = notificationService.getUnreadCount(1L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(5);
    }

    @Test
    @DisplayName("获取未读数量 - 发生异常应返回错误")
    void getUnreadCount_exception_shouldReturnError() {
        when(notificationMapper.countUnreadByUserId(any())).thenThrow(new RuntimeException("db error"));

        var result = notificationService.getUnreadCount(1L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("获取未读消息数量失败");
    }

    // ==================== getNotificationList ====================

    @Test
    @DisplayName("获取通知列表 - 空列表应返回空页")
    void getNotificationList_empty_shouldReturnEmptyPage() {
        when(notificationMapper.selectByUserId(any(), any(), any())).thenReturn(Collections.emptyList());
        when(notificationMapper.selectCount(any())).thenReturn(0L);

        var result = notificationService.getNotificationList(1L, 1, 10);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getItems()).isEmpty();
        assertThat(result.getData().getTotal()).isEqualTo(0);
    }

    @Test
    @DisplayName("获取通知列表 - 有数据应转换DTO并预加载关联数据")
    void getNotificationList_withData_shouldConvertDTO() {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setSenderId(2L);
        notification.setTargetType(Notification.TARGET_TYPE_ARTICLE);
        notification.setTargetId(10L);
        notification.setType(Notification.TYPE_ARTICLE_LIKE);
        when(notificationMapper.selectByUserId(any(), any(), any())).thenReturn(List.of(notification));
        when(notificationMapper.selectCount(any())).thenReturn(1L);
        User sender = new User();
        sender.setId(2L);
        sender.setNickname("u2");
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(sender));
        Article article = new Article();
        article.setId(10L);
        article.setTitle("a10");
        when(articleMapper.selectBatchIds(any())).thenReturn(List.of(article));

        var result = notificationService.getNotificationList(1L, 1, 10);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getTotal()).isEqualTo(1);
        assertThat(result.getData().getItems()).hasSize(1);
        assertThat(result.getData().getItems().get(0).getSenderNickname()).isEqualTo("u2");
    }

    @Test
    @DisplayName("获取通知列表 - 发生异常应返回错误")
    void getNotificationList_exception_shouldReturnError() {
        when(notificationMapper.selectByUserId(any(), any(), any())).thenThrow(new RuntimeException("db error"));

        var result = notificationService.getNotificationList(1L, 1, 10);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("获取消息列表失败");
    }

    // ==================== markAsRead ====================

    @Test
    @DisplayName("标记已读 - 有记录应成功")
    void markAsRead_rowsUpdated_shouldReturnSuccess() {
        when(notificationMapper.markAsRead(10L, 1L)).thenReturn(1);

        var result = notificationService.markAsRead(10L, 1L);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("标记已读 - 无记录应返回消息不存在")
    void markAsRead_noRows_shouldReturnNotFound() {
        when(notificationMapper.markAsRead(10L, 1L)).thenReturn(0);

        var result = notificationService.markAsRead(10L, 1L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("消息不存在");
    }

    // ==================== deleteNotification ====================

    @Test
    @DisplayName("删除通知 - 有记录应成功")
    void deleteNotification_rowsDeleted_shouldReturnSuccess() {
        when(notificationMapper.deleteByIdAndUserId(10L, 1L)).thenReturn(1);

        var result = notificationService.deleteNotification(10L, 1L);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("删除通知 - 无记录应返回消息不存在")
    void deleteNotification_noRows_shouldReturnNotFound() {
        when(notificationMapper.deleteByIdAndUserId(10L, 1L)).thenReturn(0);

        var result = notificationService.deleteNotification(10L, 1L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("消息不存在");
    }

    // ==================== markAllAsRead ====================

    @Test
    @DisplayName("批量标记已读 - 应调用Mapper")
    void markAllAsRead_shouldCallMapper() {
        var result = notificationService.markAllAsRead(1L);

        assertThat(result.isSuccess()).isTrue();
        verify(notificationMapper, times(1)).markAllAsRead(1L);
    }

    @Test
    @DisplayName("批量标记已读 - 发生异常应返回错误")
    void markAllAsRead_exception_shouldReturnError() {
        doThrow(new RuntimeException("db error")).when(notificationMapper).markAllAsRead(any());

        var result = notificationService.markAllAsRead(1L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("标记所有消息已读失败");
    }

    // ==================== createNotification 扩展 ====================

    @Test
    @DisplayName("创建通知 - insert 异常应返回错误")
    void createNotification_insertException_shouldReturnError() {
        when(notificationMapper.countExistingNotification(any(), any(), any(), any(), any())).thenReturn(0);
        when(notificationMapper.insert(any(Notification.class))).thenThrow(new RuntimeException("db error"));

        var result = notificationService.createNotification(1L, 2L, 1, 10L, 1, "content");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("创建通知失败");
    }

    @Test
    @DisplayName("创建通知 - userId 为 null 应返回错误")
    void createNotification_nullUserId_shouldReturnError() {
        var result = notificationService.createNotification(null, 2L, 1, 10L, 1, "content");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("创建通知失败");
    }

    @Test
    @DisplayName("创建通知 - senderId 为 null 且 userId 为 null 应返回错误")
    void createNotification_bothNull_shouldReturnError() {
        var result = notificationService.createNotification(null, null, 1, 10L, 1, "content");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("创建通知失败");
    }

    // ==================== getNotificationList 扩展 ====================

    @Test
    @DisplayName("获取通知列表 - 发送者不存在时应处理并保留其他字段")
    void getNotificationList_senderNotFound_shouldHandleGracefully() {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setSenderId(2L);
        notification.setTargetType(Notification.TARGET_TYPE_ARTICLE);
        notification.setTargetId(10L);
        notification.setType(Notification.TYPE_ARTICLE_LIKE);
        when(notificationMapper.selectByUserId(any(), any(), any())).thenReturn(List.of(notification));
        when(notificationMapper.selectCount(any())).thenReturn(1L);
        when(userMapper.selectBatchIds(any())).thenReturn(Collections.emptyList());
        Article article = new Article();
        article.setId(10L);
        article.setTitle("a10");
        when(articleMapper.selectBatchIds(any())).thenReturn(List.of(article));

        var result = notificationService.getNotificationList(1L, 1, 10);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getItems()).hasSize(1);
        assertThat(result.getData().getItems().get(0).getSenderNickname()).isNull();
        assertThat(result.getData().getItems().get(0).getSenderAvatar()).isNull();
        assertThat(result.getData().getItems().get(0).getTargetTitle()).isEqualTo("a10");
    }

    @Test
    @DisplayName("获取通知列表 - 文章目标不存在时应返回未知文章")
    void getNotificationList_articleNotFound_shouldHandleGracefully() {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setSenderId(2L);
        notification.setTargetType(Notification.TARGET_TYPE_ARTICLE);
        notification.setTargetId(10L);
        notification.setType(Notification.TYPE_ARTICLE_LIKE);
        when(notificationMapper.selectByUserId(any(), any(), any())).thenReturn(List.of(notification));
        when(notificationMapper.selectCount(any())).thenReturn(1L);
        User sender = new User();
        sender.setId(2L);
        sender.setNickname("u2");
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(sender));
        when(articleMapper.selectBatchIds(any())).thenReturn(Collections.emptyList());

        var result = notificationService.getNotificationList(1L, 1, 10);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getItems()).hasSize(1);
        assertThat(result.getData().getItems().get(0).getTargetTitle()).isEqualTo("未知文章");
    }

    @Test
    @DisplayName("获取通知列表 - 评论目标不存在时应返回未知评论")
    void getNotificationList_commentNotFound_shouldHandleGracefully() {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setSenderId(2L);
        notification.setTargetType(Notification.TARGET_TYPE_COMMENT);
        notification.setTargetId(20L);
        notification.setType(Notification.TYPE_COMMENT_LIKE);
        when(notificationMapper.selectByUserId(any(), any(), any())).thenReturn(List.of(notification));
        when(notificationMapper.selectCount(any())).thenReturn(1L);
        User sender = new User();
        sender.setId(2L);
        sender.setNickname("u2");
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(sender));
        when(commentMapper.selectBatchIds(any())).thenReturn(Collections.emptyList());

        var result = notificationService.getNotificationList(1L, 1, 10);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getItems()).hasSize(1);
        assertThat(result.getData().getItems().get(0).getTargetTitle()).isEqualTo("未知评论");
    }

    @Test
    @DisplayName("获取通知列表 - 未知目标类型应返回未知")
    void getNotificationList_unknownTargetType_shouldHandleGracefully() {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setSenderId(2L);
        notification.setTargetType(999);
        notification.setTargetId(10L);
        notification.setType(Notification.TYPE_ARTICLE_LIKE);
        when(notificationMapper.selectByUserId(any(), any(), any())).thenReturn(List.of(notification));
        when(notificationMapper.selectCount(any())).thenReturn(1L);
        User sender = new User();
        sender.setId(2L);
        sender.setNickname("u2");
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(sender));

        var result = notificationService.getNotificationList(1L, 1, 10);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getItems()).hasSize(1);
        assertThat(result.getData().getItems().get(0).getTargetTitle()).isEqualTo("未知");
    }

    @Test
    @DisplayName("获取通知列表 - 未知通知类型应返回未知通知")
    void getNotificationList_unknownNotificationType_shouldReturnUnknownName() {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setSenderId(2L);
        notification.setTargetType(Notification.TARGET_TYPE_ARTICLE);
        notification.setTargetId(10L);
        notification.setType(999);
        when(notificationMapper.selectByUserId(any(), any(), any())).thenReturn(List.of(notification));
        when(notificationMapper.selectCount(any())).thenReturn(1L);
        User sender = new User();
        sender.setId(2L);
        sender.setNickname("u2");
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(sender));
        Article article = new Article();
        article.setId(10L);
        article.setTitle("a10");
        when(articleMapper.selectBatchIds(any())).thenReturn(List.of(article));

        var result = notificationService.getNotificationList(1L, 1, 10);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getItems()).hasSize(1);
        assertThat(result.getData().getItems().get(0).getTypeName()).isEqualTo("未知通知");
    }

    @Test
    @DisplayName("获取通知列表 - 无文章类型通知不应查询 articleMapper")
    void getNotificationList_emptyArticleIds_shouldNotQueryArticleMapper() {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setSenderId(2L);
        notification.setTargetType(Notification.TARGET_TYPE_COMMENT);
        notification.setTargetId(20L);
        notification.setType(Notification.TYPE_COMMENT_LIKE);
        when(notificationMapper.selectByUserId(any(), any(), any())).thenReturn(List.of(notification));
        when(notificationMapper.selectCount(any())).thenReturn(1L);
        User sender = new User();
        sender.setId(2L);
        sender.setNickname("u2");
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(sender));
        Comment comment = new Comment();
        comment.setId(20L);
        comment.setContent("c20");
        when(commentMapper.selectBatchIds(any())).thenReturn(List.of(comment));

        var result = notificationService.getNotificationList(1L, 1, 10);

        assertThat(result.isSuccess()).isTrue();
        verify(articleMapper, never()).selectBatchIds(any());
    }

    @Test
    @DisplayName("获取通知列表 - 无评论类型通知不应查询 commentMapper")
    void getNotificationList_emptyCommentIds_shouldNotQueryCommentMapper() {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setSenderId(2L);
        notification.setTargetType(Notification.TARGET_TYPE_ARTICLE);
        notification.setTargetId(10L);
        notification.setType(Notification.TYPE_ARTICLE_LIKE);
        when(notificationMapper.selectByUserId(any(), any(), any())).thenReturn(List.of(notification));
        when(notificationMapper.selectCount(any())).thenReturn(1L);
        User sender = new User();
        sender.setId(2L);
        sender.setNickname("u2");
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(sender));
        Article article = new Article();
        article.setId(10L);
        article.setTitle("a10");
        when(articleMapper.selectBatchIds(any())).thenReturn(List.of(article));

        var result = notificationService.getNotificationList(1L, 1, 10);

        assertThat(result.isSuccess()).isTrue();
        verify(commentMapper, never()).selectBatchIds(any());
    }

    @Test
    @DisplayName("获取通知列表 - 昵称为空应降级使用用户名")
    void getNotificationList_senderNicknameNull_shouldFallbackToUsername() {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setSenderId(2L);
        notification.setTargetType(Notification.TARGET_TYPE_ARTICLE);
        notification.setTargetId(10L);
        notification.setType(Notification.TYPE_ARTICLE_LIKE);
        when(notificationMapper.selectByUserId(any(), any(), any())).thenReturn(List.of(notification));
        when(notificationMapper.selectCount(any())).thenReturn(1L);
        User sender = new User();
        sender.setId(2L);
        sender.setNickname(null);
        sender.setUsername("u2");
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(sender));
        Article article = new Article();
        article.setId(10L);
        article.setTitle("a10");
        when(articleMapper.selectBatchIds(any())).thenReturn(List.of(article));

        var result = notificationService.getNotificationList(1L, 1, 10);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getItems()).hasSize(1);
        assertThat(result.getData().getItems().get(0).getSenderNickname()).isEqualTo("u2");
    }

    // ==================== markAsRead / deleteNotification 异常扩展 ====================

    @Test
    @DisplayName("标记已读 - 发生异常应返回错误")
    void markAsRead_exception_shouldReturnError() {
        doThrow(new RuntimeException("db error")).when(notificationMapper).markAsRead(any(), any());

        var result = notificationService.markAsRead(10L, 1L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("标记消息已读失败");
    }

    @Test
    @DisplayName("删除通知 - 发生异常应返回错误")
    void deleteNotification_exception_shouldReturnError() {
        doThrow(new RuntimeException("db error")).when(notificationMapper).deleteByIdAndUserId(any(), any());

        var result = notificationService.deleteNotification(10L, 1L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("删除消息失败");
    }
}
