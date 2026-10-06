package com.blog.event;

import com.blog.utils.HotArticleCacheEvictionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 文章事件监听器，用于处理文章相关事件并清理热门文章结果缓存。
 */
@Component
@Slf4j
public class ArticleEventListener {

    @Autowired
    private HotArticleCacheEvictionService hotArticleCacheEvictionService;

    /**
     * 监听文章浏览量变化事件
     */
    @EventListener
    public void handleArticleViewCountChange(ArticleViewCountChangeEvent event) {
        log.debug("文章浏览量变化，触发缓存更新，文章ID：{}", event.getArticleId());
        // 清除热门文章缓存，下次查询时会重新加载
        clearHotArticlesCache();
    }

    /**
     * 监听文章点赞数变化事件
     */
    @EventListener
    public void handleArticleLikeCountChange(ArticleLikeCountChangeEvent event) {
        log.debug("文章点赞数变化，触发缓存更新，文章ID：{}", event.getArticleId());
        // 清除热门文章缓存，下次查询时会重新加载
        clearHotArticlesCache();
    }

    /**
     * 清除热门文章缓存
     * 浏览量/点赞为高频事件，使用节流失效避免 30s TTL 缓存被反复清空。
     * 注意：只清除查询结果缓存，不清除 ZSet 排行榜数据
     */
    private void clearHotArticlesCache() {
        hotArticleCacheEvictionService.evictAllThrottled();
        log.debug("已触发热门文章结果缓存失效（节流）");
    }
}