package com.blog.event;

import com.blog.dto.ModerationResult;
import com.blog.entity.Comment;
import com.blog.entity.Notification;
import com.blog.mapper.CommentMapper;
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
    private CommentMapper commentMapper;

    @Autowired
    private RedisDistributedLock redisDistributedLock;

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

            // 根据审核结果处理
            if (result.isPassed()) {
                handlePassed(event, result);
            } else {
                handleNotPassed(event, result);
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

    private void handlePassed(CommentModerationEvent event, ModerationResult result) {
        try {
            // 更新评论状态为已通过
            Comment comment = commentMapper.selectById(event.getCommentId());
            if (comment != null) {
                comment.setStatus(2); // 已通过
                commentMapper.updateById(comment);
                log.info("评论审核通过: commentId={}", event.getCommentId());
            }
        } catch (Exception e) {
            log.error("处理评论审核通过结果异常: commentId={}", event.getCommentId(), e);
        }
    }

    private void handleNotPassed(CommentModerationEvent event, ModerationResult result) {
        try {
            String reason = result != null ? String.join(", ", result.getReasons()) : "未知原因";

            // 更新评论状态为已拒绝
            Comment comment = commentMapper.selectById(event.getCommentId());
            if (comment != null) {
                comment.setStatus(3); // 已拒绝
                commentMapper.updateById(comment);
            }

            // 发送审核未通过通知
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
        } catch (Exception e) {
            log.error("处理评论审核未通过结果异常: commentId={}", event.getCommentId(), e);
        }
    }

    private String truncateContent(String content) {
        if (content == null) return "";
        return content.length() > 20 ? content.substring(0, 20) + "..." : content;
    }
}
