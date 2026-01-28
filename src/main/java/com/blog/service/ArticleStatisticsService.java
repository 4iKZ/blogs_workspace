package com.blog.service;

import com.blog.common.Result;
import com.blog.dto.ArticleStatisticsDTO;

import java.util.List;

/**
 * 文章统计服务接口
 */
public interface ArticleStatisticsService {

    /**
     * 获取文章统计信息
     * @param articleId 文章ID
     * @return 文章统计信息
     */
    Result<ArticleStatisticsDTO> getArticleStatistics(Long articleId);

    /**
     * 增加文章浏览量
     * @param articleId 文章ID
     * @return 操作结果
     */
    Result<Void> incrementViewCount(Long articleId);

    /**
     * 增加文章点赞数
     * @param articleId 文章ID
     * @return 操作结果
     */
    Result<Void> incrementLikeCount(Long articleId);

    /**
     * 减少文章点赞数
     * @param articleId 文章ID
     * @return 操作结果
     */
    Result<Void> decrementLikeCount(Long articleId);

    /**
     * 增加文章评论数
     * @param articleId 文章ID
     * @return 操作结果
     */
    Result<Void> incrementCommentCount(Long articleId);

    /**
     * 减少文章评论数
     * @param articleId 文章ID
     * @return 操作结果
     */
    Result<Void> decrementCommentCount(Long articleId);

    /**
     * 增加文章收藏数
     * @param articleId 文章ID
     * @return 操作结果
     */
    Result<Void> incrementFavoriteCount(Long articleId);

    /**
     * 减少文章收藏数
     * @param articleId 文章ID
     * @return 操作结果
     */
    Result<Void> decrementFavoriteCount(Long articleId);

    /**
     * 获取热门文章统计
     * @param limit 数量限制
     * @return 热门文章统计列表
     */
    Result<List<ArticleStatisticsDTO>> getHotArticleStatistics(Integer limit);

    /**
     * 获取置顶文章统计
     * @param limit 数量限制
     * @return 置顶文章统计列表
     */
    Result<List<ArticleStatisticsDTO>> getTopArticleStatistics(Integer limit);

    /**
     * 获取推荐文章统计
     * @param limit 数量限制
     * @return 推荐文章统计列表
     */
    Result<List<ArticleStatisticsDTO>> getRecommendedArticleStatistics(Integer limit);
}