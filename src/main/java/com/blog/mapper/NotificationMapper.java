package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 消息通知Mapper接口
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    /**
     * 查询用户的未读消息数量
     * @param userId 用户ID
     * @return 未读消息数量
     */
    @Select("SELECT COUNT(*) FROM notifications WHERE user_id = #{userId} AND is_read = 0")
    int countUnreadByUserId(@Param("userId") Long userId);

    /**
     * 查询用户的消息列表（分页）
     * @param userId 用户ID
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 消息列表
     */
    @Select("SELECT * FROM notifications WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT #{offset}, #{limit}")
    List<Notification> selectByUserId(@Param("userId") Long userId, @Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * 标记指定消息为已读
     * @param id 消息ID
     * @param userId 用户ID
     * @return 影响行数
     */
    @Update("UPDATE notifications SET is_read = 1, update_time = NOW() WHERE id = #{id} AND user_id = #{userId}")
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 标记用户所有消息为已读
     * @param userId 用户ID
     * @return 影响行数
     */
    @Update("UPDATE notifications SET is_read = 1, update_time = NOW() WHERE user_id = #{userId} AND is_read = 0")
    int markAllAsRead(@Param("userId") Long userId);

    /**
     * 删除指定消息
     * @param id 消息ID
     * @param userId 用户ID
     * @return 影响行数
     */
    @Update("DELETE FROM notifications WHERE id = #{id} AND user_id = #{userId}")
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 检查是否已存在相同的通知（防止重复通知）
     * @param userId 接收者ID
     * @param senderId 发送者ID
     * @param type 通知类型
     * @param targetId 目标ID
     * @param targetType 目标类型
     * @return 存在的数量
     */
    @Select("SELECT COUNT(*) FROM notifications WHERE user_id = #{userId} AND sender_id = #{senderId} " +
            "AND type = #{type} AND target_id = #{targetId} AND target_type = #{targetType} AND is_read = 0")
    int countExistingNotification(@Param("userId") Long userId, @Param("senderId") Long senderId,
                                   @Param("type") Integer type, @Param("targetId") Long targetId,
                                   @Param("targetType") Integer targetType);
}
