package com.blog.service;

import com.blog.common.ResultCode;
import com.blog.entity.Article;
import com.blog.exception.BusinessException;
import com.blog.mapper.ArticleMapper;
import com.blog.utils.BusinessUtils;
import com.blog.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 文章状态迁移唯一入口
 */
@Service
@Slf4j
public class ArticleStatusTransitionService {

    private final ArticleMapper articleMapper;
    private final ArticleRankService articleRankService;
    private final RedisUtils redisUtils;

    public ArticleStatusTransitionService(
            ArticleMapper articleMapper,
            ArticleRankService articleRankService,
            RedisUtils redisUtils) {
        this.articleMapper = articleMapper;
        this.articleRankService = articleRankService;
        this.redisUtils = redisUtils;
    }

    public void initializeDraft(Article article) {
        article.setStatus(Article.STATUS_DRAFT);
    }

    public void restoreStatus(Article article, Integer originalStatus) {
        article.setStatus(originalStatus);
    }

    public void publish(Article article) {
        article.setStatus(Article.STATUS_PUBLISHED);
        article.setPublishTime(LocalDateTime.now());
        if (articleMapper.updateById(article) != 1) {
            throw new BusinessException("应用审核快照失败");
        }
        articleRankService.initializeArticle(article.getId());
    }

    public void revertToDraft(Article article) {
        article.setStatus(Article.STATUS_DRAFT);
        articleMapper.updateById(article);
    }

    public void changeStatusByAdmin(Long articleId, Integer targetStatus) {
        if (Integer.valueOf(Article.STATUS_PUBLISHED).equals(targetStatus)) {
            throw new BusinessException("文章发布必须通过审核决定");
        }
        if (targetStatus == null
                || (targetStatus != Article.STATUS_DRAFT && targetStatus != Article.STATUS_DELETED)) {
            throw new BusinessException("无效的文章状态");
        }
        Article article = BusinessUtils.checkIdExist(articleId, articleMapper::selectById,
                ResultCode.ARTICLE_NOT_FOUND, "文章不存在");
        article.setStatus(targetStatus);
        BusinessUtils.setUpdateTime(article);
        if (articleMapper.updateById(article) <= 0) {
            throw new BusinessException("修改文章状态失败");
        }
        try {
            articleRankService.removeFromRank(articleId);
            log.info("文章状态变更为非发布（status={}），已从热度榜单移除，文章ID：{}", targetStatus, articleId);
        } catch (Exception rankEx) {
            log.error("从热度榜单移除文章失败，文章ID：{}，DB 状态已更新，榜单将在下次查询时自愈", articleId, rankEx);
        }
        clearRecommendedCache();
    }

    private void clearRecommendedCache() {
        try {
            Set<String> keys = redisUtils.scanKeys("recommended:articles:*");
            if (keys != null && !keys.isEmpty()) {
                redisUtils.delete(keys);
            }
        } catch (Exception e) {
            log.warn("清除推荐文章缓存失败，文章状态已变更", e);
        }
    }
}
