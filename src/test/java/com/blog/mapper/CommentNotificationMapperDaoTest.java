package com.blog.mapper;

import com.blog.entity.CommentNotification;
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
@DisplayName("CommentNotificationMapper DAO 直测")
class CommentNotificationMapperDaoTest {

    @Autowired
    private CommentNotificationMapper commentNotificationMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanup() {
        jdbcTemplate.execute("DELETE FROM comment_notifications WHERE receiver_id = 999");
    }

    @Test
    @DisplayName("通知未读计数、列表、标记已读与删除")
    void commentNotification_shouldPersistAndReturnRows() {
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        jdbcTemplate.execute(
                "INSERT INTO comment_notifications (comment_id, receiver_id, type, read_status, create_time, update_time) " +
                        "VALUES (1," + userId + ",1,0,NOW(),NOW())"
        );

        assertThat(commentNotificationMapper.getUnreadNotificationCount(userId)).isGreaterThanOrEqualTo(1);

        List<CommentNotification> notifications = commentNotificationMapper.getUserNotifications(userId, 0, 10);
        assertThat(notifications).isNotEmpty();

        int marked = commentNotificationMapper.markNotificationAsRead(notifications.get(0).getId());
        assertThat(marked).isEqualTo(1);

        int markedAll = commentNotificationMapper.markAllNotificationsAsRead(userId);
        assertThat(markedAll).isGreaterThanOrEqualTo(1);

        int deleted = commentNotificationMapper.deleteByCommentId(1L);
        assertThat(deleted).isGreaterThanOrEqualTo(0);
    }
}
