package com.blog.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.entity.Article;
import com.blog.entity.Comment;
import com.blog.entity.User;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.UserMapper;
import com.blog.mapper.VisitStatisticsMapper;
import com.blog.mapper.WebsiteAccessLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 访问统计聚合定时任务
 * 每日 00:10 将昨日的 website_access_log 原始数据聚合写入 visit_statistics
 */
@Slf4j
@Component
public class VisitStatisticsScheduler {

    @Autowired
    private WebsiteAccessLogMapper websiteAccessLogMapper;

    @Autowired
    private VisitStatisticsMapper visitStatisticsMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private CommentMapper commentMapper;

    /**
     * 每日 00:10 执行，聚合昨日数据
     * Cron：秒 分 时 日 月 周（错开 00:00 执行的排行榜重置任务）
     */
    @Scheduled(cron = "0 10 0 * * ?")
    public void aggregateYesterdayStatistics() {
        String yesterday = LocalDate.now().minusDays(1).toString();
        log.info("开始聚合 {} 的访问统计数据", yesterday);
        try {
            doAggregate(yesterday);
            log.info("访问统计聚合完成，日期：{}", yesterday);
        } catch (Exception e) {
            log.error("访问统计聚合失败，日期：{}，原因：{}", yesterday, e.getMessage(), e);
        }
    }

    /**
     * 对指定日期执行聚合并写入 visit_statistics
     */
    public void doAggregate(String dateStr) {
        // 1. 从 website_access_log 读取当日 PV / UV
        Integer pv = websiteAccessLogMapper.countPvByDate(dateStr);
        Integer uv = websiteAccessLogMapper.countUvByDate(dateStr);

        // 2. 从各业务表统计当日新增量
        LocalDate date = LocalDate.parse(dateStr);
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

        Integer newUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .ge(User::getCreateTime, dayStart)
                        .lt(User::getCreateTime, dayEnd)
        ).intValue();

        Integer newArticles = articleMapper.selectCount(
                new LambdaQueryWrapper<Article>()
                        .ge(Article::getCreateTime, dayStart)
                        .lt(Article::getCreateTime, dayEnd)
        ).intValue();

        Integer newComments = commentMapper.selectCount(
                new LambdaQueryWrapper<Comment>()
                        .ge(Comment::getCreateTime, dayStart)
                        .lt(Comment::getCreateTime, dayEnd)
                        .eq(Comment::getDeleted, 0)
        ).intValue();

        // 3. upsert 到 visit_statistics（uk_date 唯一索引保证幂等）
        visitStatisticsMapper.upsertStatistics(
                dateStr,
                pv != null ? pv : 0,
                uv != null ? uv : 0,
                pv != null ? pv : 0,   // total_visits 与 page_views 保持一致（同为 PV）
                newUsers,
                newArticles,
                newComments
        );

        log.info("日期 {} 聚合结果 → PV={}, UV={}, 新用户={}, 新文章={}, 新评论={}",
                dateStr, pv, uv, newUsers, newArticles, newComments);
    }
}
