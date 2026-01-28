package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.CommentNotification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 评论通知Mapper接口
 */
@Mapper
public interface CommentNotificationMapper extends BaseMapper<CommentNotification> {

    /**
     * 获取用户未读通知数量
     * @param userId 用户ID
     * @return 未读通知数量
     */
    @Select("SELECT COUNT(*) FROM comment_notifications WHERE receiver_id = #{userId} AND read_status = 0")
    Integer getUnreadNotificationCount(@Param("userId") Long userId);

    /**
     * 获取用户通知列表
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页数量
     * @return 通知列表
     */
    @Select("SELECT * FROM comment_notifications WHERE receiver_id = #{userId} ORDER BY create_time DESC LIMIT #{offset}, #{size}")
    List<CommentNotification> getUserNotifications(@Param("userId") Long userId, @Param("offset") Integer offset, @Param("size") Integer size);

    /**
     * 标记通知为已读
     * @param notificationId 通知ID
     * @return 影响行数
     */
    @Update("UPDATE comment_notifications SET read_status = 1 WHERE id = #{notificationId}")
    int markNotificationAsRead(@Param("notificationId") Long notificationId);

    /**
     * 标记所有通知为已读
     * @param userId 用户ID
     * @return 影响行数
     */
    @Update("UPDATE comment_notifications SET read_status = 1 WHERE receiver_id = #{userId}")
    int markAllNotificationsAsRead(@Param("userId") Long userId);

    /**
     * 根据评论ID删除通知
     * @param commentId 评论ID
     * @return 影响行数
     */
    @Select("DELETE FROM comment_notifications WHERE comment_id = #{commentId}")
    int deleteByCommentId(@Param("commentId") Long commentId);
}
