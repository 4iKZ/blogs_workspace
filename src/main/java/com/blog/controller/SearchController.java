package com.blog.controller;

import com.blog.common.Result;
import com.blog.dto.ArticleDTO;
import com.blog.service.SearchService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 搜索管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/search/legacy")
@Tag(name = "搜索管理接口")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @GetMapping("/keyword")
    @Operation(summary = "关键词搜索文章")
    public Result<List<ArticleDTO>> searchByKeyword(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size) {
        return searchService.searchArticles(keyword, page, size);
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "按分类搜索文章")
    public Result<List<ArticleDTO>> searchByCategory(
            @Parameter(description = "分类ID") @PathVariable Long categoryId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size) {
        return searchService.searchArticlesByCategory(categoryId, page, size);
    }

    @GetMapping("/tag/{tagId}")
    @Operation(summary = "按标签搜索文章")
    public Result<List<ArticleDTO>> searchByTag(
            @Parameter(description = "标签ID") @PathVariable Long tagId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size) {
        return searchService.searchArticlesByTag(tagId, page, size);
    }

    @GetMapping("/author/{authorId}")
    @Operation(summary = "按作者搜索文章")
    public Result<List<ArticleDTO>> searchByAuthor(
            @Parameter(description = "作者ID") @PathVariable Long authorId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size) {
        return searchService.searchArticlesByAuthor(authorId, page, size);
    }

    @GetMapping("/advanced")
    @Operation(summary = "高级搜索")
    public Result<List<ArticleDTO>> advancedSearch(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "标签ID列表") @RequestParam(required = false) List<Long> tagIds,
            @Parameter(description = "作者ID") @RequestParam(required = false) Long authorId,
            @Parameter(description = "开始时间") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endDate,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size) {
        Long tagId = tagIds != null && !tagIds.isEmpty() ? tagIds.get(0) : null;
        return searchService.advancedSearch(keyword, categoryId, tagId, authorId, startDate, endDate, page, size);
    }

    @GetMapping("/suggestion")
    @Operation(summary = "搜索建议")
    public Result<List<String>> getSearchSuggestions(@Parameter(description = "搜索关键词") @RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.success(java.util.Collections.emptyList());
        }
        return searchService.getSearchSuggestions(keyword.trim());
    }

    @GetMapping(value = "/hot-keywords", params = "source=legacy")
    @Operation(summary = "热门搜索词（旧版）")
    public Result<List<String>> getHotKeywordsLegacy() {
        return searchService.getHotSearchKeywords(10);
    }
}
