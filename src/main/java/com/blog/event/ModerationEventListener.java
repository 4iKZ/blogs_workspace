package com.blog.event;

import com.blog.dto.ModerationResult;
import com.blog.entity.Article;
import com.blog.entity.Notification;
import com.blog.mapper.ArticleMapper;
import com.blog.service.ArticleModerationLogService;
import com.blog.service.ArticleRankService;
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
 * 内容审核事件监听器
 * 异步执行AI内容审核，根据结果更新文章状态并发送通知
 */
@Component
@Slf4j
public class ModerationEventListener {

    @Autowired
    private ContentModerationService contentModerationService;

    @Autowired
    private ArticleModerationLogService articleModerationLogService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ArticleRankService articleRankService;

    @Autowired
    private RedisDistributedLock redisDistributedLock;

    private static final String MODERATION_LOCK_PREFIX = "moderation:article:";

    /**
     * 异步处理内容审核事件
     */
    @EventListener
    @Async("moderationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleModerationEvent(ModerationEvent event) {
        log.info("开始异步AI审核: articleId={}, type={}", event.getArticleId(), event.getType());

        // 获取分布式锁，防止重复审核
        String lockKey = MODERATION_LOCK_PREFIX + event.getArticleId();
        String lockValue = null;
        try {
            lockValue = redisDistributedLock.tryLock(lockKey, 30, TimeUnit.SECONDS, 0, TimeUnit.SECONDS);
            if (lockValue == null) {
                log.info("文章正在审核中，跳过重复审核: articleId={}", event.getArticleId());
                return;
            }

            // 调用AI审核
            var moderationResult = contentModerationService.moderateArticle(event.getTitle(), event.getContent());

            // 检查审核是否成功
            if (!moderationResult.isSuccess() || moderationResult.getData() == null) {
                log.warn("AI审核返回异常结果, articleId={}, success={}, data={}",
                        event.getArticleId(), moderationResult.isSuccess(),
                        moderationResult.getData());
                handleException(event, new Exception("AI审核返回异常"));
                return;
            }

            ModerationResult result = moderationResult.getData();

            // 保存审核记录
            articleModerationLogService.saveModerationLog(
                    event.getArticleId(),
                    event.getTitle(),
                    event.getContent(),
                    result
            );

            // 根据审核结果处理
            if (result.isPassed()) {
                // 审核通过
                handlePassed(event, result);
            } else {
                // 审核未通过
                handleNotPassed(event, result);
            }

        } catch (Exception e) {
            log.error("AI审核异常: articleId={}", event.getArticleId(), e);
            // AI服务异常时，设置为已发布状态（降级处理）
            handleException(event, e);
        } finally {
            // 释放分布式锁
            if (lockValue != null) {
                redisDistributedLock.unlock(lockKey, lockValue);
            }
        }
    }

    private void handlePassed(ModerationEvent event, ModerationResult result) {
        try {
            // 更新文章状态为已发布
            Article article = articleMapper.selectById(event.getArticleId());
            if (article != null) {
                article.setStatus(2); // 已发布
                article.setPublishTime(java.time.LocalDateTime.now());
                articleMapper.updateById(article);

                // 初始化文章到排行榜 ZSet
                articleRankService.initializeArticle(event.getArticleId());

                // 发送审核通过通知
                String notificationContent = "您的文章《" + event.getTitle() + "》已通过内容审核，成功发布！";
                notificationService.createNotification(
                        event.getAuthorId(),
                        null,
                        Notification.TYPE_ARTICLE_MODERATION_PASSED,
                        event.getArticleId(),
                        Notification.TARGET_TYPE_ARTICLE,
                        notificationContent
                );

                log.info("文章审核通过，已更新状态并初始化排行榜: articleId={}", event.getArticleId());
            }
        } catch (Exception e) {
            log.error("处理审核通过结果异常: articleId={}", event.getArticleId(), e);
        }
    }

    private void handleNotPassed(ModerationEvent event, ModerationResult result) {
        try {
            String reason = result != null ? String.join(", ", result.getReasons()) : "未知原因";
            String violationType = result != null ? result.getViolationType() : "未知";

            // 发送审核未通过通知
            String notificationContent = "您的文章《" + event.getTitle() + "》未通过内容审核。\n" +
                    "违规类型：" + violationType + "\n" +
                    "原因：" + reason;
            notificationService.createNotification(
                    event.getAuthorId(),
                    null,
                    Notification.TYPE_ARTICLE_MODERATION_FAILED,
                    event.getArticleId(),
                    Notification.TARGET_TYPE_ARTICLE,
                    notificationContent
            );

            log.info("文章审核未通过，已发送通知: articleId={}, reason={}", event.getArticleId(), reason);
        } catch (Exception e) {
            log.error("处理审核未通过结果异常: articleId={}", event.getArticleId(), e);
        }
    }

    private void handleException(ModerationEvent event, Exception e) {
        try {
            // AI服务异常时，设置为已发布状态（降级处理）
            Article article = articleMapper.selectById(event.getArticleId());
            if (article != null) {
                article.setStatus(2); // 已发布
                article.setPublishTime(java.time.LocalDateTime.now());
                articleMapper.updateById(article);

                // 发送通知告知用户
                String notificationContent = "您的文章《" + event.getTitle() + "》已发布（AI审核服务暂时不可用，已自动通过）。";
                notificationService.createNotification(
                        event.getAuthorId(),
                        null,
                        Notification.TYPE_ARTICLE_MODERATION_PASSED,
                        event.getArticleId(),
                        Notification.TARGET_TYPE_ARTICLE,
                        notificationContent
                );

                log.info("AI审核异常，文章已自动发布: articleId={}", event.getArticleId());
            }
        } catch (Exception ex) {
            log.error("处理审核异常结果异常: articleId={}", event.getArticleId(), ex);
        }
    }
}
