package com.blog.schedule;

import com.blog.service.ArticleRankService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 排行榜定时任务
 * 负责重置日榜和周榜的 Redis ZSet
 */
@Component
@Slf4j
public class RankResetSchedule {

    @Autowired
    private ArticleRankService articleRankService;

    /**
     * 每天凌晨 00:00 重置日榜
     * Cron: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void resetDayRank() {
        log.info("开始执行日榜重置任务");
        try {
            articleRankService.resetRank("day");
            log.info("日榜重置任务执行成功");
        } catch (Exception e) {
            log.error("日榜重置任务执行失败", e);
        }
    }

    /**
     * 每周一凌晨 00:00 重置周榜
     * Cron: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 0 ? * MON")
    public void resetWeekRank() {
        log.info("开始执行周榜重置任务");
        try {
            articleRankService.resetRank("week");
            log.info("周榜重置任务执行成功");
        } catch (Exception e) {
            log.error("周榜重置任务执行失败", e);
        }
    }
}
