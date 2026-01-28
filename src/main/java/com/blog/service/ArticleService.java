package com.blog.service;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.dto.ArticleCreateDTO;
import com.blog.dto.ArticleDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文章服务接口
 */
public interface ArticleService {

    /**
     * 发布文章
     * @param articleCreateDTO 文章信息
     * @param authorId 作者ID
     * @return 文章ID
     */
    Result<Long> publishArticle(ArticleCreateDTO articleCreateDTO, Long authorId);

    /**
     * 编辑文章
     * @param articleId 文章ID
     * @param articleCreateDTO 文章信息
     * @param currentUserId 当前用户ID(用于权限验证)
     * @return 是否成功
     */
    Result<Void> editArticle(Long articleId, ArticleCreateDTO articleCreateDTO, Long currentUserId);

    /**
     * 删除文章
     * @param articleId 文章ID
     * @param currentUserId 当前用户ID(用于权限验证)
     * @return 是否成功
     */
    Result<Void> deleteArticle(Long articleId, Long currentUserId);

    /**
     * 发布文章
     * @param articleId 文章ID
     * @return 是否成功
     */
    Result<Void> publishArticle(Long articleId);

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

    // Like/Unlike functionality moved to UserLikeService for proper user-article relationship tracking
    // Use UserLikeService.likeArticle() and UserLikeService.unlikeArticle() instead

    // Favorite functionality moved to UserFavoriteService for proper user-article relationship tracking

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
     * 上传文章封面图片
     * @param file 图片文件
     * @return 图片URL
     */
    Result<String> uploadCoverImage(MultipartFile file);

    /**
     * 更新文章浏览量
     * @param articleId 文章ID
     */
    void updateArticleViewCount(Long articleId);

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
