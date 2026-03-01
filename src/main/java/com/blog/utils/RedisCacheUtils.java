package com.blog.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis缓存工具类
 */
@Component
public class RedisCacheUtils {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 设置缓存
     * @param key 缓存键
     * @param value 缓存值
     * @param timeout 过期时间
     * @param timeUnit 时间单位
     */
    public void setCache(String key, Object value, long timeout, TimeUnit timeUnit) {
        ValueOperations<String, Object> operations = redisTemplate.opsForValue();
        operations.set(key, value, timeout, timeUnit);
    }

    /**
     * 获取缓存
     * @param key 缓存键
     * @return 缓存值
     */
    public Object getCache(String key) {
        ValueOperations<String, Object> operations = redisTemplate.opsForValue();
        return operations.get(key);
    }

    /**
     * 删除缓存
     * @param key 缓存键
     */
    public void deleteCache(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 缓存递增
     * @param key 缓存键
     * @param delta 递增步长
     * @return 递增后的值
     */
    public Long incrementCache(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 缓存递减
     * @param key 缓存键
     * @param delta 递减步长
     * @return 递减后的值
     */
    public Long decrementCache(String key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }

    /**
     * 判断缓存是否存在
     * @param key 缓存键
     * @return 是否存在
     */
    public boolean hasCache(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 设置缓存过期时间
     * @param key 缓存键
     * @param timeout 过期时间
     * @param timeUnit 时间单位
     */
    public void expireCache(String key, long timeout, TimeUnit timeUnit) {
        redisTemplate.expire(key, timeout, timeUnit);
    }

    /**
     * 获取缓存过期时间
     * @param key 缓存键
     * @return 过期时间（秒）
     */
    public Long getCacheExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    // 评论相关缓存键前缀
    public static final String COMMENT_LIST_KEY_PREFIX = "comment:list:";
    public static final String COMMENT_TREE_KEY_PREFIX = "comment:tree:";
    public static final String COMMENT_COUNT_KEY_PREFIX = "comment:count:";
    public static final String COMMENT_DETAIL_KEY_PREFIX = "comment:detail:";
    public static final String COMMENT_LIKE_KEY_PREFIX = "comment:like:";
    public static final String COMMENT_HOT_KEY_PREFIX = "comment:hot:";
    public static final String SENSITIVE_WORDS_KEY = "sensitive:words";

    // 文章浏览量相关缓存键前缀
    public static final String ARTICLE_VIEW_COUNT_PREFIX = "article:view:count:";
    public static final String ARTICLE_VIEW_QUEUE_KEY = "article:view:queue";

    // 文章点赞/收藏状态缓存键前缀
    public static final String ARTICLE_LIKE_KEY_PREFIX = "article:like:";
    public static final String ARTICLE_FAVORITE_KEY_PREFIX = "article:favorite:";

    /**
     * 生成评论列表缓存键
     * @param articleId 文章ID
     * @param page 页码
     * @param size 每页数量
     * @param sortBy 排序方式
     * @return 缓存键
     */
    public static String generateCommentListKey(Long articleId, Integer page, Integer size, String sortBy) {
        return COMMENT_LIST_KEY_PREFIX + articleId + ":" + page + ":" + size + ":" + sortBy;
    }

    /**
     * 生成评论树缓存键
     * @param articleId 文章ID
     * @param page 页码
     * @param size 每页数量
     * @return 缓存键
     */
    public static String generateCommentTreeKey(Long articleId, Integer page, Integer size) {
        return COMMENT_TREE_KEY_PREFIX + articleId + ":" + page + ":" + size;
    }

    /**
     * 生成评论计数缓存键
     * @param articleId 文章ID
     * @return 缓存键
     */
    public static String generateCommentCountKey(Long articleId) {
        return COMMENT_COUNT_KEY_PREFIX + articleId;
    }

    /**
     * 生成评论详情缓存键
     * @param commentId 评论ID
     * @return 缓存键
     */
    public static String generateCommentDetailKey(Long commentId) {
        return COMMENT_DETAIL_KEY_PREFIX + commentId;
    }

    /**
     * 生成评论点赞状态缓存键
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 缓存键
     */
    public static String generateCommentLikeKey(Long commentId, Long userId) {
        return COMMENT_LIKE_KEY_PREFIX + commentId + ":" + userId;
    }

    /**
     * 生成热门评论缓存键
     * @param articleId 文章ID
     * @return 缓存键
     */
    public static String generateHotCommentsKey(Long articleId) {
        return COMMENT_HOT_KEY_PREFIX + articleId;
    }

    /**
     * 生成文章浏览量缓存键
     * @param articleId 文章ID
     * @return 缓存键
     */
    public static String generateArticleViewCountKey(Long articleId) {
        return ARTICLE_VIEW_COUNT_PREFIX + articleId;
    }

    /**
     * 生成文章点赞状态缓存键
     * @param articleId 文章ID
     * @param userId 用户ID
     * @return 缓存键
     */
    public static String generateArticleLikeKey(Long articleId, Long userId) {
        return ARTICLE_LIKE_KEY_PREFIX + articleId + ":" + userId;
    }

    /**
     * 生成文章收藏状态缓存键
     * @param articleId 文章ID
     * @param userId 用户ID
     * @return 缓存键
     */
    public static String generateArticleFavoriteKey(Long articleId, Long userId) {
        return ARTICLE_FAVORITE_KEY_PREFIX + articleId + ":" + userId;
    }
}
