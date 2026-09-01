package com.blog.service;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.dto.ArticleDTO;

import java.util.List;

/**
 * 文章热度排行服务
 * 使用 Redis ZSet 实现实时热度计算
 */
public interface ArticleRankService {

    /**
     * 增加文章热度分数
     * @param articleId 文章ID
     * @param score 增加的分数
     */
    void incrementScore(Long articleId, double score);

    /**
     * 减少文章热度分数（取消点赞/删除评论时）
     * @param articleId 文章ID
     * @param score 减少的分数
     */
    void decrementScore(Long articleId, double score);

    /**
     * 获取热门文章列表（从 ZSet 获取）
     * @param limit 数量限制
     * @param period 时间范围：day-日榜，week-周榜
     * @return 文章列表
     */
    Result<List<ArticleDTO>> getHotArticles(Integer limit, String period);

    /**
     * 分页获取热门文章列表
     * @param page 页码
     * @param size 每页数量
     * @param period 时间范围：day 或 week
     * @return 分页结果
     */
    Result<PageResult<ArticleDTO>> getHotArticlesPage(Integer page, Integer size, String period);

    /**
     * 重置指定时间范围的排行榜（定时任务调用）
     * @param period 时间范围：day 或 week
     */
    void resetRank(String period);

    /**
     * 初始化文章到排行榜（文章发布时调用）
     * @param articleId 文章ID
     */
    void initializeArticle(Long articleId);

    // ==================== 便捷方法：按行为类型增减分数 ====================//

    /**
     * 浏览文章时调用（需排除作者自己）
     */
    void incrementViewScore(Long articleId, Long viewerId, Long authorId);

    /**
     * 点赞文章时调用（需排除作者自己）
     */
    void incrementLikeScore(Long articleId, Long likerId, Long authorId);

    /**
     * 取消点赞时调用（需排除作者自己）
     */
    void decrementLikeScore(Long articleId, Long likerId, Long authorId);

    /**
     * 评论文章时调用（需排除作者自己）
     */
    void incrementCommentScore(Long articleId, Long commenterId, Long authorId);

    /**
     * 删除评论时调用（需排除作者自己）
     */
    void decrementCommentScore(Long articleId, Long commenterId, Long authorId);

    /**
     * 收藏文章时调用（需排除作者自己）
     */
    void incrementFavoriteScore(Long articleId, Long favoriterId, Long authorId);

    /**
     * 取消收藏时调用（需排除作者自己）
     */
    void decrementFavoriteScore(Long articleId, Long favoriterId, Long authorId);

    /**
     * 初始化所有已发布的文章到排行榜（首次启动时调用）
     * 只初始化尚未在 ZSet 中的文章
     */
    void initializeAllArticles();

    /**
     * 从排行榜中删除文章（文章被删除时调用）
     * @param articleId 文章ID
     */
    void removeFromRank(Long articleId);
}
