package com.blog.service;

import com.blog.common.Result;
import com.blog.dto.ArticleDTO;

import java.util.List;

/**
 * 搜索服务接口
 */
public interface SearchService {

    /**
     * 关键词搜索文章
     */
    Result<List<ArticleDTO>> searchArticles(String keyword, Integer page, Integer size);

    /**
     * 按分类搜索文章
     */
    Result<List<ArticleDTO>> searchArticlesByCategory(Long categoryId, Integer page, Integer size);

    /**
     * 按标签搜索文章
     */
    Result<List<ArticleDTO>> searchArticlesByTag(Long tagId, Integer page, Integer size);

    /**
     * 按作者搜索文章
     */
    Result<List<ArticleDTO>> searchArticlesByAuthor(Long authorId, Integer page, Integer size);

    /**
     * 高级搜索
     */
    Result<List<ArticleDTO>> advancedSearch(String keyword, Long categoryId, Long tagId, Long authorId, 
                                            String startDate, String endDate, Integer page, Integer size);

    /**
     * 搜索建议
     */
    Result<List<String>> getSearchSuggestions(String keyword);

    /**
     * 热门搜索词
     */
    Result<List<String>> getHotSearchKeywords(Integer limit);
}