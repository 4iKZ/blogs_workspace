package com.blog.event;

import com.blog.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 通知事件监听器
 * 异步处理通知创建，避免阻塞主业务流程
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    @Autowired
    private NotificationService notificationService;

    /**
     * 异步处理通知创建事件
     * 使用 @EventListener 注册为Spring事件监听器
     * 使用 @TransactionalEventListener 确保在主事务提交后再处理
     * 使用 @Async 使方法在独立线程中执行
     */
    @EventListener
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationEvent(NotificationEvent event) {
        try {
            log.info("开始异步创建通知: {}", event);

            // 调用通知服务创建通知
            notificationService.createNotification(
                    event.getUserId(),
                    event.getSenderId(),
                    event.getType(),
                    event.getTargetId(),
                    event.getTargetType(),
                    event.getContent()
            );

            log.info("异步创建通知成功: userId={}, senderId={}, type={}",
                    event.getUserId(), event.getSenderId(), event.getType());
        } catch (Exception e) {
            // 通知创建失败不应影响主业务，仅记录日志
            log.error("异步创建通知失败: {}", event, e);
        }
    }
}
