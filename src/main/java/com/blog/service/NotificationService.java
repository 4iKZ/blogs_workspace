package com.blog.service;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.dto.NotificationDTO;

/**
 * 消息通知服务接口
 */
public interface NotificationService {

    /**
     * 创建通知
     * @param userId 接收通知的用户ID
     * @param senderId 触发通知的用户ID
     * @param type 通知类型
     * @param targetId 目标ID
     * @param targetType 目标类型
     * @param content 通知内容
     * @return 通知ID
     */
    Result<Long> createNotification(Long userId, Long senderId, Integer type, Long targetId, Integer targetType, String content);

    /**
     * 获取用户的未读消息数量
     * @param userId 用户ID
     * @return 未读消息数量
     */
    Result<Integer> getUnreadCount(Long userId);

    /**
     * 获取用户的消息列表（分页）
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页数量
     * @return 分页消息列表
     */
    Result<PageResult<NotificationDTO>> getNotificationList(Long userId, Integer page, Integer size);

    /**
     * 标记消息为已读
     * @param id 消息ID
     * @param userId 用户ID
     * @return 操作结果
     */
    Result<Void> markAsRead(Long id, Long userId);

    /**
     * 标记所有消息为已读
     * @param userId 用户ID
     * @return 操作结果
     */
    Result<Void> markAllAsRead(Long userId);

    /**
     * 删除消息
     * @param id 消息ID
     * @param userId 用户ID
     * @return 操作结果
     */
    Result<Void> deleteNotification(Long id, Long userId);
}
