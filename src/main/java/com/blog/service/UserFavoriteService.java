package com.blog.service;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.dto.UserFavoriteDTO;

import java.util.List;

/**
 * 用户收藏服务接口
 */
public interface UserFavoriteService {

    /**
     * 收藏文章
     * @param articleId 文章ID
     * @return 收藏ID
     */
    Result<Long> favoriteArticle(Long articleId);

    /**
     * 取消收藏文章
     * @param articleId 文章ID
     * @return 操作结果
     */
    Result<Void> unfavoriteArticle(Long articleId);

    /**
     * 获取用户收藏列表
     * @param page 页码
     * @param size 每页数量
     * @return 收藏列表
     */
    Result<PageResult<UserFavoriteDTO>> getUserFavorites(Integer page, Integer size);

    /**
     * 检查文章是否已收藏
     * @param articleId 文章ID
     * @return 是否已收藏
     */
    Result<Boolean> isArticleFavorited(Long articleId);

    /**
     * 获取用户收藏数量
     * @return 收藏数量
     */
    Result<Integer> getUserFavoriteCount();
}