package com.blog.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 文章浏览量变化事件
 */
@Getter
public class ArticleViewCountChangeEvent extends ApplicationEvent {
    
    private final Long articleId;
    
    public ArticleViewCountChangeEvent(Object source, Long articleId) {
        super(source);
        this.articleId = articleId;
    }
}