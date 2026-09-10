package com.blog.event;

import com.blog.dto.ModerationResult;
import com.blog.entity.Notification;
import com.blog.service.CommentService;
import com.blog.service.ContentModerationService;
import com.blog.service.NotificationService;
import com.blog.utils.RedisDistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.TimeUnit;

/**
 * 评论审核事件监听器
 * 异步执行AI评论内容审核，根据结果更新评论状态并发送通知
 */
@Component
@Slf4j
public class CommentModerationEventListener {

    @Autowired
    private ContentModerationService contentModerationService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private RedisDistributedLock redisDistributedLock;

    @Autowired
    private CommentService commentService;

    private static final String MODERATION_LOCK_PREFIX = "moderation:comment:";

    /**
     * 异步处理评论审核事件
     */
    @EventListener
    @Async("moderationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentModerationEvent(CommentModerationEvent event) {
        log.info("开始异步AI评论审核: commentId={}", event.getCommentId());

        // 获取分布式锁，防止重复审核
        String lockKey = MODERATION_LOCK_PREFIX + event.getCommentId();
        String lockValue = null;
        try {
            lockValue = redisDistributedLock.tryLock(lockKey, 30, TimeUnit.SECONDS, 0, TimeUnit.SECONDS);
            if (lockValue == null) {
                log.info("评论正在审核中，跳过重复审核: commentId={}", event.getCommentId());
                return;
            }

            // 调用AI审核
            var moderationResult = contentModerationService.moderateComment(event.getContent());

            // 检查审核是否成功
            if (!moderationResult.isSuccess() || moderationResult.getData() == null) {
                log.warn("AI评论审核返回异常结果, commentId={}, success={}, data={}",
                        event.getCommentId(), moderationResult.isSuccess(),
                        moderationResult.getData());
                return;
            }

            ModerationResult result = moderationResult.getData();

            // 根据审核结果事务性地落库（状态更新与计数/热度调整原子化）
            // 落库失败时有限重试（最多3次，间隔500ms、2000ms），都失败则评论保持待审核，记录补偿日志
            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    commentService.applyModerationResult(event.getCommentId(), result.isPassed());
                    break;
                } catch (Exception e) {
                    if (attempt == 3) {
                        log.error("评论审核结果落库最终失败，评论保持待审核，需补偿: commentId={}", event.getCommentId(), e);
                        return;
                    }
                    log.warn("评论审核结果落库失败，准备重试: commentId={}, attempt={}/3",
                            event.getCommentId(), attempt, e);
                    try {
                        Thread.sleep(attempt == 1 ? 500L : 2000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("评论审核结果落库重试被中断，评论保持待审核: commentId={}", event.getCommentId());
                        return;
                    }
                }
            }

            if (!result.isPassed()) {
                // 发送审核未通过通知
                String reason = String.join(", ", result.getReasons());
                String notificationContent = "您的评论《" + truncateContent(event.getContent()) + "》未通过内容审核。\n" +
                        "原因：" + reason;
                notificationService.createNotification(
                        event.getUserId(),
                        null,
                        Notification.TYPE_COMMENT_MODERATION_FAILED,
                        event.getCommentId(),
                        Notification.TARGET_TYPE_COMMENT,
                        notificationContent
                );
                log.info("评论审核未通过，已发送通知: commentId={}, reason={}", event.getCommentId(), reason);
            }

        } catch (Exception e) {
            log.error("AI评论审核异常: commentId={}", event.getCommentId(), e);
        } finally {
            // 释放分布式锁
            if (lockValue != null) {
                redisDistributedLock.unlock(lockKey, lockValue);
            }
        }
    }

    private String truncateContent(String content) {
        if (content == null) return "";
        return content.length() > 20 ? content.substring(0, 20) + "..." : content;
    }
}
