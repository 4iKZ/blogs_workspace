package com.blog.service;

import com.blog.common.Result;
import com.blog.dto.ArticleStatisticsDTO;

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
     * 强一致语义：文章不存在（更新影响行数为 0）时抛出 BusinessException(ARTICLE_NOT_FOUND)，
     * 不吞异常，由外层事务统一回滚
     * @param articleId 文章ID
     * @return 操作结果
     */
    Result<Void> incrementLikeCount(Long articleId);

    /**
     * 减少文章点赞数
     * 幂等语义：计数已为 0 或文章不存在（更新影响行数为 0）时仍返回成功
     * @param articleId 文章ID
     * @return 操作结果
     */
    Result<Void> decrementLikeCount(Long articleId);

    /**
     * 增加文章评论数
     * 强一致语义：文章不存在（更新影响行数为 0）时抛出 BusinessException(ARTICLE_NOT_FOUND)，
     * 不吞异常，由外层事务统一回滚
     * @param articleId 文章ID
     * @return 操作结果
     */
    Result<Void> incrementCommentCount(Long articleId);

    /**
     * 减少文章评论数
     * 幂等语义：计数已为 0 或文章不存在（更新影响行数为 0）时仍返回成功
     * @param articleId 文章ID
     * @return 操作结果
     */
    Result<Void> decrementCommentCount(Long articleId);

    /**
     * 批量减少文章评论数
     * 幂等语义：计数已为 0 或文章不存在（更新影响行数为 0）时仍返回成功
     * @param articleId 文章ID
     * @param count 减少数量
     * @return 操作结果
     */
    Result<Void> decrementCommentCount(Long articleId, int count);

    /**
     * 增加文章收藏数
     * 强一致语义：文章不存在（更新影响行数为 0）时抛出 BusinessException(ARTICLE_NOT_FOUND)，
     * 不吞异常，由外层事务统一回滚
     * @param articleId 文章ID
     * @return 操作结果
     */
    Result<Void> incrementFavoriteCount(Long articleId);

    /**
     * 减少文章收藏数
     * 幂等语义：计数已为 0 或文章不存在（更新影响行数为 0）时仍返回成功
     * @param articleId 文章ID
     * @return 操作结果
     */
    Result<Void> decrementFavoriteCount(Long articleId);
}