package com.blog.utils;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RedisCacheUtilsTest {

    @Test
    void setAndGetCache_shouldRoundTrip() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("key")).thenReturn("value");

        RedisCacheUtils utils = new RedisCacheUtils();
        setField(utils, "redisTemplate", redisTemplate);

        utils.setCache("key", "value", 60, TimeUnit.SECONDS);
        Object result = utils.getCache("key");

        verify(valueOps).set("key", "value", 60, TimeUnit.SECONDS);
        assertThat(result).isEqualTo("value");
    }

    @Test
    void deleteCache_shouldCallRedisDelete() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        RedisCacheUtils utils = new RedisCacheUtils();
        setField(utils, "redisTemplate", redisTemplate);

        utils.deleteCache("key");

        verify(redisTemplate).delete("key");
    }

    @Test
    void incrementCache_shouldReturnNewValue() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment("counter", 1)).thenReturn(11L);

        RedisCacheUtils utils = new RedisCacheUtils();
        setField(utils, "redisTemplate", redisTemplate);

        Long result = utils.incrementCache("counter", 1);

        assertThat(result).isEqualTo(11L);
        verify(valueOps).increment("counter", 1);
    }

    @Test
    void decrementCache_shouldReturnNewValue() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.decrement("counter", 1)).thenReturn(9L);

        RedisCacheUtils utils = new RedisCacheUtils();
        setField(utils, "redisTemplate", redisTemplate);

        Long result = utils.decrementCache("counter", 1);

        assertThat(result).isEqualTo(9L);
        verify(valueOps).decrement("counter", 1);
    }

    @Test
    void hasCache_shouldReturnTrueWhenExists() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        when(redisTemplate.hasKey("key")).thenReturn(true);

        RedisCacheUtils utils = new RedisCacheUtils();
        setField(utils, "redisTemplate", redisTemplate);

        assertThat(utils.hasCache("key")).isTrue();
    }

    @Test
    void expireCache_shouldSetExpiration() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);

        RedisCacheUtils utils = new RedisCacheUtils();
        setField(utils, "redisTemplate", redisTemplate);

        utils.expireCache("key", 30, TimeUnit.MINUTES);

        verify(redisTemplate).expire("key", 30, TimeUnit.MINUTES);
    }

    @Test
    void getCacheExpire_shouldReturnTtl() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        when(redisTemplate.getExpire("key", TimeUnit.SECONDS)).thenReturn(42L);

        RedisCacheUtils utils = new RedisCacheUtils();
        setField(utils, "redisTemplate", redisTemplate);

        Long ttl = utils.getCacheExpire("key");

        assertThat(ttl).isEqualTo(42L);
    }

    private static void setField(RedisCacheUtils target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = RedisCacheUtils.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
