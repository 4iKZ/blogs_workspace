package com.blog.service;

import com.blog.common.Result;
import com.blog.dto.UserDTO;
import com.blog.dto.UserLoginDTO;
import com.blog.dto.UserRegisterDTO;
import com.blog.dto.UserUpdateDTO;
import com.blog.dto.ChangePasswordDTO;
import com.blog.entity.User;

import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户注册
     * 
     * @param registerDTO 注册信息
     * @return 注册结果
     */
    Result<String> register(UserRegisterDTO registerDTO);

    /**
     * 用户登录
     * 
     * @param loginDTO 登录信息
     * @return 登录结果（包含JWT令牌）
     */
    Result<UserDTO> login(UserLoginDTO loginDTO);

    /**
     * 用户登出
     * 
     * @param userId 用户ID
     * @return 登出结果
     */
    Result<Void> logout(Long userId);

    /**
     * 刷新JWT令牌
     * 
     * @param refreshToken 刷新令牌
     * @return 新的JWT令牌
     */
    Result<String> refreshToken(String refreshToken);

    /**
     * 获取用户信息
     * 
     * @param userId 用户ID
     * @return 用户信息
     */
    Result<UserDTO> getUserInfo(Long userId);

    /**
     * 更新用户信息
     * 
     * @param userId    用户ID
     * @param updateDTO 更新信息
     * @return 更新结果
     */
    Result<Void> updateUserInfo(Long userId, UserUpdateDTO updateDTO);

    /**
     * 修改密码
     *
     * @param userId            用户ID
     * @param changePasswordDTO 修改密码请求DTO
     * @return 修改结果
     */
    Result<Void> changePassword(Long userId, ChangePasswordDTO changePasswordDTO);

    /**
     * 重置密码
     * 
     * @param email       邮箱
     * @param newPassword 新密码
     * @return 重置结果
     */
    Result<Void> resetPassword(String email, String newPassword);

    /**
     * 获取用户列表（管理员功能）
     * 
     * @param page    页码
     * @param size    每页大小
     * @param keyword 搜索关键词
     * @return 用户列表
     */
    Result<List<UserDTO>> getUserList(Integer page, Integer size, String keyword);

    /**
     * 禁用/启用用户（管理员功能）
     * 
     * @param userId 用户ID
     * @param status 状态（0-禁用，1-启用）
     * @return 操作结果
     */
    Result<Void> updateUserStatus(Long userId, Integer status);

    /**
     * 删除用户（管理员功能）
     * 
     * @param userId 用户ID
     * @return 删除结果
     */
    Result<Void> deleteUser(Long userId);

    /**
     * 根据用户名获取用户
     * 
     * @param username 用户名
     * @return 用户信息
     */
    User getUserByUsername(String username);

    /**
     * 根据邮箱获取用户
     * 
     * @param email 邮箱
     * @return 用户信息
     */
    User getUserByEmail(String email);

    /**
     * 根据用户ID获取用户（内部使用）
     * 
     * @param userId 用户ID
     * @return 用户信息
     */
    User getUserById(Long userId);

    /**
     * 获取用户公开信息
     * 
     * @param userId 用户ID
     * @return 用户公开信息DTO
     */
    Result<UserDTO> getPublicUserInfo(Long userId);

    /**
     * 关注用户
     * 
     * @param followerId  关注者ID
     * @param followingId 被关注者ID
     * @return 操作结果
     */
    Result<Void> follow(Long followerId, Long followingId);

    /**
     * 取消关注用户
     * 
     * @param followerId  关注者ID
     * @param followingId 被关注者ID
     * @return 操作结果
     */
    Result<Void> unfollow(Long followerId, Long followingId);

    /**
     * 检查是否关注
     * 
     * @param followerId  关注者ID
     * @param followingId 被关注者ID
     * @return 是否关注
     */
    Result<Boolean> isFollowing(Long followerId, Long followingId);

    /**
     * 获取作者排行榜（按粉丝数排序）
     * 
     * @param limit 数量限制
     * @return 作者列表
     */
    Result<List<UserDTO>> getTopAuthors(Integer limit);

    /**
     * 获取当前用户的关注列表
     * 
     * @param userId 用户ID
     * @param page   页码
     * @param size   每页数量
     * @return 关注的用户列表
     */
    Result<List<UserDTO>> getFollowings(Long userId, Integer page, Integer size);

    /**
     * 获取当前用户的粉丝列表
     * 
     * @param userId 用户ID
     * @param page   页码
     * @param size   每页数量
     * @return 粉丝用户列表
     */
    Result<List<UserDTO>> getFollowers(Long userId, Integer page, Integer size);
}
