package com.blog.controller;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.dto.ArticleCreateDTO;
import com.blog.dto.ArticleDTO;
import com.blog.exception.BusinessException;
import com.blog.common.ResultCode;
import com.blog.service.ArticleService;
import com.blog.service.ChunkedUploadService;
import com.blog.utils.AuthUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文章控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/article")
@Tag(name = "文章管理接口")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private com.blog.service.ArticleRankService articleRankService;

    @Autowired
    private ChunkedUploadService chunkedUploadService;

    @PostMapping("/publish")
    @Operation(summary = "发布文章")
    public Result<Long> publishArticle(@Valid @RequestBody ArticleCreateDTO articleCreateDTO) {
        log.info("发布文章：{}", articleCreateDTO.getTitle());
        
        // 获取当前用户ID
        Long currentUserId = AuthUtils.getCurrentUserId();

        return articleService.publishArticle(articleCreateDTO, currentUserId);
    }

    @PutMapping("/{articleId:[0-9]+}")
    @Operation(summary = "编辑文章")
    public Result<Void> editArticle(
            @Parameter(description = "文章ID") @PathVariable Long articleId,
            @Valid @RequestBody ArticleCreateDTO articleCreateDTO) {
        
        // 获取当前用户ID
        Long currentUserId = AuthUtils.getCurrentUserId();

        return articleService.editArticle(articleId, articleCreateDTO, currentUserId);
    }

    @DeleteMapping("/{articleId:[0-9]+}")
    @Operation(summary = "删除文章")
    public Result<Void> deleteArticle(@Parameter(description = "文章ID") @PathVariable Long articleId) {
        
        // 获取当前用户ID
        Long currentUserId = AuthUtils.getCurrentUserId();

        return articleService.deleteArticle(articleId, currentUserId);
    }

    @GetMapping("/list")
    @Operation(summary = "获取文章列表")
    public Result<PageResult<ArticleDTO>> getArticleList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "标签ID") @RequestParam(required = false) Long tagId,
            @Parameter(description = "文章状态：0-草稿，1-已发布") @RequestParam(required = false) Integer status,
            @Parameter(description = "作者ID") @RequestParam(required = false) Long authorId,
            @Parameter(description = "排序方式：popular-按热度，latest-按最新") @RequestParam(required = false, defaultValue = "latest") String sortBy) {
        return articleService.getArticleList(page, size, keyword, categoryId, tagId, status, authorId, sortBy);
    }

    @GetMapping("/{articleId:[0-9]+}")
    @Operation(summary = "获取文章详情")
    public Result<ArticleDTO> getArticleDetail(@Parameter(description = "文章ID") @PathVariable Long articleId) {
        return articleService.getArticleDetail(articleId);
    }

    @GetMapping("/hot")
    @Operation(summary = "获取热门文章（基于 Redis ZSet 热度分数）")
    public Result<List<ArticleDTO>> getHotArticles(
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "10") Integer limit,
            @Parameter(description = "时间范围：day-今日，week-本周") @RequestParam(defaultValue = "week") String type) {
        return articleRankService.getHotArticles(limit, type);
    }

    @GetMapping("/recommended")
    @Operation(summary = "获取推荐文章")
    public Result<List<ArticleDTO>> getRecommendedArticles(
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "10") Integer limit) {
        return articleService.getRecommendedArticles(limit);
    }

    // Like/Unlike functionality moved to UserLikeController for proper user-article relationship tracking
    // Use POST /api/user/like/{articleId} for liking articles
    // Use DELETE /api/user/like/{articleId} for unliking articles

    // Favorite functionality moved to UserFavoriteController for proper user-article relationship tracking

    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户的文章列表")
    public Result<PageResult<ArticleDTO>> getUserArticles(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size) {
        return articleService.getUserArticles(userId, page, size);
    }

    @GetMapping("/user/{userId}/liked")
    @Operation(summary = "获取用户点赞的文章列表")
    public Result<PageResult<ArticleDTO>> getUserLikedArticles(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size) {
        return articleService.getUserLikedArticles(userId, page, size);
    }

    @GetMapping("/user/{userId}/favorite")
    @Operation(summary = "获取用户收藏的文章列表")
    public Result<PageResult<ArticleDTO>> getUserFavoriteArticles(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size) {
        return articleService.getUserFavoriteArticles(userId, page, size);
    }

    @PostMapping("/upload-cover")
    @Operation(summary = "上传文章封面图片")
    public Result<String> uploadCoverImage(@Parameter(description = "图片文件") @RequestParam("file") MultipartFile file) {
        return articleService.uploadCoverImage(file);
    }

    @GetMapping("/search")
    @Operation(summary = "搜索文章")
    public Result<PageResult<ArticleDTO>> searchArticles(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size) {
        return articleService.searchArticles(keyword, page, size);
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "按分类获取文章列表")
    public Result<PageResult<ArticleDTO>> getArticlesByCategory(
            @Parameter(description = "分类ID") @PathVariable Long categoryId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size) {
        return articleService.getArticlesByCategory(categoryId, page, size);
    }



    @GetMapping("/following")
    @Operation(summary = "获取关注作者的文章列表")
    public Result<PageResult<ArticleDTO>> getFollowingArticles(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size) {
        return articleService.getFollowingArticles(page, size);
    }

    // ==================== 分片上传相关接口 ====================

    @PostMapping("/init-upload")
    @Operation(summary = "初始化分片上传会话")
    public Result<Map<String, Object>> initChunkedUpload(
            @Parameter(description = "上传ID") @RequestParam String uploadId,
            @Parameter(description = "文件名") @RequestParam String fileName,
            @Parameter(description = "文件大小") @RequestParam Long fileSize,
            @Parameter(description = "总分片数") @RequestParam Integer totalChunks,
            @Parameter(description = "文件哈希") @RequestParam(required = false) String fileHash) {
        log.info("初始化分片上传: uploadId={}, fileName={}, fileSize={}", uploadId, fileName, fileSize);
        String resultId = chunkedUploadService.initUpload(uploadId, fileName, fileSize, totalChunks, fileHash);
        Map<String, Object> result = new HashMap<>();
        result.put("uploadId", resultId);
        return Result.success(result);
    }

    @PostMapping("/upload-chunk")
    @Operation(summary = "上传单个分片")
    public Result<Map<String, Object>> uploadChunk(
            @Parameter(description = "分片文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "上传ID") @RequestParam String uploadId,
            @Parameter(description = "分片索引") @RequestParam Integer chunkIndex,
            @Parameter(description = "总分片数") @RequestParam Integer totalChunks,
            @Parameter(description = "文件名") @RequestParam String fileName,
            @Parameter(description = "文件大小") @RequestParam Long fileSize) {
        log.debug("上传分片: uploadId={}, chunkIndex={}", uploadId, chunkIndex);

        boolean success = chunkedUploadService.uploadChunk(uploadId, chunkIndex, file);
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("chunkIndex", chunkIndex);

        if (success) {
            ChunkedUploadService.ChunkedUploadStatus status = chunkedUploadService.getUploadStatus(uploadId);
            if (status != null) {
                result.put("uploadedChunks", status.getUploadedChunks());
                result.put("uploadedBytes", status.getUploadedBytes());
            }
        }

        return Result.success(result);
    }

    @PostMapping("/complete-upload")
    @Operation(summary = "完成分片上传，合并所有分片")
    public Result<Map<String, String>> completeChunkedUpload(
            @Parameter(description = "上传ID") @RequestParam String uploadId,
            @Parameter(description = "文件名") @RequestParam String fileName,
            @Parameter(description = "总分片数") @RequestParam Integer totalChunks) {
        log.info("完成分片上传: uploadId={}, fileName={}", uploadId, fileName);

        try {
            String fileUrl = chunkedUploadService.completeUpload(uploadId);
            Map<String, String> result = new HashMap<>();
            result.put("url", fileUrl);
            return Result.success(result);
        } catch (Exception e) {
            log.error("完成分片上传失败", e);
            return Result.error("完成上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/cancel-upload")
    @Operation(summary = "取消分片上传")
    public Result<Void> cancelChunkedUpload(
            @Parameter(description = "上传ID") @RequestParam String uploadId) {
        log.info("取消分片上传: uploadId={}", uploadId);
        boolean success = chunkedUploadService.cancelUpload(uploadId);
        return success ? Result.success() : Result.error("取消上传失败");
    }

    @GetMapping("/check-upload/{fileHash}")
    @Operation(summary = "检查是否有可恢复的上传")
    public Result<Map<String, String>> checkUpload(
            @Parameter(description = "文件哈希") @PathVariable String fileHash) {
        String uploadId = chunkedUploadService.checkResumeUpload(fileHash);
        Map<String, String> result = new HashMap<>();
        if (uploadId != null) {
            result.put("uploadId", uploadId);
            result.put("resumable", "true");
        } else {
            result.put("resumable", "false");
        }
        return Result.success(result);
    }

    @GetMapping("/upload-status/{uploadId}")
    @Operation(summary = "获取上传状态")
    public Result<ChunkedUploadService.ChunkedUploadStatus> getUploadStatus(
            @Parameter(description = "上传ID") @PathVariable String uploadId) {
        ChunkedUploadService.ChunkedUploadStatus status = chunkedUploadService.getUploadStatus(uploadId);
        if (status == null) {
            return Result.error("上传会话不存在");
        }
        return Result.success(status);
    }
}
