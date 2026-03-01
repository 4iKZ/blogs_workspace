package com.blog.schedule;

import com.blog.config.CacheConsistencyConfig;
import com.blog.entity.UserFavorite;
import com.blog.entity.UserLike;
import com.blog.mapper.UserFavoriteMapper;
import com.blog.mapper.UserLikeMapper;
import com.blog.utils.RedisCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 缓存一致性验证任务
 * 
 * 定期检查缓存与数据库的一致性，发现不一致时自动修复
 */
@Slf4j
@Component
public class CacheConsistencyVerifier {

    @Autowired
    private UserLikeMapper userLikeMapper;

    @Autowired
    private UserFavoriteMapper userFavoriteMapper;

    @Autowired
    private RedisCacheUtils redisCacheUtils;

    @Autowired
    private CacheConsistencyConfig cacheConfig;

    /**
     * 验证点赞状态缓存一致性
     * 每5分钟执行一次
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void verifyLikeStatusConsistency() {
        if (!cacheConfig.isEnabled() || !cacheConfig.isEnableVerification()) {
            return;
        }

        try {
            int sampleSize = cacheConfig.getVerificationSampleSize();
            List<UserLike> likes = userLikeMapper.selectRecentRecords(sampleSize);

            if (likes == null || likes.isEmpty()) {
                log.debug("没有点赞记录需要验证");
                return;
            }

            AtomicInteger inconsistentCount = new AtomicInteger(0);
            AtomicInteger fixedCount = new AtomicInteger(0);

            for (UserLike like : likes) {
                String cacheKey = RedisCacheUtils.generateArticleLikeKey(
                    like.getArticleId(), like.getUserId()
                );

                Boolean cached = (Boolean) redisCacheUtils.getCache(cacheKey);
                boolean expected = true;

                if (cached != null && cached != expected) {
                    inconsistentCount.incrementAndGet();
                    log.warn("点赞缓存不一致: key={}, cached={}, expected={}", 
                        cacheKey, cached, expected);

                    redisCacheUtils.deleteCache(cacheKey);
                    fixedCount.incrementAndGet();
                }
            }

            if (inconsistentCount.get() > 0) {
                log.warn("点赞缓存一致性验证完成，抽样数量: {}，不一致: {}，已修复: {}", 
                    likes.size(), inconsistentCount.get(), fixedCount.get());
            } else {
                log.debug("点赞缓存一致性验证通过，抽样数量: {}", likes.size());
            }

        } catch (Exception e) {
            log.error("点赞缓存一致性验证失败", e);
        }
    }

    /**
     * 验证收藏状态缓存一致性
     * 每5分钟执行一次
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void verifyFavoriteStatusConsistency() {
        if (!cacheConfig.isEnabled() || !cacheConfig.isEnableVerification()) {
            return;
        }

        try {
            int sampleSize = cacheConfig.getVerificationSampleSize();
            List<UserFavorite> favorites = userFavoriteMapper.selectRecentRecords(sampleSize);

            if (favorites == null || favorites.isEmpty()) {
                log.debug("没有收藏记录需要验证");
                return;
            }

            AtomicInteger inconsistentCount = new AtomicInteger(0);
            AtomicInteger fixedCount = new AtomicInteger(0);

            for (UserFavorite favorite : favorites) {
                String cacheKey = RedisCacheUtils.generateArticleFavoriteKey(
                    favorite.getArticleId(), favorite.getUserId()
                );

                Boolean cached = (Boolean) redisCacheUtils.getCache(cacheKey);
                boolean expected = true;

                if (cached != null && cached != expected) {
                    inconsistentCount.incrementAndGet();
                    log.warn("收藏缓存不一致: key={}, cached={}, expected={}", 
                        cacheKey, cached, expected);

                    redisCacheUtils.deleteCache(cacheKey);
                    fixedCount.incrementAndGet();
                }
            }

            if (inconsistentCount.get() > 0) {
                log.warn("收藏缓存一致性验证完成，抽样数量: {}，不一致: {}，已修复: {}", 
                    favorites.size(), inconsistentCount.get(), fixedCount.get());
            } else {
                log.debug("收藏缓存一致性验证通过，抽样数量: {}", favorites.size());
            }

        } catch (Exception e) {
            log.error("收藏缓存一致性验证失败", e);
        }
    }

    /**
     * 清理过期缓存
     * 每小时执行一次
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void cleanupExpiredCaches() {
        if (!cacheConfig.isEnabled()) {
            return;
        }

        try {
            log.info("开始清理过期缓存...");

            // 这里可以添加清理逻辑
            // 例如：清理已删除文章的点赞/收藏缓存

            log.info("过期缓存清理完成");
        } catch (Exception e) {
            log.error("过期缓存清理失败", e);
        }
    }
}
