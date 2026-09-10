package com.blog.service;

import com.blog.common.Result;
import com.blog.dto.ArticleCreateDTO;
import org.springframework.web.multipart.MultipartFile;

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

    // Like/Unlike functionality moved to UserLikeService for proper user-article relationship tracking
    // Use UserLikeService.likeArticle() and UserLikeService.unlikeArticle() instead

    // Favorite functionality moved to UserFavoriteService for proper user-article relationship tracking

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
}
