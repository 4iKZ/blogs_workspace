package com.blog.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CacheInvalidationQueueManager {

    private static final String QUEUE_KEY = "cache:invalidation:queue";
    private static final long QUEUE_EXPIRE_DAYS = 1;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void addToQueue(CacheInvalidationEventDTO eventDTO) {
        try {
            redisTemplate.opsForZSet().add(QUEUE_KEY, eventDTO, eventDTO.getExecuteTime());
            log.debug("缓存失效事件已添加到Redis队列: {}", eventDTO);
        } catch (Exception e) {
            log.error("添加缓存失效事件到Redis队列失败: {}", eventDTO, e);
            throw e;
        }
    }

    public Set<Object> getReadyEvents(long currentTime) {
        try {
            return redisTemplate.opsForZSet().rangeByScore(QUEUE_KEY, 0, currentTime);
        } catch (Exception e) {
            log.error("获取待执行缓存失效事件失败", e);
            return null;
        }
    }

    public boolean removeFromQueue(Object event) {
        try {
            Long removed = redisTemplate.opsForZSet().remove(QUEUE_KEY, event);
            return removed != null && removed > 0;
        } catch (Exception e) {
            log.error("从Redis队列移除缓存失效事件失败: {}", event, e);
            return false;
        }
    }

    public Long getQueueSize() {
        try {
            return redisTemplate.opsForZSet().size(QUEUE_KEY);
        } catch (Exception e) {
            log.error("获取队列大小失败", e);
            return 0L;
        }
    }

    public void cleanExpiredEvents() {
        try {
            long now = System.currentTimeMillis();
            Set<Object> allEvents = redisTemplate.opsForZSet().range(QUEUE_KEY, 0, -1);
            if (allEvents != null) {
                for (Object event : allEvents) {
                    if (event instanceof CacheInvalidationEventDTO) {
                        CacheInvalidationEventDTO dto = (CacheInvalidationEventDTO) event;
                        if (dto.getExecuteTime() < now - TimeUnit.DAYS.toMillis(QUEUE_EXPIRE_DAYS)) {
                            redisTemplate.opsForZSet().remove(QUEUE_KEY, event);
                            log.debug("清理过期缓存失效事件: {}", dto);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("清理过期缓存失效事件失败", e);
        }
    }
}
