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
     * 审核评论
     */
    Result<Void> reviewComment(Long commentId, Integer status);

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
     * 检测评论内容是否包含敏感词
     */
    Result<Boolean> checkSensitiveWords(String content);

    /**
     * 替换评论内容中的敏感词
     */
    Result<String> replaceSensitiveWords(String content);

    /**
     * 获取评论的子评论列表
     */
    Result<List<CommentDTO>> getChildComments(Long parentId, Integer page, Integer size);

}
