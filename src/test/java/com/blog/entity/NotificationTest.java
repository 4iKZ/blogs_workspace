package com.blog.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Notification 常量单元测试")
class NotificationTest {

    @Test
    @DisplayName("通知类型常量值正确")
    void testNotificationTypeConstants() {
        assertEquals(1, Notification.TYPE_ARTICLE_LIKE);
        assertEquals(2, Notification.TYPE_ARTICLE_COMMENT);
        assertEquals(3, Notification.TYPE_COMMENT_LIKE);
        assertEquals(4, Notification.TYPE_COMMENT_REPLY);
        assertEquals(5, Notification.TYPE_USER_FOLLOW);
        assertEquals(6, Notification.TYPE_ARTICLE_MODERATION_FAILED);
        assertEquals(7, Notification.TYPE_ARTICLE_MODERATION_PASSED);
        assertEquals(8, Notification.TYPE_COMMENT_MODERATION_FAILED);
    }

    @Test
    @DisplayName("目标类型常量值正确")
    void testTargetTypeConstants() {
        assertEquals(1, Notification.TARGET_TYPE_ARTICLE);
        assertEquals(2, Notification.TARGET_TYPE_COMMENT);
        assertEquals(3, Notification.TARGET_TYPE_USER);
    }

    @Test
    @DisplayName("已读状态常量值正确")
    void testReadStatusConstants() {
        assertEquals(0, Notification.READ_STATUS_UNREAD);
        assertEquals(1, Notification.READ_STATUS_READ);
    }

    @Test
    @DisplayName("类型常量互不相同")
    void testNotificationTypesAreUnique() {
        int[] types = {
                Notification.TYPE_ARTICLE_LIKE,
                Notification.TYPE_ARTICLE_COMMENT,
                Notification.TYPE_COMMENT_LIKE,
                Notification.TYPE_COMMENT_REPLY,
                Notification.TYPE_USER_FOLLOW,
                Notification.TYPE_ARTICLE_MODERATION_FAILED,
                Notification.TYPE_ARTICLE_MODERATION_PASSED,
                Notification.TYPE_COMMENT_MODERATION_FAILED
        };

        for (int i = 0; i < types.length; i++) {
            for (int j = i + 1; j < types.length; j++) {
                assertNotEquals(types[i], types[j], "通知类型常量应该互不相同");
            }
        }
    }

    @Test
    @DisplayName("目标类型常量互不相同")
    void testTargetTypesAreUnique() {
        assertNotEquals(Notification.TARGET_TYPE_ARTICLE, Notification.TARGET_TYPE_COMMENT);
        assertNotEquals(Notification.TARGET_TYPE_ARTICLE, Notification.TARGET_TYPE_USER);
        assertNotEquals(Notification.TARGET_TYPE_COMMENT, Notification.TARGET_TYPE_USER);
    }
}