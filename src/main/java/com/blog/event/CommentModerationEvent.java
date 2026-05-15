package com.blog.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 评论审核事件
 * 用于异步执行AI评论内容审核
 */
@Getter
public class CommentModerationEvent extends ApplicationEvent {

    private final Long commentId;
    private final Long userId;
    private final String content;
    private final String articleTitle;

    public CommentModerationEvent(Object source, Long commentId, Long userId, String content, String articleTitle) {
        super(source);
        this.commentId = commentId;
        this.userId = userId;
        this.content = content;
        this.articleTitle = articleTitle;
    }

    @Override
    public String toString() {
        return "CommentModerationEvent{" +
                "commentId=" + commentId +
                ", userId=" + userId +
                ", articleTitle='" + articleTitle + '\'' +
                '}';
    }
}
