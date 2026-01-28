package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.User;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户实体
     */
    @Select("SELECT * FROM users WHERE username = #{username}")
    User selectByUsername(@Param("username") String username);

    /**
     * 根据邮箱查询用户
     * @param email 邮箱地址
     * @return 用户实体
     */
    @Select("SELECT * FROM users WHERE email = #{email}")
    User selectByEmail(@Param("email") String email);

    /**
     * 检查用户名是否存在
     * @param username 用户名
     * @return 存在返回1，不存在返回0
     */
    @Select("SELECT COUNT(1) FROM users WHERE username = #{username}")
    int countByUsername(@Param("username") String username);

    /**
     * 检查邮箱是否存在
     * @param email 邮箱地址
     * @return 存在返回1，不存在返回0
     */
    @Select("SELECT COUNT(1) FROM users WHERE email = #{email}")
    int countByEmail(@Param("email") String email);

    /**
     * 更新用户最后登录信息
     * @param userId 用户ID
     * @param loginTime 登录时间
     * @param loginIp 登录IP
     * @return 影响行数
     */
    @Update("UPDATE users SET last_login_time = #{loginTime}, last_login_ip = #{loginIp}, update_time = #{loginTime} WHERE id = #{userId}")
    int updateLastLoginInfo(@Param("userId") Long userId,
                           @Param("loginTime") java.time.LocalDateTime loginTime,
                           @Param("loginIp") String loginIp);
    
    /**
     * 统计用户总数
     * @return 用户总数
     */
    @Select("SELECT COUNT(1) FROM users")
    int countTotalUsers();
    
    /**
     * 统计活跃用户数（最近30天有登录记录）
     * @return 活跃用户数
     */
    @Select("SELECT COUNT(1) FROM users WHERE last_login_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)")
    int countActiveUsers();
    
    /**
     * 统计今日新增用户数
     * @return 今日新增用户数
     */
    @Select("SELECT COUNT(1) FROM users WHERE DATE(create_time) = CURDATE()")
    int countNewUsersToday();
    
    /**
     * 分页查询用户列表
     * @param offset 偏移量
     * @param size 查询数量
     * @param keyword 关键词（可选）
     * @param status 状态（可选）
     * @return 用户列表
     */
    @Select("<script>" +
            "SELECT * FROM users " +
            "<if test='keyword != null and keyword != &quot;&quot;'>" +
            "WHERE (username LIKE CONCAT('%', #{keyword}, '%') OR nickname LIKE CONCAT('%', #{keyword}, '%') OR email LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "<if test='keyword == null or keyword == &quot;&quot; and status != null'>WHERE status = #{status}</if>" +
            "<if test='keyword != null and keyword != &quot;&quot; and status != null'>AND status = #{status}</if>" +
            "ORDER BY create_time DESC " +
            "LIMIT #{offset}, #{size}" +
            "</script>")
    List<User> selectUserList(@Param("offset") Integer offset, 
                             @Param("size") Integer size, 
                             @Param("keyword") String keyword, 
                             @Param("status") Integer status);
    
    /**
     * 更新用户状态
     * @param userId 用户ID
     * @param status 状态
     * @return 影响行数
     */
    @Update("UPDATE users SET status = #{status}, update_time = NOW() WHERE id = #{userId}")
    int updateStatus(@Param("userId") Long userId, @Param("status") Integer status);
    


    /**
     * 增加粉丝数
     * @param userId 用户ID
     * @return 影响行数
     */
    @Update("UPDATE users SET follower_count = follower_count + 1, update_time = NOW() WHERE id = #{userId}")
    int incrementFollowerCount(@Param("userId") Long userId);

    /**
     * 减少粉丝数
     * @param userId 用户ID
     * @return 影响行数
     */
    @Update("UPDATE users SET follower_count = GREATEST(follower_count - 1, 0), update_time = NOW() WHERE id = #{userId}")
    int decrementFollowerCount(@Param("userId") Long userId);

    /**
     * 增加关注数
     * @param userId 用户ID
     * @return 影响行数
     */
    @Update("UPDATE users SET following_count = following_count + 1, update_time = NOW() WHERE id = #{userId}")
    int incrementFollowingCount(@Param("userId") Long userId);

    /**
     * 减少关注数
     * @param userId 用户ID
     * @return 影响行数
     */
    @Update("UPDATE users SET following_count = GREATEST(following_count - 1, 0), update_time = NOW() WHERE id = #{userId}")
    int decrementFollowingCount(@Param("userId") Long userId);
}