package com.blog.service.impl;

import com.blog.common.Result;
import com.blog.utils.RedisDistributedLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentServiceImpl 锁行为测试")
class CommentServiceImplLockingTest {

    @Mock
    private RedisDistributedLock redisDistributedLock;

    @InjectMocks
    private CommentServiceImpl commentService;

    @Test
    @DisplayName("deleteComment 使用显式 watchdog 获取删除锁")
    void testDeleteComment_UsesWatchdogLock() {
        when(redisDistributedLock.tryLockWithWatchdog("comment:delete:123", 10L, TimeUnit.SECONDS, 3L, TimeUnit.SECONDS))
                .thenReturn(null);

        Result<Void> result = commentService.deleteComment(123L);

        assertFalse(result.isSuccess());
        assertEquals("操作过于频繁，请稍后重试", result.getMessage());
        verify(redisDistributedLock).tryLockWithWatchdog("comment:delete:123", 10L, TimeUnit.SECONDS, 3L, TimeUnit.SECONDS);
        verify(redisDistributedLock, never()).tryLock("comment:delete:123", 10L, TimeUnit.SECONDS);
    }
}
