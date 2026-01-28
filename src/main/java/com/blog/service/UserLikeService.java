package com.blog.service;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.dto.UserLikeDTO;

import java.util.List;

/**
 * 用户点赞服务接口
 */
public interface UserLikeService {

    /**
     * 点赞文章
     * @param articleId 文章ID
     * @return 点赞ID
     */
    Result<Long> likeArticle(Long articleId);

    /**
     * 取消点赞文章
     * @param articleId 文章ID
     * @return 操作结果
     */
    Result<Void> unlikeArticle(Long articleId);

    /**
     * 获取用户点赞列表
     * @param page 页码
     * @param size 每页数量
     * @return 分页点赞列表
     */
    Result<PageResult<UserLikeDTO>> getUserLikes(Integer page, Integer size);

    /**
     * 检查文章是否已点赞
     * @param articleId 文章ID
     * @return 是否已点赞
     */
    Result<Boolean> isArticleLiked(Long articleId);

    /**
     * 获取用户点赞数量
     * @return 点赞数量
     */
    Result<Integer> getUserLikeCount();
}