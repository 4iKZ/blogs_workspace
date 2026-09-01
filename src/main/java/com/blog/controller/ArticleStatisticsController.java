package com.blog.controller;

import com.blog.common.Result;
import com.blog.dto.ArticleStatisticsDTO;
import com.blog.service.ArticleStatisticsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 文章统计控制器
 */
@RestController
@RequestMapping("/api/statistics/article")
@Tag(name = "文章统计管理")
public class ArticleStatisticsController {

    @Autowired
    private ArticleStatisticsService articleStatisticsService;

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
}
