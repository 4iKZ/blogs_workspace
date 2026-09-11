package com.blog.service;

import com.blog.entity.UserFollow;
import com.blog.mapper.UserFollowMapper;
import com.blog.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowCountServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserFollowMapper userFollowMapper;

    @InjectMocks
    private FollowCountService followCountService;

    private void runAfterCommit() {
        for (TransactionSynchronization ts : TransactionSynchronizationManager.getSynchronizations()) {
            ts.afterCommit();
        }
    }

    @Test
    @DisplayName("关注提交后更新双方计数")
    void applyFollowCommitted_updatesCounters() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            followCountService.applyFollowCommitted(1L, 2L);
            runAfterCommit();

            var ordered = inOrder(userMapper);
            ordered.verify(userMapper).incrementFollowerCount(2L);
            ordered.verify(userMapper).incrementFollowingCount(1L);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("关注计数失败后重试成功")
    void applyFollowCommitted_retriesOnFailure() {
        when(userMapper.incrementFollowerCount(2L))
                .thenThrow(new RuntimeException("db down"))
                .thenReturn(1);
        when(userMapper.incrementFollowingCount(1L)).thenReturn(1);

        TransactionSynchronizationManager.initSynchronization();
        try {
            followCountService.applyFollowCommitted(1L, 2L);
            runAfterCommit();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(userMapper, times(2)).incrementFollowerCount(2L);
        verify(userMapper).incrementFollowingCount(1L);
    }

    @Test
    @DisplayName("关注计数一直失败重试到上限")
    void applyFollowCommitted_retriesExhausted() {
        when(userMapper.incrementFollowerCount(2L)).thenThrow(new RuntimeException("db down"));
        when(userMapper.incrementFollowingCount(1L)).thenReturn(1);

        TransactionSynchronizationManager.initSynchronization();
        try {
            followCountService.applyFollowCommitted(1L, 2L);
            runAfterCommit();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(userMapper, times(5)).incrementFollowerCount(2L);
        verify(userMapper).incrementFollowingCount(1L);
    }

    @Test
    @DisplayName("取关提交后递减双方计数（失败重试成功）")
    void applyUnfollowCommitted_decrementsWithRetry() {
        when(userMapper.decrementFollowerCount(2L))
                .thenThrow(new RuntimeException("db down"))
                .thenReturn(1);
        when(userMapper.decrementFollowingCount(1L)).thenReturn(1);

        TransactionSynchronizationManager.initSynchronization();
        try {
            followCountService.applyUnfollowCommitted(1L, 2L);
            runAfterCommit();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(userMapper, times(2)).decrementFollowerCount(2L);
        verify(userMapper).decrementFollowingCount(1L);
    }

    @Test
    @DisplayName("删除用户前修正双向关注计数")
    void detachUserRelations_decrementsBothDirections() {
        UserFollow follower = new UserFollow();
        follower.setFollowerId(2L);
        UserFollow following = new UserFollow();
        following.setFollowingId(3L);
        when(userFollowMapper.selectList(any()))
                .thenReturn(List.of(follower))
                .thenReturn(List.of(following));

        followCountService.detachUserRelations(1L);

        verify(userMapper).decrementFollowingCount(2L);
        verify(userMapper).decrementFollowerCount(3L);
    }

    @Test
    @DisplayName("无关注关系时不更新计数")
    void detachUserRelations_emptyRelations_noUpdate() {
        when(userFollowMapper.selectList(any())).thenReturn(List.of());

        followCountService.detachUserRelations(1L);

        verify(userMapper, never()).decrementFollowingCount(anyLong());
        verify(userMapper, never()).decrementFollowerCount(anyLong());
    }

    @Test
    @DisplayName("全量校正调用两个校正SQL")
    void correctAll_invokesBothCorrections() {
        when(userMapper.correctFollowerCounts()).thenReturn(10);
        when(userMapper.correctFollowingCounts()).thenReturn(8);

        followCountService.correctAll();

        verify(userMapper).correctFollowerCounts();
        verify(userMapper).correctFollowingCounts();
    }

    @Test
    @DisplayName("全量校正失败不抛出")
    void correctAll_failureSwallowed() {
        when(userMapper.correctFollowerCounts()).thenThrow(new RuntimeException("db error"));

        assertThatCode(() -> followCountService.correctAll()).doesNotThrowAnyException();

        verify(userMapper).correctFollowerCounts();
        verify(userMapper, never()).correctFollowingCounts();
    }
}
