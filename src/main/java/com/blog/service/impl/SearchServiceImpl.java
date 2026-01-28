package com.blog.service.impl;

import com.blog.common.Result;
import com.blog.dto.ArticleDTO;
import com.blog.entity.Article;
import com.blog.mapper.ArticleMapper;
import com.blog.service.SearchService;
import com.blog.utils.BusinessUtils;
import com.blog.utils.PageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 搜索服务实现类
 */
@Service
@Slf4j
public class SearchServiceImpl implements SearchService {

    @Autowired
    private ArticleMapper articleMapper;

    @Override
    public Result<List<ArticleDTO>> searchArticles(String keyword, Integer page, Integer size) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return Result.error("搜索关键词不能为空");
            }

            List<Article> articles = articleMapper.searchByKeyword(keyword.trim(), (page - 1) * size, size);
            List<ArticleDTO> articleDTOList = articles.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            return Result.success(articleDTOList);
        } catch (Exception e) {
            log.error("搜索文章失败", e);
            return Result.error("搜索文章失败");
        }
    }

    @Override
    public Result<List<ArticleDTO>> searchArticlesByCategory(Long categoryId, Integer page, Integer size) {
        try {
            if (categoryId == null) {
                return Result.error("分类ID不能为空");
            }

            List<Article> articles = articleMapper.selectByCategoryId(categoryId, (page - 1) * size, size);
            List<ArticleDTO> articleDTOList = articles.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            return Result.success(articleDTOList);
        } catch (Exception e) {
            log.error("按分类搜索文章失败", e);
            return Result.error("按分类搜索文章失败");
        }
    }

    @Override
    public Result<List<ArticleDTO>> searchArticlesByTag(Long tagId, Integer page, Integer size) {
        try {
            if (tagId == null) {
                return Result.error("标签ID不能为空");
            }

            List<Article> articles = articleMapper.selectByTagId(tagId, (page - 1) * size, size);
            List<ArticleDTO> articleDTOList = articles.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            return Result.success(articleDTOList);
        } catch (Exception e) {
            log.error("按标签搜索文章失败", e);
            return Result.error("按标签搜索文章失败");
        }
    }

    @Override
    public Result<List<ArticleDTO>> searchArticlesByAuthor(Long authorId, Integer page, Integer size) {
        try {
            if (authorId == null) {
                return Result.error("作者ID不能为空");
            }

            List<Article> articles = articleMapper.selectByAuthorId(authorId, (page - 1) * size, size);
            List<ArticleDTO> articleDTOList = articles.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            return Result.success(articleDTOList);
        } catch (Exception e) {
            log.error("按作者搜索文章失败", e);
            return Result.error("按作者搜索文章失败");
        }
    }

    @Override
    public Result<List<ArticleDTO>> advancedSearch(String keyword, Long categoryId, Long tagId, Long authorId, 
                                                    String startDate, String endDate, Integer page, Integer size) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDateTime start = null;
            LocalDateTime end = null;
            
            if (startDate != null && !startDate.trim().isEmpty()) {
                start = LocalDate.parse(startDate, formatter).atStartOfDay();
            }
            
            if (endDate != null && !endDate.trim().isEmpty()) {
                end = LocalDate.parse(endDate, formatter).atTime(23, 59, 59);
            }

            List<Article> articles = articleMapper.advancedSearch(keyword, categoryId, tagId, authorId, 
                                                                 "all", "time", start, end, 
                                                                 (page - 1) * size, size);
            List<ArticleDTO> articleDTOList = articles.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            return Result.success(articleDTOList);
        } catch (Exception e) {
            log.error("高级搜索失败", e);
            return Result.error("高级搜索失败");
        }
    }

    @Override
    public Result<List<String>> getSearchSuggestions(String keyword) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return Result.success(List.of());
            }

            List<String> suggestions = articleMapper.getSearchSuggestions(keyword.trim());
            return Result.success(suggestions);
        } catch (Exception e) {
            log.error("获取搜索建议失败", e);
            return Result.error("获取搜索建议失败");
        }
    }

    @Override
    public Result<List<String>> getHotSearchKeywords(Integer limit) {
        try {
            if (limit == null || limit <= 0) {
                limit = 10;
            }

            List<String> hotKeywords = articleMapper.getHotSearchKeywords(limit);
            return Result.success(hotKeywords);
        } catch (Exception e) {
            log.error("获取热门搜索词失败", e);
            return Result.error("获取热门搜索词失败");
        }
    }

    private ArticleDTO convertToDTO(Article article) {
        ArticleDTO articleDTO = new ArticleDTO();
        BeanUtils.copyProperties(article, articleDTO);
        return articleDTO;
    }
}