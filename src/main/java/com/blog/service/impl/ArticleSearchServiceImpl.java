package com.blog.service.impl;

import com.blog.common.Result;
import com.blog.dto.SearchRequestDTO;
import com.blog.dto.SearchResultDTO;
import com.blog.dto.SearchStatisticsDTO;
import com.blog.entity.Article;
import com.blog.mapper.ArticleMapper;
import com.blog.service.ArticleSearchService;
import com.blog.utils.RedisCacheUtils;
import com.blog.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文章搜索服务实现类
 */
@Service
@Slf4j
public class ArticleSearchServiceImpl implements ArticleSearchService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private RedisUtils redisUtils;

    @Override
    public Result<List<SearchResultDTO>> searchArticles(SearchRequestDTO searchRequestDTO) {
        log.info("搜索文章，关键词：{}", searchRequestDTO.getKeyword());
        try {
            log.info("进入搜索方法，尝试获取参数");

            // Convert DTO parameters to entity search parameters
            String keyword = searchRequestDTO.getKeyword();
            log.info("提取keyword成功: {}", keyword);

            Long categoryId = searchRequestDTO.getCategoryId();
            log.info("提取categoryId成功: {}", categoryId);

            Long[] tagIds = searchRequestDTO.getTagIds();
            log.info("提取tagIds成功，长度: {}", tagIds != null ? tagIds.length : "null");

            Long authorId = searchRequestDTO.getAuthorId();
            log.info("提取authorId成功: {}", authorId);

            String searchScope = searchRequestDTO.getSearchScope();
            log.info("提取searchScope成功: {}", searchScope);

            String sortBy = searchRequestDTO.getSortBy();
            log.info("提取sortBy成功: {}", sortBy);

            Integer pageNum = searchRequestDTO.getPageNum();
            Integer pageSize = searchRequestDTO.getPageSize();
            String startDateStr = searchRequestDTO.getStartDate();
            String endDateStr = searchRequestDTO.getEndDate();

            // Calculate offset for pagination
            int offset = (pageNum - 1) * pageSize;

            // Parse dates if provided
            LocalDateTime startDate = null;
            LocalDateTime endDate = null;
            if (startDateStr != null && !startDateStr.isEmpty()) {
                startDate = LocalDate.parse(startDateStr).atStartOfDay();
            }
            if (endDateStr != null && !endDateStr.isEmpty()) {
                endDate = LocalDate.parse(endDateStr).atTime(23, 59, 59);
            }

            log.info("执行搜索 - 关键词: {}, 分类ID: {}, 作者ID: {}, 标签数量: {}, 搜索范围: {}, 排序方式: {}, 开始日期: {}, 结束日期: {}, 偏移: {}, 大小: {}", 
                    keyword, categoryId, authorId, tagIds != null ? tagIds.length : 0, searchScope, sortBy, startDateStr, endDateStr, offset, pageSize);

            // Perform advanced search using all parameters
            List<Article> articles;
            Long tagId = (tagIds != null && tagIds.length > 0) ? tagIds[0] : null; // 使用第一个标签，后续可扩展为多标签
            articles = articleMapper.advancedSearch(
                keyword,
                categoryId,
                tagId,
                authorId,
                searchScope,
                sortBy,
                startDate,
                endDate,
                offset,
                pageSize
            );
            log.info("数据库查询完成，返回 {} 条记录", articles.size());

            // Convert articles to search results
            log.info("开始转换文章结果...");
            List<SearchResultDTO> searchResults = articles.stream()
                    .map(this::convertToSearchResult)
                    .collect(Collectors.toList());
            log.info("转换后的搜索结果数量: {}", searchResults.size());

            return Result.success(searchResults);
        } catch (Exception e) {
            log.error("搜索文章失败", e);
            e.printStackTrace(); // Print stack trace for debugging
            return Result.error("搜索文章失败: " + e.getMessage());
        }
    }

    @Override
    public Result<List<String>> getSearchSuggestions(String keyword, Integer limit) {
        log.info("关键词建议：{}", keyword);
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return Result.success(List.of());
            }

            if (limit == null || limit <= 0) {
                limit = 10;
            }

            List<String> suggestions = articleMapper.getSearchSuggestions(keyword.trim());
            return Result.success(suggestions);
        } catch (Exception e) {
            log.error("获取搜索建议失败", e);
            return Result.error("获取搜索建议失败");
        }
    }

    @Override
    public Result<List<String>> getHotKeywords(Integer limit) {
        log.info("获取热门关键词");
        try {
            if (limit == null || limit <= 0) {
                limit = 10;
            }

            List<String> hotKeywords = articleMapper.getHotSearchKeywords(limit);
            return Result.success(hotKeywords);
        } catch (Exception e) {
            log.error("获取热门关键词失败", e);
            return Result.error("获取热门关键词失败");
        }
    }

    @Override
    public Result<SearchStatisticsDTO> getSearchStatistics(String keyword, Long categoryId, List<Long> tagIds) {
        log.info("获取搜索统计");
        try {
            SearchStatisticsDTO stats = new SearchStatisticsDTO();
            // Calculate total results for the search
            List<Article> articles = articleMapper.advancedSearch(
                keyword,
                categoryId,
                tagIds != null && !tagIds.isEmpty() ? tagIds.get(0) : null,
                null, // authorId
                "all", // searchScope
                "time", // sortBy
                null, // startDate
                null, // endDate
                0, // offset
                Integer.MAX_VALUE // Use max to get all results for count
            );

            stats.setTotalResults((long) articles.size());
            stats.setKeyword(keyword);

            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取搜索统计失败", e);
            return Result.error("获取搜索统计失败");
        }
    }

    @Override
    public Result<List<SearchResultDTO>> searchByAuthor(Long authorId, Integer pageNum, Integer pageSize, String sortBy) {
        log.info("按作者搜索文章，作者ID：{}", authorId);
        try {
            if (authorId == null) {
                return Result.error("作者ID不能为空");
            }

            if (pageNum == null || pageNum < 1) {
                pageNum = 1;
            }
            if (pageSize == null || pageSize < 1) {
                pageSize = 10;
            }

            int offset = (pageNum - 1) * pageSize;
            List<Article> articles = articleMapper.selectByAuthorId(authorId, offset, pageSize);

            List<SearchResultDTO> searchResults = articles.stream()
                    .map(this::convertToSearchResult)
                    .collect(Collectors.toList());

            return Result.success(searchResults);
        } catch (Exception e) {
            log.error("按作者搜索文章失败", e);
            return Result.error("按作者搜索文章失败");
        }
    }

    @Override
    public Result<Void> rebuildSearchIndex() {
        log.info("重建搜索索引");
        // In a real implementation, this would rebuild the search index
        // For now, we'll just log the action
        return Result.success(null);
    }

    @Override
    public Result<Void> addArticleToIndex(Long articleId) {
        log.info("添加文章到搜索索引，文章ID：{}", articleId);
        // In a real implementation, this would add an article to the search index
        // For now, we'll just log the action
        return Result.success(null);
    }

    @Override
    public Result<Void> removeArticleFromIndex(Long articleId) {
        log.info("从搜索索引中删除文章，文章ID：{}", articleId);
        // In a real implementation, this would remove an article from the search index
        // For now, we'll just log the action
        return Result.success(null);
    }

    @Override
    public Result<Void> updateArticleInIndex(Long articleId) {
        log.info("更新搜索索引中的文章，文章ID：{}", articleId);
        // In a real implementation, this would update an article in the search index
        // For now, we'll just log the action
        return Result.success(null);
    }

    /**
     * Convert Article entity to SearchResultDTO
     */
    private SearchResultDTO convertToSearchResult(Article article) {
        SearchResultDTO result = new SearchResultDTO();
        // Manually map the properties since some fields might not match exactly
        result.setArticleId(article.getId());
        result.setTitle(article.getTitle());
        result.setSummary(article.getSummary());
        result.setContent(article.getContent());
        result.setCoverImage(article.getCoverImage());
        result.setAuthorId(article.getAuthorId());
        result.setCategoryId(article.getCategoryId());
        // 合并Redis浏览量
        int dbViewCount = article.getViewCount() != null ? article.getViewCount() : 0;
        int redisViewCount = getRedisViewCount(article.getId());
        result.setViewCount(dbViewCount + redisViewCount);
        result.setLikeCount(article.getLikeCount());
        result.setCommentCount(article.getCommentCount());
        result.setFavoriteCount(article.getFavoriteCount());

        // Format dates as strings
        if (article.getPublishTime() != null) {
            result.setPublishTime(article.getPublishTime().toString());
        } else if (article.getCreateTime() != null) {
            result.setPublishTime(article.getCreateTime().toString());
        }

        result.setRelevanceScore(1.0); // Default relevance score
        result.setMatchedField("title"); // Default matched field

        return result;
    }

    private int getRedisViewCount(Long articleId) {
        try {
            String viewCountKey = RedisCacheUtils.generateArticleViewCountKey(articleId);
            Object value = redisUtils.getObject(viewCountKey);
            if (value != null) {
                return Integer.parseInt(value.toString());
            }
        } catch (Exception e) {
            log.warn("获取Redis浏览量失败，文章ID: {}", articleId, e);
        }
        return 0;
    }
}