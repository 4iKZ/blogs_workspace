package com.blog.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis工具类，封装常用Redis操作
 */
@Component
@Slf4j
public class RedisUtils {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // ============================ 基本操作 ============================//

    /**
     * 设置缓存
     * @param key 键
     * @param value 值
     * @return true成功，false失败
     */
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            log.error("Redis set操作失败，key: {}", key, e);
            return false;
        }
    }

    /**
     * 设置缓存，带有过期时间
     * @param key 键
     * @param value 值
     * @param time 过期时间
     * @param unit 时间单位
     * @return true成功，false失败
     */
    public boolean set(String key, Object value, long time, TimeUnit unit) {
        try {
            // 空值检查
            if (key == null || value == null) {
                log.error("Redis set操作失败，key或value为null，key: {}", key);
                return false;
            }
            
            redisTemplate.opsForValue().set(key, value, time, unit);
            log.debug("Redis set操作成功，key: {}, time: {}, valueType: {}", 
                    key, time, value.getClass().getName());
            return true;
        } catch (Exception e) {
            log.error("Redis set操作失败，key: {}, time: {}, valueType: {}", 
                    key, time, value.getClass().getName(), e);
            return false;
        }
    }

    /**
     * 获取缓存
     * @param key 键
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            log.debug("Redis get操作成功，key: {}, value: {}, valueType: {}", key, value, value != null ? value.getClass().getName() : "null");
            try {
                return (T) value;
            } catch (ClassCastException ce) {
                log.warn("Redis get类型转换失败，key: {}, valueType: {}", key, value != null ? value.getClass().getName() : "null");
                return null;
            }
        } catch (Exception e) {
            log.error("Redis get操作失败，key: {}", key, e);
            return null;
        }
    }

    public Object getObject(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            log.debug("Redis getObject操作成功，key: {}, valueType: {}", key, value != null ? value.getClass().getName() : "null");
            return value;
        } catch (Exception e) {
            log.warn("Redis getObject操作失败，key: {}", key, e);
            return null;
        }
    }

    /**
     * 删除缓存
     * @param key 键
     * @return true成功，false失败
     */
    public boolean delete(String key) {
        try {
            return redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Redis delete操作失败，key: {}", key, e);
            return false;
        }
    }

    /**
     * 批量删除缓存
     * @param keys 键集合
     * @return 删除的数量
     */
    public long delete(Collection<String> keys) {
        try {
            return redisTemplate.delete(keys);
        } catch (Exception e) {
            log.error("Redis delete批量操作失败", e);
            return 0;
        }
    }

    /**
     * 判断缓存是否存在
     * @param key 键
     * @return true存在，false不存在
     */
    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Redis exists操作失败，key: {}", key, e);
            return false;
        }
    }

    // ============================ 高级操作 ============================//

    /**
     * 根据前缀获取所有键
     * @param pattern 前缀，例如：hot:articles:*
     * @return 键集合
     */
    public Set<String> keys(String pattern) {
        try {
            return redisTemplate.keys(pattern);
        } catch (Exception e) {
            log.error("Redis keys操作失败，pattern: {}", pattern, e);
            return null;
        }
    }

    /**
     * 使用 SCAN 命令安全地获取匹配的键（不阻塞 Redis）
     * @param pattern 键模式，例如：hot:articles:*
     * @return 键集合
     */
    public Set<String> scanKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        try {
            keys = redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
                Set<String> result = new HashSet<>();
                ScanOptions options = ScanOptions.scanOptions()
                        .match(pattern)
                        .count(100)
                        .build();
                org.springframework.data.redis.core.Cursor<byte[]> cursor = connection.scan(options);
                while (cursor.hasNext()) {
                    result.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
                cursor.close();
                return result;
            });
            log.debug("Redis scan操作成功，pattern: {}, 找到{}个键", pattern, keys.size());
        } catch (Exception e) {
            log.error("Redis scan操作失败，pattern: {}", pattern, e);
        }
        return keys;
    }

    /**
     * 设置过期时间
     * @param key 键
     * @param time 过期时间
     * @param unit 时间单位
     * @return true成功，false失败
     */
    public boolean expire(String key, long time, TimeUnit unit) {
        try {
            return Boolean.TRUE.equals(redisTemplate.expire(key, time, unit));
        } catch (Exception e) {
            log.error("Redis expire操作失败，key: {}, time: {}", key, time, e);
            return false;
        }
    }

    /**
     * 获取剩余过期时间
     * @param key 键
     * @param unit 时间单位
     * @return 剩余时间，-1表示永久有效，-2表示不存在
     */
    public long getExpire(String key, TimeUnit unit) {
        try {
            return redisTemplate.getExpire(key, unit);
        } catch (Exception e) {
            log.error("Redis getExpire操作失败，key: {}", key, e);
            return -2;
        }
    }

    // ============================ 列表操作 ============================//

    /**
     * 从列表左侧添加元素
     * @param key 键
     * @param value 值
     * @return 添加后的列表长度
     */
    public long lPush(String key, Object value) {
        try {
            return redisTemplate.opsForList().leftPush(key, value);
        } catch (Exception e) {
            log.error("Redis lPush操作失败，key: {}", key, e);
            return 0;
        }
    }

    /**
     * 获取列表所有元素
     * @param key 键
     * @return 列表元素
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> lRange(String key) {
        try {
            return (List<T>) redisTemplate.opsForList().range(key, 0, -1);
        } catch (Exception e) {
            log.error("Redis lRange操作失败，key: {}", key, e);
            return null;
        }
    }

    // ============================ ZSet 操作 ============================//
    // 注意：ZSet 操作使用 StringRedisTemplate，避免 Jackson 序列化 Long 的问题

    /**
     * 向 ZSet 中添加元素
     * @param key 键
     * @param value 值（文章ID，会被转换为字符串）
     * @param score 分数
     * @return true成功，false失败
     */
    public boolean zAdd(String key, Object value, double score) {
        try {
            String strValue = String.valueOf(value);
            Boolean result = stringRedisTemplate.opsForZSet().add(key, strValue, score);
            boolean success = Boolean.TRUE.equals(result);
            log.info("Redis zAdd操作，key: {}, value: {}, score: {}, result: {}", key, strValue, score, result);
            return success;
        } catch (Exception e) {
            log.error("Redis zAdd操作失败，key: {}, value: {}, score: {}", key, value, score, e);
            return false;
        }
    }

    /**
     * 增加 ZSet 中元素的分数
     * @param key 键
     * @param value 值（文章ID，会被转换为字符串）
     * @param delta 增加的分数
     * @return 增加后的分数
     */
    public Double zIncrBy(String key, Object value, double delta) {
        try {
            String strValue = String.valueOf(value);
            Double newScore = stringRedisTemplate.opsForZSet().incrementScore(key, strValue, delta);
            log.info("Redis zIncrBy操作，key: {}, value: {}, delta: {}, newScore: {}", key, strValue, delta, newScore);
            return newScore;
        } catch (Exception e) {
            log.error("Redis zIncrBy操作失败，key: {}, value: {}, delta: {}", key, value, delta, e);
            return null;
        }
    }

    /**
     * 减少 ZSet 中元素的分数
     * @param key 键
     * @param value 值（文章ID）
     * @param delta 减少的分数
     * @return 减少后的分数
     */
    public Double zDecrBy(String key, Object value, double delta) {
        return zIncrBy(key, value, -delta);
    }

    /**
     * 获取 ZSet 中指定范围的元素（按分数降序）
     * @param key 键
     * @param start 起始位置
     * @param end 结束位置
     * @return 元素集合（字符串类型的文章ID）
     */
    public Set<Object> zReverseRange(String key, long start, long end) {
        try {
            Set<String> stringSet = stringRedisTemplate.opsForZSet().reverseRange(key, start, end);
            log.info("Redis zReverseRange操作，key: {}, start: {}, end: {}, size: {}", key, start, end, stringSet != null ? stringSet.size() : 0);
            // 转换为 Set<Object> 以保持兼容性
            if (stringSet == null) {
                return null;
            }
            // 使用 LinkedHashSet 保持 Redis 返回的排序顺序
            return new java.util.LinkedHashSet<Object>(stringSet);
        } catch (Exception e) {
            log.error("Redis zReverseRange操作失败，key: {}, start: {}, end: {}", key, start, end, e);
            return null;
        }
    }

    /**
     * 获取 ZSet 中指定范围元素及其分数（按分数降序）
     * @param key 键
     * @param start 起始位置
     * @param end 结束位置
     * @return 元素及其分数的集合
     */
    public Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object>> zReverseRangeWithScores(String key, long start, long end) {
        try {
            Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
            log.info("Redis zReverseRangeWithScores操作，key: {}, start: {}, end: {}, size: {}", key, start, end, tuples != null ? tuples.size() : 0);
            if (tuples == null) {
                return null;
            }
            // 转换为 TypedTuple<Object> - 使用匿名类
            // 使用 LinkedHashSet 保持 Redis 返回的排序顺序
            Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object>> result = new LinkedHashSet<>();
            for (final org.springframework.data.redis.core.ZSetOperations.TypedTuple<String> tuple : tuples) {
                result.add(new org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object>() {
                    @Override
                    public Object getValue() {
                        return tuple.getValue();
                    }

                    @Override
                    public Double getScore() {
                        return tuple.getScore();
                    }

                    @Override
                    public int compareTo(org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object> o) {
                        if (o == null || o.getScore() == null) {
                            return getScore() == null ? 0 : 1;
                        }
                        if (getScore() == null) {
                            return -1;
                        }
                        return getScore().compareTo(o.getScore());
                    }
                });
            }
            return result;
        } catch (Exception e) {
            log.error("Redis zReverseRangeWithScores操作失败，key: {}, start: {}, end: {}", key, start, end, e);
            return null;
        }
    }

    /**
     * 获取 ZSet 中元素的分数
     * @param key 键
     * @param value 值（会被转换为字符串）
     * @return 分数，不存在时返回 null
     */
    public Double zScore(String key, Object value) {
        try {
            String strValue = String.valueOf(value);
            Double score = stringRedisTemplate.opsForZSet().score(key, strValue);
            log.info("Redis zScore操作，key: {}, value: {}, score: {}", key, strValue, score);
            return score;
        } catch (Exception e) {
            log.error("Redis zScore操作失败，key: {}, value: {}", key, value, e);
            return null;
        }
    }

    /**
     * 移除 ZSet 中的元素
     * @param key 键
     * @param value 值（会被转换为字符串）
     * @return 移除的数量
     */
    public long zRemove(String key, Object value) {
        try {
            String strValue = String.valueOf(value);
            Long count = stringRedisTemplate.opsForZSet().remove(key, strValue);
            log.info("Redis zRemove操作，key: {}, value: {}, count: {}", key, strValue, count);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("Redis zRemove操作失败，key: {}, value: {}", key, value, e);
            return 0;
        }
    }

    /**
     * 获取 ZSet 的元素个数
     * @param key 键
     * @return 元素个数
     */
    public long zSize(String key) {
        try {
            Long size = stringRedisTemplate.opsForZSet().size(key);
            log.info("Redis zSize操作，key: {}, size: {}", key, size);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("Redis zSize操作失败，key: {}", key, e);
            return 0;
        }
    }

    /**
     * 删除 ZSet
     * @param key 键
     * @return true成功，false失败
     */
    public boolean zDelete(String key) {
        return delete(key);
    }
}
