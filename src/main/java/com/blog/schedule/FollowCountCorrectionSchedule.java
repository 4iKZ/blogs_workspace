package com.blog.schedule;

import com.blog.service.FollowCountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 关注计数器校正定时任务
 * 负责校正用户的粉丝数和关注数，确保数据一致性
 */
@Component
public class FollowCountCorrectionSchedule {

    @Autowired
    private FollowCountService followCountService;

    /**
     * 每天凌晨 2:00 校正所有用户的粉丝数和关注数
     * 通过重新计算 user_follows 表中的实际关注关系来更新
     * Cron: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void correctFollowCounts() {
        followCountService.correctAll();
    }
}
