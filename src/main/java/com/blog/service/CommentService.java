package com.blog.service;

import com.blog.common.Result;
import com.blog.dto.CommentCreateDTO;
import com.blog.dto.CommentDTO;

import java.util.List;
import java.util.Map;

/**
 * 评论服务接口
 */
public interface CommentService {

    /**
     * 发表评论
     */
    Result<Long> createComment(CommentCreateDTO commentCreateDTO);

    /**
     * 获取评论列表
     */
    Result<List<CommentDTO>> getCommentList(Long articleId, Integer page, Integer size, Integer status, String sortBy, Long userId);

    /**
     * 获取评论详情
     */
    Result<CommentDTO> getCommentById(Long commentId);

    /**
     * 删除评论
     */
    Result<Void> deleteComment(Long commentId);

    /**
     * 获取文章的评论数量
     */
    Result<Integer> getArticleCommentCount(Long articleId);

    /**
     * 获取用户的评论列表
     */
    Result<List<CommentDTO>> getUserComments(Long userId, Integer page, Integer size);

    /**
     * 评论点赞
     */
    Result<Void> likeComment(Long commentId);

    /**
     * 取消评论点赞
     */
    Result<Void> unlikeComment(Long commentId);

    /**
     * 检查用户是否点赞了评论
     */
    Result<Boolean> checkCommentLikeStatus(Long commentId, Long userId);

    /**
     * 批量检查用户对多个评论的点赞状态
     */
    Result<Map<Long, Boolean>> batchCheckCommentLikeStatus(List<Long> commentIds, Long userId);

    /**
     * 获取热门评论
     */
    Result<List<CommentDTO>> getHotComments(Long articleId, Integer limit);

    /**
     * 获取评论的子评论列表
     */
    Result<List<CommentDTO>> getChildComments(Long parentId, Integer page, Integer size);

    /**
     * 清除指定文章相关的评论缓存
     */
    void clearCommentCache(Long articleId);

    /**
     * 应用评论AI审核结果（事务性）
     * 通过：状态置为已通过并增加文章评论数、清除缓存；拒绝：状态置为已拒绝并扣减热度分
     * @param commentId 评论ID
     * @param passed 是否通过审核
     */
    void applyModerationResult(Long commentId, boolean passed);

}
