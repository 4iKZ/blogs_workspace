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
        log.info("文章浏览量变化，触发缓存更新，文章ID：{}", event.getArticleId());
        // 清除热门文章缓存，下次查询时会重新加载
        clearHotArticlesCache();
    }

    /**
     * 监听文章点赞数变化事件
     */
    @EventListener
    public void handleArticleLikeCountChange(ArticleLikeCountChangeEvent event) {
        log.info("文章点赞数变化，触发缓存更新，文章ID：{}", event.getArticleId());
        // 清除热门文章缓存，下次查询时会重新加载
        clearHotArticlesCache();
    }

    /**
     * 清除热门文章缓存
     * 注意：只清除查询结果缓存，不清除 ZSet 排行榜数据
     */
    private void clearHotArticlesCache() {
        hotArticleCacheEvictionService.evictAll();
        log.info("成功清除热门文章结果缓存（Spring Cache）");
    }
}