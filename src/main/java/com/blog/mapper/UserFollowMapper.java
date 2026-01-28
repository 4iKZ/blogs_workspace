package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.UserFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户关注Mapper
 */
@Mapper
public interface UserFollowMapper extends BaseMapper<UserFollow> {

    /**
     * 查询关注关系（包括已逻辑删除的记录）
     * 绕过 MyBatis Plus 的逻辑删除拦截
     * 
     * @param followerId  关注者ID
     * @param followingId 被关注者ID
     * @return 关注关系记录
     */
    @Select("SELECT * FROM user_follows WHERE follower_id = #{followerId} AND following_id = #{followingId} LIMIT 1")
    UserFollow selectByFollowerAndFollowingIncludingDeleted(@Param("followerId") Long followerId,
            @Param("followingId") Long followingId);

    /**
     * 恢复已删除的关注关系
     * 
     * @param id 记录ID
     * @return 影响行数
     */
    @Update("UPDATE user_follows SET deleted = 0, create_time = NOW(), update_time = NOW() WHERE id = #{id}")
    int restoreFollow(@Param("id") Long id);
}
