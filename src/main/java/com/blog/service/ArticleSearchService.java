package com.blog.service;

import com.blog.common.Result;
import com.blog.dto.SearchRequestDTO;
import com.blog.dto.SearchResultDTO;
import com.blog.dto.SearchStatisticsDTO;

import java.util.List;

/**
 * 文章搜索服务接口
 */
public interface ArticleSearchService {

    /**
     * 搜索文章
     */
    Result<List<SearchResultDTO>> searchArticles(SearchRequestDTO searchRequestDTO);

    /**
     * 获取搜索建议
     */
    Result<List<String>> getSearchSuggestions(String keyword, Integer limit);

    /**
     * 获取热门搜索关键词
     */
    Result<List<String>> getHotKeywords(Integer limit);

    /**
     * 获取搜索统计信息
     */
    Result<SearchStatisticsDTO> getSearchStatistics(String keyword, Long categoryId, List<Long> tagIds);

    /**
     * 按作者搜索文章
     */
    Result<List<SearchResultDTO>> searchByAuthor(Long authorId, Integer pageNum, Integer pageSize, String sortBy);

    /**
     * 重建搜索索引
     */
    Result<Void> rebuildSearchIndex();

    /**
     * 添加文章到搜索索引
     */
    Result<Void> addArticleToIndex(Long articleId);

    /**
     * 从搜索索引中删除文章
     */
    Result<Void> removeArticleFromIndex(Long articleId);

    /**
     * 更新搜索索引中的文章
     */
    Result<Void> updateArticleInIndex(Long articleId);
}