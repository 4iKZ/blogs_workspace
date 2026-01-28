package com.blog.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 文章点赞数变化事件
 */
@Getter
public class ArticleLikeCountChangeEvent extends ApplicationEvent {
    
    private final Long articleId;
    
    public ArticleLikeCountChangeEvent(Object source, Long articleId) {
        super(source);
        this.articleId = articleId;
    }
}