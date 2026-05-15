package com.blog.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ModerationEvent 单元测试")
class ModerationEventTest {

    @Test
    @DisplayName("构造方法正确初始化所有字段")
    void testConstructor() {
        Long articleId = 1L;
        String title = "测试标题";
        String content = "测试内容";
        Long authorId = 100L;
        ModerationEvent.ModerationType type = ModerationEvent.ModerationType.NEW_PUBLISH;

        ModerationEvent event = new ModerationEvent(this, articleId, title, content, authorId, type);

        assertEquals(articleId, event.getArticleId());
        assertEquals(title, event.getTitle());
        assertEquals(content, event.getContent());
        assertEquals(authorId, event.getAuthorId());
        assertEquals(type, event.getType());
        assertEquals(this, event.getSource());
    }

    @Test
    @DisplayName("toString 方法返回正确格式")
    void testToString() {
        ModerationEvent event = new ModerationEvent(
                this, 1L, "标题", "内容", 100L, ModerationEvent.ModerationType.NEW_PUBLISH);

        String result = event.toString();

        assertTrue(result.contains("articleId=1"));
        assertTrue(result.contains("标题"));
        assertTrue(result.contains("authorId=100"));
        assertTrue(result.contains("NEW_PUBLISH"));
    }

    @Test
    @DisplayName("NEW_PUBLISH 类型正确")
    void testNewPublishType() {
        ModerationEvent event = new ModerationEvent(
                this, 1L, "标题", "内容", 100L, ModerationEvent.ModerationType.NEW_PUBLISH);
        assertEquals(ModerationEvent.ModerationType.NEW_PUBLISH, event.getType());
    }

    @Test
    @DisplayName("RE_PUBLISH 类型正确")
    void testRePublishType() {
        ModerationEvent event = new ModerationEvent(
                this, 1L, "标题", "内容", 100L, ModerationEvent.ModerationType.RE_PUBLISH);
        assertEquals(ModerationEvent.ModerationType.RE_PUBLISH, event.getType());
    }
}