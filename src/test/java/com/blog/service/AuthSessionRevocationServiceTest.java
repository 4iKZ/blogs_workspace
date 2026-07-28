package com.blog.service;

import com.blog.mapper.UserMapper;
import com.blog.utils.RedisUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Set;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthSessionRevocationServiceTest {

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void revokeAfterCommit_shouldNotTouchRedisBeforeCommitOrAfterRollback() {
        RedisUtils redisUtils = mock(RedisUtils.class);
        AuthSessionRevocationService service = service(redisUtils);
        beginTransaction();

        service.revokeAfterCommit(7L);

        verify(redisUtils, never()).stringSetMembers("auth:refresh:user-families:7");
        synchronization().afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        verify(redisUtils, never()).stringSetMembers("auth:refresh:user-families:7");
    }

    @Test
    void revokeAfterCommit_shouldRunOnlyAfterCommitAndSwallowRedisFailureAfterBoundedRetries() {
        RedisUtils redisUtils = mock(RedisUtils.class);
        when(redisUtils.stringSetMembers("auth:refresh:user-families:7"))
                .thenThrow(new IllegalStateException("redis unavailable"));
        AuthSessionRevocationService service = service(redisUtils);
        beginTransaction();
        service.revokeAfterCommit(7L);

        assertThatCode(() -> synchronization().afterCommit()).doesNotThrowAnyException();

        verify(redisUtils, times(3)).stringSetMembers("auth:refresh:user-families:7");
    }

    @Test
    void revokeUserSessions_shouldRemoveNewFamilyIndexesAndLegacyUserKey() {
        RedisUtils redisUtils = mock(RedisUtils.class);
        when(redisUtils.stringSetMembers("auth:refresh:user-families:7")).thenReturn(Set.of("family-1"));
        when(redisUtils.stringSetMembers("auth:refresh:family-jtis:family-1")).thenReturn(Set.of("jti-1"));
        when(redisUtils.stringSetMembers("auth:refresh:user-jtis:7")).thenReturn(Set.of("jti-2"));

        service(redisUtils).revokeUserSessions(7L);

        verify(redisUtils).deleteString("auth:refresh:jti:jti-1");
        verify(redisUtils).deleteString("auth:refresh:jti:jti-2");
        verify(redisUtils).deleteString("auth:refresh:family-active:family-1");
        verify(redisUtils).deleteString("auth:refresh:user:7");
        verify(redisUtils).delete("auth:refresh:user:7");
    }

    @Test
    void synchronousFamilyRevocation_shouldFailClosedAfterRedisRetriesAreExhausted() {
        RedisUtils redisUtils = mock(RedisUtils.class);
        when(redisUtils.stringSetMembers("auth:refresh:family-jtis:family-1"))
                .thenThrow(new IllegalStateException("redis unavailable"));

        assertThatThrownBy(() -> service(redisUtils).revokeFamily(7L, "family-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("认证会话撤销失败");

        verify(redisUtils, times(3)).stringSetMembers("auth:refresh:family-jtis:family-1");
    }

    private static AuthSessionRevocationService service(RedisUtils redisUtils) {
        Executor directExecutor = Runnable::run;
        return new AuthSessionRevocationService(mock(UserMapper.class), redisUtils, directExecutor);
    }

    private static void beginTransaction() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }

    private static TransactionSynchronization synchronization() {
        return TransactionSynchronizationManager.getSynchronizations().getFirst();
    }
}
