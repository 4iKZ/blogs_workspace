package com.blog.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CommentModerationEvent 单元测试")
class CommentModerationEventTest {

    @Test
    @DisplayName("构造方法正确初始化所有字段")
    void testConstructor() {
        Long commentId = 1L;
        Long userId = 100L;
        String content = "评论内容";
        String articleTitle = "文章标题";

        CommentModerationEvent event = new CommentModerationEvent(this, commentId, userId, content, articleTitle);

        assertEquals(commentId, event.getCommentId());
        assertEquals(userId, event.getUserId());
        assertEquals(content, event.getContent());
        assertEquals(articleTitle, event.getArticleTitle());
        assertEquals(this, event.getSource());
    }

    @Test
    @DisplayName("toString 方法返回正确格式")
    void testToString() {
        CommentModerationEvent event = new CommentModerationEvent(
                this, 1L, 100L, "评论内容", "文章标题");

        String result = event.toString();

        assertTrue(result.contains("commentId=1"));
        assertTrue(result.contains("userId=100"));
        assertTrue(result.contains("文章标题"));
    }

    @Test
    @DisplayName("null 内容不导致空指针")
    void testNullContent() {
        CommentModerationEvent event = new CommentModerationEvent(
                this, 1L, 100L, null, "文章标题");

        assertNull(event.getContent());
    }

    @Test
    @DisplayName("空字符串内容正常工作")
    void testEmptyContent() {
        CommentModerationEvent event = new CommentModerationEvent(
                this, 1L, 100L, "", "文章标题");

        assertEquals("", event.getContent());
    }
}