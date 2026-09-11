package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.entity.UserFollow;
import com.blog.mapper.UserFollowMapper;
import com.blog.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * 关注计数服务：users.follower_count / following_count 的唯一变更入口。
 * <p>
 * 关注/取关在事务提交后以同步重试方式更新计数（最多 5 次，耗尽仅记日志，
 * 由 {@link #correctAll()} 的每日校正兜底）；删除用户时在调用方事务内修正计数。
 */
@Service
@Slf4j
public class FollowCountService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserFollowMapper userFollowMapper;

    /**
     * 关注关系提交后更新双方计数（follower 的 following_count、following 的 follower_count）。
     */
    public void applyFollowCommitted(Long followerId, Long followingId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                retryExecute(() -> userMapper.incrementFollowerCount(followingId),
                        "更新粉丝数失败");
                retryExecute(() -> userMapper.incrementFollowingCount(followerId),
                        "更新关注数失败");
                log.debug("事务提交后更新关注计数：followerId={}, followingId={}", followerId, followingId);
            }
        });
    }

    /**
     * 取关关系提交后更新双方计数。
     */
    public void applyUnfollowCommitted(Long followerId, Long followingId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                retryExecute(() -> userMapper.decrementFollowerCount(followingId),
                        "更新粉丝数失败");
                retryExecute(() -> userMapper.decrementFollowingCount(followerId),
                        "更新关注数失败");
                log.debug("事务提交后更新取消关注计数：followerId={}, followingId={}", followerId, followingId);
            }
        });
    }

    /**
     * 删除用户前修正受影响的关注计数：关注该用户的人 following_count-1，
     * 该用户关注的人 follower_count-1。在调用方事务内执行，失败由调用方回滚。
     */
    @Transactional
    public void detachUserRelations(Long userId) {
        // 1. 找到所有关注该用户的人，减少他们的 following_count
        List<UserFollow> followers = userFollowMapper.selectList(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowingId, userId));
        if (!followers.isEmpty()) {
            for (UserFollow follow : followers) {
                userMapper.decrementFollowingCount(follow.getFollowerId());
            }
            log.info("更新关注者计数：{} 用户的 following_count 已减少", followers.size());
        }

        // 2. 找到该用户关注的所有人，减少他们的 follower_count
        List<UserFollow> following = userFollowMapper.selectList(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, userId));
        if (!following.isEmpty()) {
            for (UserFollow follow : following) {
                userMapper.decrementFollowerCount(follow.getFollowingId());
            }
            log.info("更新被关注者计数：{} 用户的 follower_count 已减少", following.size());
        }
    }

    /**
     * 全量校正所有用户的关注计数（每日定时任务调用）。
     */
    public void correctAll() {
        log.info("开始执行关注计数器校正任务");
        long startTime = System.currentTimeMillis();

        try {
            int followerCount = userMapper.correctFollowerCounts();
            log.info("粉丝数校正完成，影响行数: {}", followerCount);

            int followingCount = userMapper.correctFollowingCounts();
            log.info("关注数校正完成，影响行数: {}", followingCount);

            long duration = System.currentTimeMillis() - startTime;
            log.info("关注计数器校正任务执行成功，耗时: {}ms", duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("关注计数器校正任务执行失败，耗时: {}ms", duration, e);
        }
    }

    /**
     * 重试执行任务：事务提交后计数更新失败时最多重试 5 次，耗尽仅记录日志。
     */
    private void retryExecute(Runnable task, String errorMsg) {
        int maxRetries = 5;
        for (int i = 0; i < maxRetries; i++) {
            try {
                task.run();
                return;
            } catch (Exception e) {
                if (i == maxRetries - 1) {
                    log.error(errorMsg + "，已达最大重试次数", e);
                } else {
                    try {
                        Thread.sleep(100 * (i + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    log.warn(errorMsg + "，重试 {}/{}", i + 1, maxRetries);
                }
            }
        }
    }
}
