package com.blog.security;

import com.blog.utils.RedisUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetRedisClaimTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void claimAtomicallyMovesCodeIntoOwnedClaimAndKeepsAttemptLockContract() {
        RedisUtils redis = new RedisUtils();
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        setField(redis, "stringRedisTemplate", template);
        when(template.execute(any(RedisScript.class), eq(List.of("code", "attempts", "lock", "claim")),
                eq("digest"), eq("claim-id"), eq("120"))).thenReturn(1L);

        assertThat(redis.claimPasswordResetCode(
                "code", "attempts", "lock", "claim", "digest", "claim-id", 120)).isEqualTo(1);

        ArgumentCaptor<RedisScript> script = ArgumentCaptor.forClass(RedisScript.class);
        verify(template).execute(script.capture(), eq(List.of("code", "attempts", "lock", "claim")),
                eq("digest"), eq("claim-id"), eq("120"));
        String lua = script.getValue().getScriptAsString();
        assertThat(lua)
                .contains("EXISTS', KEYS[3]")
                .contains("EXISTS', KEYS[4]")
                .contains("SET', KEYS[4]")
                .contains("DEL', KEYS[1]")
                .contains("attempts >= 5");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void finalizeAndReleaseRequireTheClaimOwnerAndReleaseRestoresDigest() {
        RedisUtils redis = new RedisUtils();
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        setField(redis, "stringRedisTemplate", template);
        when(template.execute(any(RedisScript.class), eq(List.of("claim")), eq("claim-id")))
                .thenReturn(1L);
        when(template.execute(any(RedisScript.class), eq(List.of("code", "claim")),
                eq("claim-id"), eq("600"))).thenReturn(1L);

        assertThat(redis.finalizePasswordResetClaim("claim", "claim-id")).isTrue();
        assertThat(redis.releasePasswordResetClaim("code", "claim", "claim-id", 600)).isTrue();

        ArgumentCaptor<RedisScript> finalizeScript = ArgumentCaptor.forClass(RedisScript.class);
        verify(template).execute(finalizeScript.capture(), eq(List.of("claim")), eq("claim-id"));
        assertThat(finalizeScript.getValue().getScriptAsString())
                .contains("ARGV[1] .. ':'")
                .contains("DEL', KEYS[1]");

        ArgumentCaptor<RedisScript> releaseScript = ArgumentCaptor.forClass(RedisScript.class);
        verify(template).execute(releaseScript.capture(), eq(List.of("code", "claim")),
                eq("claim-id"), eq("600"));
        assertThat(releaseScript.getValue().getScriptAsString())
                .contains("string.sub(claim")
                .contains("EXISTS', KEYS[1]")
                .contains("SET', KEYS[1]")
                .contains("'EX', ARGV[2]")
                .contains("DEL', KEYS[2]");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void refreshFamilyRotationUsesOneAtomicCompareAndSwapScript() {
        RedisUtils redis = new RedisUtils();
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        setField(redis, "stringRedisTemplate", template);
        List<String> keys = List.of(
                "active", "revoked", "old-jti-key", "new-jti-key",
                "family-jtis", "user-jtis", "user-families");
        when(template.execute(any(RedisScript.class), eq(keys),
                eq("7:0:old-jti"), eq("7:1:new-jti"), eq("7:4"),
                eq("new-jti"), eq("family-1"), eq("600"))).thenReturn(1L);

        assertThat(redis.rotateRefreshTokenFamily(
                keys.get(0), keys.get(1), keys.get(2), keys.get(3),
                keys.get(4), keys.get(5), keys.get(6),
                "7:0:old-jti", "7:1:new-jti", "7:4",
                "new-jti", "family-1", 600)).isEqualTo(1);

        ArgumentCaptor<RedisScript> script = ArgumentCaptor.forClass(RedisScript.class);
        verify(template).execute(script.capture(), eq(keys),
                eq("7:0:old-jti"), eq("7:1:new-jti"), eq("7:4"),
                eq("new-jti"), eq("family-1"), eq("600"));
        assertThat(script.getValue().getScriptAsString())
                .contains("EXISTS', KEYS[2]")
                .contains("GET', KEYS[1]")
                .contains("GET', KEYS[3]")
                .contains("DEL', KEYS[3]")
                .contains("SET', KEYS[4]")
                .contains("SADD', KEYS[5]")
                .contains("SADD', KEYS[6]")
                .contains("SADD', KEYS[7]");
    }

    private static void setField(Object target, String name, Object value) {
        try {
            var field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
