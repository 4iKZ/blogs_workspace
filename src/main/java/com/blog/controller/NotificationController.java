package com.blog.controller;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.dto.NotificationDTO;
import com.blog.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 消息通知控制器
 */
@RestController
@RequestMapping("/api/notification")
@Tag(name = "消息通知管理", description = "消息通知相关接口")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private HttpServletRequest request;

    @GetMapping("/unread-count")
    @Operation(summary = "获取未读消息数量")
    public Result<Integer> getUnreadCount() {
        Long userId = getCurrentUserId();
        return notificationService.getUnreadCount(userId);
    }

    @GetMapping("/list")
    @Operation(summary = "获取消息列表（分页）")
    public Result<PageResult<NotificationDTO>> getNotificationList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer size) {
        Long userId = getCurrentUserId();
        return notificationService.getNotificationList(userId, page, size);
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记消息为已读")
    public Result<Void> markAsRead(@Parameter(description = "消息ID") @PathVariable Long id) {
        Long userId = getCurrentUserId();
        return notificationService.markAsRead(id, userId);
    }

    @PutMapping("/read-all")
    @Operation(summary = "标记所有消息为已读")
    public Result<Void> markAllAsRead() {
        Long userId = getCurrentUserId();
        return notificationService.markAllAsRead(userId);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除消息")
    public Result<Void> deleteNotification(@Parameter(description = "消息ID") @PathVariable Long id) {
        Long userId = getCurrentUserId();
        return notificationService.deleteNotification(id, userId);
    }

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            throw new com.blog.exception.BusinessException(
                com.blog.common.ResultCode.UNAUTHORIZED, "用户未登录");
        }
        return Long.valueOf(userId.toString());
    }
}
