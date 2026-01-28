package com.blog.controller;

import com.blog.common.Result;
import com.blog.dto.*;
import com.blog.service.ArticleStatisticsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文章统计控制器
 */
@RestController
@RequestMapping("/api/statistics/article")
@Tag(name = "文章统计管理")
public class ArticleStatisticsController {

    @Autowired
    private ArticleStatisticsService articleStatisticsService;

    @GetMapping("/hot")
    @Operation(summary = "获取热门文章统计")
    public Result<List<ArticleStatisticsDTO>> getHotArticleStatistics(
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "10") Integer limit) {
        return articleStatisticsService.getHotArticleStatistics(limit);
    }

    @GetMapping("/top")
    @Operation(summary = "获取置顶文章统计")
    public Result<List<ArticleStatisticsDTO>> getTopArticleStatistics(
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "10") Integer limit) {
        return articleStatisticsService.getTopArticleStatistics(limit);
    }

    @GetMapping("/recommended")
    @Operation(summary = "获取推荐文章统计")
    public Result<List<ArticleStatisticsDTO>> getRecommendedArticleStatistics(
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "10") Integer limit) {
        return articleStatisticsService.getRecommendedArticleStatistics(limit);
    }

    @GetMapping("/{articleId}")
    @Operation(summary = "获取文章统计信息")
    public Result<ArticleStatisticsDTO> getArticleStatistics(
            @Parameter(description = "文章ID") @PathVariable Long articleId) {
        return articleStatisticsService.getArticleStatistics(articleId);
    }

    @PostMapping("/view/{articleId}")
    @Operation(summary = "增加文章浏览量")
    public Result<Void> incrementViewCount(
            @Parameter(description = "文章ID") @PathVariable Long articleId) {
        return articleStatisticsService.incrementViewCount(articleId);
    }

    @PostMapping("/like/{articleId}/increment")
    @Operation(summary = "增加文章点赞数")
    public Result<Void> incrementLikeCount(
            @Parameter(description = "文章ID") @PathVariable Long articleId) {
        return articleStatisticsService.incrementLikeCount(articleId);
    }

    @PostMapping("/like/{articleId}/decrement")
    @Operation(summary = "减少文章点赞数")
    public Result<Void> decrementLikeCount(
            @Parameter(description = "文章ID") @PathVariable Long articleId) {
        return articleStatisticsService.decrementLikeCount(articleId);
    }

    @PostMapping("/comment/{articleId}/increment")
    @Operation(summary = "增加文章评论数")
    public Result<Void> incrementCommentCount(
            @Parameter(description = "文章ID") @PathVariable Long articleId) {
        return articleStatisticsService.incrementCommentCount(articleId);
    }

    @PostMapping("/comment/{articleId}/decrement")
    @Operation(summary = "减少文章评论数")
    public Result<Void> decrementCommentCount(
            @Parameter(description = "文章ID") @PathVariable Long articleId) {
        return articleStatisticsService.decrementCommentCount(articleId);
    }

    @PostMapping("/favorite/{articleId}/increment")
    @Operation(summary = "增加文章收藏数")
    public Result<Void> incrementFavoriteCount(
            @Parameter(description = "文章ID") @PathVariable Long articleId) {
        return articleStatisticsService.incrementFavoriteCount(articleId);
    }

    @PostMapping("/favorite/{articleId}/decrement")
    @Operation(summary = "减少文章收藏数")
    public Result<Void> decrementFavoriteCount(
            @Parameter(description = "文章ID") @PathVariable Long articleId) {
        return articleStatisticsService.decrementFavoriteCount(articleId);
    }
}
