package com.blog.controller;

import com.blog.common.Result;
import com.blog.dto.SearchRequestDTO;
import com.blog.dto.SearchResultDTO;
import com.blog.dto.SearchStatisticsDTO;
import com.blog.service.ArticleSearchService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文章搜索控制器
 */
@RestController
@RequestMapping("/api/search")
@Tag(name = "文章搜索")
public class ArticleSearchController {

    private static final Logger log = LoggerFactory.getLogger(ArticleSearchController.class);

    @Autowired
    private ArticleSearchService articleSearchService;

    @PostMapping("/article")
    @Operation(summary = "搜索文章")
    public Result<List<SearchResultDTO>> searchArticles(
            @Parameter(description = "搜索请求参数") @Valid @RequestBody SearchRequestDTO searchRequestDTO) {
        log.info("搜索文章：关键词={}，分类ID={}，标签数量={}，搜索范围={}，排序方式={}",
                searchRequestDTO.getKeyword(),
                searchRequestDTO.getCategoryId(),
                searchRequestDTO.getTagIds() != null ? searchRequestDTO.getTagIds().length : 0,
                searchRequestDTO.getSearchScope(),
                searchRequestDTO.getSortBy());
        return articleSearchService.searchArticles(searchRequestDTO);
    }

    @GetMapping("/quick")
    @Operation(summary = "快速搜索")
    public Result<List<SearchResultDTO>> quickSearch(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("快速搜索：关键词={}，页码={}，每页数量={}", keyword, pageNum, pageSize);
        SearchRequestDTO searchRequestDTO = new SearchRequestDTO();
        searchRequestDTO.setKeyword(keyword);
        searchRequestDTO.setPageNum(pageNum);
        searchRequestDTO.setPageSize(pageSize);
        return articleSearchService.searchArticles(searchRequestDTO);
    }

    @GetMapping("/suggestions")
    @Operation(summary = "获取搜索建议")
    public Result<List<String>> getSearchSuggestions(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            @Parameter(description = "建议数量") @RequestParam(defaultValue = "10") Integer limit) {
        log.info("获取搜索建议：关键词={}，建议数量={}", keyword, limit);
        return articleSearchService.getSearchSuggestions(keyword, limit);
    }

    @GetMapping("/hot-keywords")
    @Operation(summary = "获取热门搜索关键词")
    public Result<List<String>> getHotKeywords(
            @Parameter(description = "返回数量") @RequestParam(defaultValue = "10") Integer limit) {
        log.info("获取热门搜索关键词：数量={}", limit);
        return articleSearchService.getHotKeywords(limit);
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取搜索统计信息")
    public Result<SearchStatisticsDTO> getSearchStatistics(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "标签ID列表") @RequestParam(required = false) List<Long> tagIds) {
        log.info("获取搜索统计信息：关键词={}，分类ID={}，标签数量={}", keyword, categoryId, tagIds != null ? tagIds.size() : 0);
        return articleSearchService.getSearchStatistics(keyword, categoryId, tagIds);
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "按分类搜索文章")
    public Result<List<SearchResultDTO>> searchByCategory(
            @Parameter(description = "分类ID") @PathVariable Long categoryId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "排序方式") @RequestParam(defaultValue = "time") String sortBy) {
        log.info("按分类搜索文章：分类ID={}，页码={}，每页数量={}，排序方式={}", categoryId, pageNum, pageSize, sortBy);
        SearchRequestDTO searchRequestDTO = new SearchRequestDTO();
        searchRequestDTO.setCategoryId(categoryId);
        searchRequestDTO.setPageNum(pageNum);
        searchRequestDTO.setPageSize(pageSize);
        searchRequestDTO.setSortBy(sortBy);
        return articleSearchService.searchArticles(searchRequestDTO);
    }

    @GetMapping("/tag/{tagId}")
    @Operation(summary = "按标签搜索文章")
    public Result<List<SearchResultDTO>> searchByTag(
            @Parameter(description = "标签ID") @PathVariable Long tagId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "排序方式") @RequestParam(defaultValue = "time") String sortBy) {
        log.info("按标签搜索文章：标签ID={}，页码={}，每页数量={}，排序方式={}", tagId, pageNum, pageSize, sortBy);
        SearchRequestDTO searchRequestDTO = new SearchRequestDTO();
        searchRequestDTO.setTagIds(new Long[]{tagId});
        searchRequestDTO.setPageNum(pageNum);
        searchRequestDTO.setPageSize(pageSize);
        searchRequestDTO.setSortBy(sortBy);
        return articleSearchService.searchArticles(searchRequestDTO);
    }

    @GetMapping("/author/{authorId}")
    @Operation(summary = "按作者搜索文章")
    public Result<List<SearchResultDTO>> searchByAuthor(
            @Parameter(description = "作者ID") @PathVariable Long authorId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "排序方式") @RequestParam(defaultValue = "time") String sortBy) {
        log.info("按作者搜索文章：作者ID={}，页码={}，每页数量={}，排序方式={}", authorId, pageNum, pageSize, sortBy);
        return articleSearchService.searchByAuthor(authorId, pageNum, pageSize, sortBy);
    }
}
