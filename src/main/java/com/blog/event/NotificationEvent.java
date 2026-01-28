package com.blog.event;

import org.springframework.context.ApplicationEvent;

/**
 * 通知事件
 * 用于异步创建通知，避免阻塞主业务流程
 */
public class NotificationEvent extends ApplicationEvent {

    private final Long userId;
    private final Long senderId;
    private final Integer type;
    private final Long targetId;
    private final Integer targetType;
    private final String content;

    public NotificationEvent(Object source, Long userId, Long senderId,
                            Integer type, Long targetId, Integer targetType, String content) {
        super(source);
        this.userId = userId;
        this.senderId = senderId;
        this.type = type;
        this.targetId = targetId;
        this.targetType = targetType;
        this.content = content;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public Integer getType() {
        return type;
    }

    public Long getTargetId() {
        return targetId;
    }

    public Integer getTargetType() {
        return targetType;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "NotificationEvent{" +
                "userId=" + userId +
                ", senderId=" + senderId +
                ", type=" + type +
                ", targetId=" + targetId +
                ", targetType=" + targetType +
                ", content='" + content + '\'' +
                '}';
    }
}
