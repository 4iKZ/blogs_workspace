package com.blog.schedule;

import com.blog.service.CommentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 评论审核兜底任务：AI 审核失败或事件丢失时评论会长期停留在待审核（status=1），
 * 定时重新投递审核事件，避免用户内容永久不可见。
 */
@Slf4j
@Component
public class CommentModerationRetryScheduler {

    @Autowired
    private CommentService commentService;

    // ponytail: 固定节奏重投（5 分钟最多 50 条），需要精确退避时再引入持久化重试表
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void requeueStalePendingComments() {
        try {
            commentService.requeueStalePendingModeration();
        } catch (Exception e) {
            log.error("评论审核兜底重投任务异常", e);
        }
    }
}