package com.blog.service;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.dto.ArticleDTO;

import java.util.List;

/**
 * 文章查询服务接口
 */
public interface ArticleQueryService {

    /**
     * 获取文章列表（分页）
     * @param page 页码
     * @param size 每页数量
     * @param keyword 搜索关键词
     * @param categoryId 分类ID
     * @param tagId 标签ID
     * @param status 文章状态
     * @param authorId 作者ID
     * @param sortBy 排序方式：popular-按热度，latest-按最新
     * @return 文章分页结果
     */
    Result<PageResult<ArticleDTO>> getArticleList(Integer page, Integer size, String keyword,
                                           Long categoryId, Long tagId, Integer status, Long authorId, String sortBy);

    /**
     * 获取文章详情
     * @param articleId 文章ID
     * @return 文章详情
     */
    Result<ArticleDTO> getArticleDetail(Long articleId);

    /**
     * 获取推荐文章
     * @param limit 数量限制
     * @return 推荐文章列表
     */
    Result<List<ArticleDTO>> getRecommendedArticles(Integer limit);

    /**
     * 获取用户的文章列表
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页数量
     * @return 文章分页结果
     */
    Result<PageResult<ArticleDTO>> getUserArticles(Long userId, Integer page, Integer size);

    /**
     * 获取用户点赞的文章列表
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页数量
     * @return 文章分页结果
     */
    Result<PageResult<ArticleDTO>> getUserLikedArticles(Long userId, Integer page, Integer size);

    /**
     * 获取用户收藏的文章列表
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页数量
     * @return 文章分页结果
     */
    Result<PageResult<ArticleDTO>> getUserFavoriteArticles(Long userId, Integer page, Integer size);

    /**
     * 统计作者已发布文章数
     * @param authorId 作者ID
     * @return 已发布文章数
     */
    long countPublishedByAuthor(Long authorId);

    /**
     * 搜索文章
     * @param keyword 搜索关键词
     * @param page 页码
     * @param size 每页数量
     * @return 文章分页结果
     */
    Result<PageResult<ArticleDTO>> searchArticles(String keyword, Integer page, Integer size);

    /**
     * 按分类获取文章列表
     * @param categoryId 分类ID
     * @param page 页码
     * @param size 每页数量
     * @return 文章分页结果
     */
    Result<PageResult<ArticleDTO>> getArticlesByCategory(Long categoryId, Integer page, Integer size);

    /**
     * 获取当前用户关注的作者发布的文章列表
     * @param page 页码
     * @param size 每页数量
     * @return 文章分页结果
     */
    Result<PageResult<ArticleDTO>> getFollowingArticles(Integer page, Integer size);
}
