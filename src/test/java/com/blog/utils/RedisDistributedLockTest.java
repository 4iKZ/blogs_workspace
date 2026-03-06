package com.blog.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Redis 分布式锁测试")
class RedisDistributedLockTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RedisDistributedLock redisDistributedLock;

    @BeforeEach
    void setUp() {
        redisDistributedLock = new RedisDistributedLock();
        ReflectionTestUtils.setField(redisDistributedLock, "redisTemplate", redisTemplate);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
        redisDistributedLock.destroy();
    }

    @Test
    @DisplayName("默认 tryLock 不启动 watchdog")
    void testTryLock_DefaultDoesNotStartWatchdog() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("lock:article:like:1:2"), any(String.class), eq(10L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);

        String lockValue = redisDistributedLock.tryLock("article:like:1:2");

        assertNotNull(lockValue);
        assertTrue(getWatchdogFutures().isEmpty());
    }

    @Test
    @DisplayName("显式 watchdog 按锁实例绑定，释放一个实例不会取消另一个实例")
    void testTryLockWithWatchdog_InstanceBoundCancellation() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("lock:upload:complete:test"), any(String.class), eq(30L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(redisTemplate.execute(org.mockito.ArgumentMatchers.<DefaultRedisScript<Long>>any(), anyList(), any()))
                .thenReturn(1L);

        String firstLockValue = redisDistributedLock.tryLockWithWatchdog(
                "upload:complete:test", 30, TimeUnit.SECONDS, 0, TimeUnit.SECONDS);

        assertNotNull(firstLockValue);
        assertEquals(1, getWatchdogFutures().size());

        ReflectionTestUtils.invokeMethod(
                redisDistributedLock,
                "startWatchdog",
                "lock:upload:complete:test",
                "manual-lock-value",
                30L,
                TimeUnit.SECONDS
        );

        Map<String, ?> watchdogs = getWatchdogFutures();
        assertEquals(2, watchdogs.size());
        assertTrue(watchdogs.containsKey("lock:upload:complete:test::" + firstLockValue));
        assertTrue(watchdogs.containsKey("lock:upload:complete:test::manual-lock-value"));

        assertTrue(redisDistributedLock.unlock("upload:complete:test", firstLockValue));

        watchdogs = getWatchdogFutures();
        assertEquals(1, watchdogs.size());
        assertFalse(watchdogs.containsKey("lock:upload:complete:test::" + firstLockValue));
        assertTrue(watchdogs.containsKey("lock:upload:complete:test::manual-lock-value"));
    }

    @Test
    @DisplayName("事务内 releaseLock 延迟到 afterCompletion 才真正解锁")
    void testReleaseLock_DefersUnlockUntilTransactionCompletion() {
        redisDistributedLock = spy(redisDistributedLock);
        doReturn(true).when(redisDistributedLock).unlock("comment:like:1:2", "token-1");
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        redisDistributedLock.releaseLock("comment:like:1:2", "token-1");

        verify(redisDistributedLock, never()).unlock("comment:like:1:2", "token-1");

        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, synchronizations.size());

        synchronizations.forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));

        verify(redisDistributedLock, times(1)).unlock("comment:like:1:2", "token-1");
    }

    @Test
    @DisplayName("非事务场景 releaseLock 立即解锁")
    void testReleaseLock_UnlocksImmediatelyWithoutTransaction() {
        redisDistributedLock = spy(redisDistributedLock);
        doReturn(true).when(redisDistributedLock).unlock("follow:1:2", "token-2");

        redisDistributedLock.releaseLock("follow:1:2", "token-2");

        verify(redisDistributedLock, times(1)).unlock("follow:1:2", "token-2");
    }

    @Test
    @DisplayName("事务回滚后也会释放锁")
    void testReleaseLock_UnlocksAfterRollbackCompletion() {
        redisDistributedLock = spy(redisDistributedLock);
        doReturn(true).when(redisDistributedLock).unlock("article:favorite:1:2", "token-3");
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        redisDistributedLock.releaseLock("article:favorite:1:2", "token-3");

        verify(redisDistributedLock, never()).unlock("article:favorite:1:2", "token-3");

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(redisDistributedLock, times(1)).unlock("article:favorite:1:2", "token-3");
    }

    @Test
    @DisplayName("事务完成后解锁异常不会向外抛出")
    void testReleaseLock_SwallowsUnlockExceptionAfterCompletion() {
        redisDistributedLock = spy(redisDistributedLock);
        doThrow(new RuntimeException("unlock failed"))
                .when(redisDistributedLock).unlock("comment:delete:99", "token-4");
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        redisDistributedLock.releaseLock("comment:delete:99", "token-4");

        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, synchronizations.size());

        assertDoesNotThrow(() -> synchronizations.forEach(
                sync -> sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED)));
        verify(redisDistributedLock, times(1)).unlock("comment:delete:99", "token-4");
    }

    @SuppressWarnings("unchecked")
    private Map<String, ?> getWatchdogFutures() {
        return (Map<String, ?>) ReflectionTestUtils.getField(redisDistributedLock, "watchdogFutures");
    }
}
