package com.blog.event;

import com.blog.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文章事件监听器，用于处理文章相关事件，更新Redis缓存
 */
@Component
@Slf4j
public class ArticleEventListener {

    @Autowired
    private RedisUtils redisUtils;

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
        // 使用Redis的keys命令查找所有热门文章缓存键并删除
        // 排除 ZSet 键（排行榜数据），只删除查询结果缓存
        Set<String> keys = redisUtils.scanKeys("hot:articles:*");
        if (keys != null && !keys.isEmpty()) {
            // 过滤掉 ZSet 排行榜键（使用前缀匹配，兼容带日期后缀的新格式）
            Set<String> keysToDelete = keys.stream()
                    .filter(key -> !key.startsWith("hot:articles:zset:day:")
                            && !key.startsWith("hot:articles:zset:week:"))
                    .collect(Collectors.toSet());

            if (!keysToDelete.isEmpty()) {
                long deletedCount = redisUtils.delete(keysToDelete);
                log.info("成功清除热门文章缓存，数量：{}", deletedCount);
            } else {
                log.info("没有找到需要清除的热门文章缓存键（已排除排行榜键）");
            }
        } else {
            log.info("没有找到热门文章缓存键");
        }
    }
}