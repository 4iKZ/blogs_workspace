package com.blog.service;

import com.blog.common.Result;
import com.blog.common.PageResult;
import com.blog.dto.UserDTO;
import com.blog.dto.UserLoginDTO;
import com.blog.dto.UserRegisterDTO;
import com.blog.dto.UserUpdateDTO;
import com.blog.dto.ChangePasswordDTO;
import com.blog.dto.SendResetCodeDTO;
import com.blog.dto.SendRegisterCodeDTO;
import com.blog.dto.ResetPasswordByCodeDTO;
import com.blog.dto.TokenRefreshResponseDTO;
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
     * 用户登出（支持刷新令牌注销）
     *
     * @param userId 用户ID
     * @param refreshToken 刷新令牌（可选）
     * @return 登出结果
     */
    Result<Void> logout(Long userId, String refreshToken);

    /**
     * 刷新JWT令牌
     * 
     * @param refreshToken 刷新令牌
     * @return 新的JWT令牌
     */
    Result<TokenRefreshResponseDTO> refreshToken(String refreshToken);

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

    Result<Void> changePassword(Long userId, ChangePasswordDTO changePasswordDTO, String authorizationHeader);

    /**
     * 重置密码
     * 
     * @param email       邮箱
     * @param newPassword 新密码
     * @return 重置结果
     */
    Result<Void> resetPassword(String email, String newPassword);

    /**
     * 发送邮箱重置验证码
     *
     * @param sendResetCodeDTO 发送验证码请求
     * @return 发送结果
     */
    Result<Void> sendResetCode(SendResetCodeDTO sendResetCodeDTO);

    /**
     * 通过邮箱验证码重置密码
     *
     * @param resetPasswordByCodeDTO 重置请求
     * @return 重置结果
     */
    Result<Void> resetPasswordByCode(ResetPasswordByCodeDTO resetPasswordByCodeDTO);

    /**
     * 兼容前端的刷新令牌接口
     *
     * @param refreshToken 刷新令牌（可为空）
     * @param authorizationHeader Authorization请求头（可为空）
     * @return 新访问令牌
     */
    Result<TokenRefreshResponseDTO> refreshTokenCompatible(String refreshToken, String authorizationHeader);

    /**
     * 校验访问令牌是否有效
     *
     * @param authorizationHeader Authorization请求头
     * @return 是否有效
     */
    Result<Boolean> validateToken(String authorizationHeader);

    /**
     * 获取用户列表（管理员功能）
     * 
     * @param page    页码
     * @param size    每页大小
     * @param keyword 搜索关键词
     * @return 用户列表
     */
    Result<PageResult<UserDTO>> getUserList(Integer page, Integer size, String keyword);

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

    /**
     * 发送注册邮箱验证码（需要图形验证码校验）
     *
     * @param sendRegisterCodeDTO 发送验证码请求（含邮箱和图形验证码）
     * @return 发送结果
     */
    Result<Void> sendRegisterVerifyCode(SendRegisterCodeDTO sendRegisterCodeDTO);
}
