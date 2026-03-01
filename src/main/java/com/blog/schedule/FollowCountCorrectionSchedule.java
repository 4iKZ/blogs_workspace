package com.blog.schedule;

import com.blog.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 关注计数器校正定时任务
 * 负责校正用户的粉丝数和关注数，确保数据一致性
 */
@Component
@Slf4j
public class FollowCountCorrectionSchedule {

    @Autowired
    private UserMapper userMapper;

    /**
     * 每天凌晨 2:00 校正所有用户的粉丝数和关注数
     * 通过重新计算 user_follows 表中的实际关注关系来更新
     * Cron: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void correctFollowCounts() {
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
}
