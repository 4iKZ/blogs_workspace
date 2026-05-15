package com.blog.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 内容审核事件
 * 用于异步执行AI内容审核，避免阻塞主业务流程
 */
@Getter
public class ModerationEvent extends ApplicationEvent {

    /**
     * 审核类型
     */
    public enum ModerationType {
        NEW_PUBLISH,  // 新发布
        RE_PUBLISH    // 重新发布（从草稿）
    }

    private final Long articleId;
    private final String title;
    private final String content;
    private final Long authorId;
    private final ModerationType type;

    public ModerationEvent(Object source, Long articleId, String title, String content,
                          Long authorId, ModerationType type) {
        super(source);
        this.articleId = articleId;
        this.title = title;
        this.content = content;
        this.authorId = authorId;
        this.type = type;
    }

    @Override
    public String toString() {
        return "ModerationEvent{" +
                "articleId=" + articleId +
                ", title='" + title + '\'' +
                ", authorId=" + authorId +
                ", type=" + type +
                '}';
    }
}
