package com.blog.service.impl;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.common.ResultCode;
import com.blog.dto.NotificationDTO;
import com.blog.entity.Article;
import com.blog.entity.Comment;
import com.blog.entity.Notification;
import com.blog.entity.User;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.NotificationMapper;
import com.blog.mapper.UserMapper;
import com.blog.service.NotificationService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 消息通知服务实现类
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Override
    public Result<Long> createNotification(Long userId, Long senderId, Integer type, Long targetId, Integer targetType, String content) {
        try {
            // 不给自己发送通知
            if (userId.equals(senderId)) {
                return Result.success(0L);
            }

            // 检查是否已存在相同的未读通知，避免重复通知
            int existingCount = notificationMapper.countExistingNotification(userId, senderId, type, targetId, targetType);
            if (existingCount > 0) {
                log.debug("通知已存在，跳过创建: userId={}, senderId={}, type={}, targetId={}", 
                         userId, senderId, type, targetId);
                return Result.success(0L);
            }

            // 创建通知对象
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setSenderId(senderId);
            notification.setType(type);
            notification.setTargetId(targetId);
            notification.setTargetType(targetType);
            notification.setContent(content);
            notification.setIsRead(Notification.READ_STATUS_UNREAD);

            // 保存通知
            notificationMapper.insert(notification);

            log.info("创建通知成功: id={}, userId={}, senderId={}, type={}", 
                    notification.getId(), userId, senderId, type);

            return Result.success(notification.getId());
        } catch (Exception e) {
            log.error("创建通知失败", e);
            return Result.error(ResultCode.ERROR, "创建通知失败");
        }
    }

    @Override
    public Result<Integer> getUnreadCount(Long userId) {
        try {
            int count = notificationMapper.countUnreadByUserId(userId);
            return Result.success(count);
        } catch (Exception e) {
            log.error("获取未读消息数量失败", e);
            return Result.error(ResultCode.ERROR, "获取未读消息数量失败");
        }
    }

    @Override
    public Result<PageResult<NotificationDTO>> getNotificationList(Long userId, Integer page, Integer size) {
        try {
            // 计算偏移量
            int offset = (page - 1) * size;

            // 查询通知列表
            List<Notification> notifications = notificationMapper.selectByUserId(userId, offset, size);

            // 查询总数
            long total = notificationMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Notification>()
                            .eq(Notification::getUserId, userId)
            );

            if (notifications.isEmpty()) {
                return Result.success(PageResult.empty(page, size));
            }

            // 批量预加载所有需要的数据，解决 N+1 查询问题
            // 1. 收集所有发送者ID
            Set<Long> senderIds = notifications.stream()
                    .map(Notification::getSenderId)
                    .collect(Collectors.toSet());

            // 2. 批量查询发送者信息
            Map<Long, User> senderMap = userMapper.selectBatchIds(senderIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));

            // 3. 收集所有文章ID
            Set<Long> articleIds = notifications.stream()
                    .filter(n -> n.getTargetType() != null && n.getTargetType() == Notification.TARGET_TYPE_ARTICLE)
                    .map(Notification::getTargetId)
                    .collect(Collectors.toSet());

            // 4. 批量查询文章信息
            Map<Long, Article> articleMap = articleIds.isEmpty() ? Map.of() :
                    articleMapper.selectBatchIds(articleIds).stream()
                            .collect(Collectors.toMap(Article::getId, a -> a));

            // 5. 收集所有评论ID
            Set<Long> commentIds = notifications.stream()
                    .filter(n -> n.getTargetType() != null && n.getTargetType() == Notification.TARGET_TYPE_COMMENT)
                    .map(Notification::getTargetId)
                    .collect(Collectors.toSet());

            // 6. 批量查询评论信息
            Map<Long, Comment> commentMap = commentIds.isEmpty() ? Map.of() :
                    commentMapper.selectBatchIds(commentIds).stream()
                            .collect(Collectors.toMap(Comment::getId, c -> c));

            // 使用预加载的数据批量转换DTO
            List<NotificationDTO> dtoList = notifications.stream()
                    .map(n -> convertToDTOWithCache(n, senderMap, articleMap, commentMap))
                    .collect(Collectors.toList());

            return Result.success(PageResult.of(dtoList, total, page, size));
        } catch (Exception e) {
            log.error("获取消息列表失败", e);
            return Result.error(ResultCode.ERROR, "获取消息列表失败");
        }
    }

    @Override
    public Result<Void> markAsRead(Long id, Long userId) {
        try {
            int rows = notificationMapper.markAsRead(id, userId);
            if (rows > 0) {
                return Result.success();
            } else {
                return Result.error(ResultCode.NOT_FOUND, "消息不存在");
            }
        } catch (Exception e) {
            log.error("标记消息已读失败", e);
            return Result.error(ResultCode.ERROR, "标记消息已读失败");
        }
    }

    @Override
    public Result<Void> markAllAsRead(Long userId) {
        try {
            notificationMapper.markAllAsRead(userId);
            return Result.success();
        } catch (Exception e) {
            log.error("标记所有消息已读失败", e);
            return Result.error(ResultCode.ERROR, "标记所有消息已读失败");
        }
    }

    @Override
    public Result<Void> deleteNotification(Long id, Long userId) {
        try {
            int rows = notificationMapper.deleteByIdAndUserId(id, userId);
            if (rows > 0) {
                return Result.success();
            } else {
                return Result.error(ResultCode.NOT_FOUND, "消息不存在");
            }
        } catch (Exception e) {
            log.error("删除消息失败", e);
            return Result.error(ResultCode.ERROR, "删除消息失败");
        }
    }

    /**
     * 将Notification实体转换为DTO（使用预加载的数据缓存，避免N+1查询）
     */
    private NotificationDTO convertToDTOWithCache(Notification notification,
                                                   Map<Long, User> senderMap,
                                                   Map<Long, Article> articleMap,
                                                   Map<Long, Comment> commentMap) {
        NotificationDTO dto = new NotificationDTO();
        BeanUtils.copyProperties(notification, dto);

        // 从预加载的Map中获取发送者信息，避免数据库查询
        User sender = senderMap.get(notification.getSenderId());
        if (sender != null) {
            dto.setSenderNickname(sender.getNickname() != null ? sender.getNickname() : sender.getUsername());
            dto.setSenderAvatar(sender.getAvatar());
        }

        // 设置通知类型名称
        dto.setTypeName(getTypeName(notification.getType()));

        // 从预加载的Map中获取目标信息（文章标题或评论内容）
        String targetTitle = getTargetTitleFromCache(notification.getTargetId(), notification.getTargetType(), articleMap, commentMap);
        dto.setTargetTitle(targetTitle);

        return dto;
    }

    /**
     * 获取通知类型名称
     */
    private String getTypeName(Integer type) {
        if (type == null) {
            return "未知通知";
        }
        switch (type) {
            case Notification.TYPE_ARTICLE_LIKE:
                return "点赞了你的文章";
            case Notification.TYPE_ARTICLE_COMMENT:
                return "评论了你的文章";
            case Notification.TYPE_COMMENT_LIKE:
                return "点赞了你的评论";
            case Notification.TYPE_COMMENT_REPLY:
                return "回复了你的评论";
            case Notification.TYPE_USER_FOLLOW:
                return "关注了你";
            default:
                return "未知通知";
        }
    }

    /**
     * 从预加载的数据中获取目标标题（避免数据库查询）
     */
    private String getTargetTitleFromCache(Long targetId, Integer targetType,
                                           Map<Long, Article> articleMap,
                                           Map<Long, Comment> commentMap) {
        try {
            if (targetType == null) {
                return "未知";
            }
            if (targetType == Notification.TARGET_TYPE_ARTICLE) {
                Article article = articleMap.get(targetId);
                return article != null ? article.getTitle() : "未知文章";
            } else if (targetType == Notification.TARGET_TYPE_COMMENT) {
                Comment comment = commentMap.get(targetId);
                if (comment != null && comment.getContent() != null) {
                    // 截取评论内容前50个字符作为标题
                    String content = comment.getContent();
                    return content.length() > 50 ? content.substring(0, 50) + "..." : content;
                }
                return "未知评论";
            } else if (targetType == Notification.TARGET_TYPE_USER) {
                return "点击查看主页";
            }
        } catch (Exception e) {
            log.error("获取目标标题失败", e);
        }
        return "未知";
    }
}
