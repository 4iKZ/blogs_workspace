package com.blog.service;

import com.blog.common.Result;
import com.blog.dto.ArticleDTO;
import com.blog.dto.CommentDTO;
import com.blog.dto.UserDTO;

import java.util.List;
import java.util.Map;

/**
 * 后台管理服务接口
 */
public interface AdminService {

    /**
     * 获取用户列表（管理员）
     */
    Result<List<UserDTO>> getUserList(Integer page, Integer size, String keyword, Integer status);

    /**
     * 修改用户状态
     */
    Result<Void> updateUserStatus(Long userId, Integer status);

    /**
     * 删除用户
     */
    Result<Void> deleteUser(Long userId);

    /**
     * 获取文章列表（管理员）
     */
    Result<List<ArticleDTO>> getArticleList(Integer page, Integer size, String keyword, Integer status);

    /**
     * 修改文章状态
     */
    Result<Void> updateArticleStatus(Long articleId, Integer status);

    /**
     * 删除文章（管理员）
     */
    Result<Void> deleteArticle(Long articleId);

    /**
     * 获取评论列表（管理员）
     */
    Result<List<CommentDTO>> getCommentList(Integer page, Integer size, String keyword, Integer status, Long articleId);

    /**
     * 获取网站统计信息
     */
    Result<Map<String, Object>> getWebsiteStatistics();

    /**
     * 获取访问统计
     */
    Result<Map<String, Object>> getVisitStatistics(String startDate, String endDate);

    /**
     * 获取系统配置
     */
    Result<Map<String, String>> getSystemConfig();

    /**
     * 更新系统配置
     */
    Result<Void> updateSystemConfig(Map<String, String> config);

    /**
     * 数据备份
     */
    Result<String> backupDatabase();

    /**
     * 清理缓存
     */
    Result<Void> clearCache();
}