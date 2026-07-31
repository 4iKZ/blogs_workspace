package com.blog.mapper;

import com.blog.entity.Notification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dao-test")
@DisplayName("NotificationMapper DAO 直测")
class NotificationMapperDaoTest {

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanup() {
        jdbcTemplate.execute("DELETE FROM notifications WHERE content LIKE 'dao-test-%'");
    }

    @Test
    @DisplayName("未读通知计数、列表、标记已读与重复检查")
    void notificationLifecycle_shouldPersistAndReturnRows() {
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setSenderId(userId);
        notification.setType(Notification.TYPE_ARTICLE_LIKE);
        notification.setTargetId(1L);
        notification.setTargetType(Notification.TARGET_TYPE_ARTICLE);
        notification.setContent("dao-test-notification");
        notification.setIsRead(Notification.READ_STATUS_UNREAD);
        notificationMapper.insert(notification);

        int unread = notificationMapper.countUnreadByUserId(userId);
        assertThat(unread).isGreaterThanOrEqualTo(1);

        List<Notification> notifications = notificationMapper.selectByUserId(userId, 0, 10);
        assertThat(notifications).extracting(Notification::getContent).contains("dao-test-notification");

        int marked = notificationMapper.markAsRead(notification.getId(), userId);
        assertThat(marked).isEqualTo(1);

        int existing = notificationMapper.countExistingNotification(userId, userId, Notification.TYPE_ARTICLE_LIKE, 1L, Notification.TARGET_TYPE_ARTICLE);
        assertThat(existing).isEqualTo(0);
    }

    @Test
    @DisplayName("批量标记所有通知为已读")
    void markAllAsRead_shouldUpdateRows() {
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setSenderId(userId);
        notification.setType(Notification.TYPE_ARTICLE_COMMENT);
        notification.setTargetId(1L);
        notification.setTargetType(Notification.TARGET_TYPE_ARTICLE);
        notification.setContent("dao-test-batch-read");
        notification.setIsRead(Notification.READ_STATUS_UNREAD);
        notificationMapper.insert(notification);

        int updated = notificationMapper.markAllAsRead(userId);
        assertThat(updated).isGreaterThanOrEqualTo(1);

        int unread = notificationMapper.countUnreadByUserId(userId);
        assertThat(unread).isEqualTo(0);
    }
}
