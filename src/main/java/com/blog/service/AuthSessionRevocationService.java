package com.blog.service;

import com.blog.mapper.UserMapper;
import com.blog.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AuthSessionRevocationService {

    private static final String USER_JTIS_PREFIX = "auth:refresh:user-jtis:";
    private static final String USER_FAMILIES_PREFIX = "auth:refresh:user-families:";
    private static final String FAMILY_JTIS_PREFIX = "auth:refresh:family-jtis:";
    private static final String FAMILY_ACTIVE_PREFIX = "auth:refresh:family-active:";
    private static final String FAMILY_REVOKED_PREFIX = "auth:refresh:family-revoked:";
    private static final long REVOKED_TTL_SECONDS = TimeUnit.DAYS.toSeconds(30);

    private final UserMapper userMapper;
    private final RedisUtils redisUtils;
    private final Executor retryExecutor;

    public AuthSessionRevocationService(
            UserMapper userMapper,
            RedisUtils redisUtils,
            @Qualifier("notificationTaskExecutor") Executor retryExecutor) {
        this.userMapper = userMapper;
        this.redisUtils = redisUtils;
        this.retryExecutor = retryExecutor;
    }

    public boolean updateStatusAndRevoke(Long userId, Integer status) {
        int updated = userMapper.updateStatusAndIncrementTokenVersion(userId, status);
        if (updated > 0) {
            revokeAfterCommit(userId);
        }
        return updated > 0;
    }

    public boolean incrementVersionAndRevoke(Long userId) {
        int updated = userMapper.incrementTokenVersion(userId);
        if (updated > 0) {
            revokeAfterCommit(userId);
        }
        return updated > 0;
    }

    public void revokeAfterCommit(Long userId) {
        afterCommitWithRetry("revoke-user-" + userId, () -> revokeUserNow(userId));
    }

    public void afterCommitWithRetry(String taskName, Runnable action) {
        Runnable submit = () -> {
            try {
                retryExecutor.execute(() -> {
                    try {
                        runWithRetriesOrThrow(taskName, action);
                    } catch (IllegalStateException e) {
                        log.error("异步认证清理重试耗尽：task={}", taskName, e);
                    }
                });
            } catch (RuntimeException e) {
                log.error("提交认证清理重试任务失败：task={}", taskName, e);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    submit.run();
                }
            });
        } else {
            submit.run();
        }
    }

    public void revokeFamily(Long userId, String familyId) {
        runWithRetriesOrThrow(
                "revoke-family-" + familyId,
                () -> revokeFamilyNow(userId, familyId));
    }

    public void revokeUserSessions(Long userId) {
        runWithRetriesOrThrow("revoke-user-" + userId, () -> revokeUserNow(userId));
    }

    private void runWithRetriesOrThrow(String taskName, Runnable action) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                action.run();
                return;
            } catch (RuntimeException e) {
                lastFailure = e;
                log.error("认证清理失败：task={}, attempt={}", taskName, attempt, e);
            }
        }
        throw new IllegalStateException("认证会话撤销失败：" + taskName, lastFailure);
    }

    private void revokeUserNow(Long userId) {
        for (String familyId : redisUtils.stringSetMembers(USER_FAMILIES_PREFIX + userId)) {
            revokeFamilyNow(userId, familyId);
        }
        for (String jti : redisUtils.stringSetMembers(USER_JTIS_PREFIX + userId)) {
            redisUtils.deleteString("auth:refresh:jti:" + jti);
        }
        redisUtils.deleteString(USER_JTIS_PREFIX + userId);
        redisUtils.deleteString(USER_FAMILIES_PREFIX + userId);
        redisUtils.deleteString("auth:refresh:user:" + userId);
        redisUtils.delete("auth:refresh:user:" + userId);
    }

    private void revokeFamilyNow(Long userId, String familyId) {
        redisUtils.setString(
                FAMILY_REVOKED_PREFIX + familyId, Long.toString(userId),
                REVOKED_TTL_SECONDS, TimeUnit.SECONDS);
        Set<String> jtis = redisUtils.stringSetMembers(FAMILY_JTIS_PREFIX + familyId);
        for (String jti : jtis) {
            redisUtils.deleteString("auth:refresh:jti:" + jti);
            redisUtils.removeFromSet(USER_JTIS_PREFIX + userId, jti);
        }
        redisUtils.deleteString(FAMILY_JTIS_PREFIX + familyId);
        redisUtils.deleteString(FAMILY_ACTIVE_PREFIX + familyId);
        redisUtils.removeFromSet(USER_FAMILIES_PREFIX + userId, familyId);
    }
}
