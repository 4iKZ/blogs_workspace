package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.common.ResultCode;
import com.blog.dto.ArticleDTO;
import com.blog.entity.Article;
import com.blog.entity.UserFollow;
import com.blog.exception.BusinessException;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.UserFavoriteMapper;
import com.blog.mapper.UserFollowMapper;
import com.blog.mapper.UserLikeMapper;
import com.blog.service.ArticleQueryService;
import com.blog.service.ArticleRankService;
import com.blog.utils.AuthUtils;
import com.blog.utils.BusinessUtils;
import com.blog.utils.PageUtils;
import com.blog.utils.RedisCacheUtils;
import com.blog.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 文章查询服务实现类
 */
@Service
@Slf4j
public class ArticleQueryServiceImpl implements ArticleQueryService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private UserLikeMapper userLikeMapper;

    @Autowired
    private UserFavoriteMapper userFavoriteMapper;

    @Autowired
    private UserFollowMapper userFollowMapper;

    @Autowired
    private ArticleRankService articleRankService;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private RedisCacheUtils redisCacheUtils;

    @Autowired
    private ArticleDtoAssembler articleDtoAssembler;

    @Override
    public Result<PageResult<ArticleDTO>> getArticleList(Integer page, Integer size, String keyword,
            Long categoryId, Long tagId, Integer status, Long authorId, String sortBy) {
        // 公共列表始终仅展示已发布文章；草稿和下线文章由管理员接口查询。
        Integer effectiveStatus = Article.STATUS_PUBLISHED;
        log.info("获取文章列表，页码：{}，页大小：{}，关键词：{}，分类ID：{}，状态：{}，作者ID：{}，排序方式：{}", page, size, keyword, categoryId,
                effectiveStatus,
                authorId, sortBy);

        // 热门排序且无任何筛选条件时，直接复用排行榜 ZSet 分页查询：
        // 避免 SQL 按 viewCount 预排序截断候选集，导致周榜高分文章无法进入结果
        if ("popular".equals(sortBy) && categoryId == null && tagId == null
                && !StringUtils.hasText(keyword) && authorId == null) {
            return articleRankService.getHotArticlesPage(page, size, "week");
        }

        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = 10;
        }
        if (size > 100) {
            size = 100;
        }

        Page<Article> pageObj = PageUtils.createPage(page, size);
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();

        if (categoryId != null) {
            queryWrapper.eq(Article::getCategoryId, categoryId);
        }

        if (tagId != null) {
            queryWrapper.apply("id IN (SELECT article_id FROM article_tags WHERE tag_id = {0})", tagId);
        }

        queryWrapper.eq(Article::getStatus, effectiveStatus);

        if (authorId != null) {
            queryWrapper.eq(Article::getAuthorId, authorId);
        }

        queryWrapper.orderByDesc(Article::getIsTop);

        if ("popular".equals(sortBy)) {
            queryWrapper.orderByDesc(Article::getViewCount);
        } else {
            queryWrapper.orderByDesc(Article::getPublishTime);
        }

        IPage<Article> articlePage;
        if (StringUtils.hasText(keyword)) {
            articlePage = articleMapper.selectPublishedByFulltext(pageObj, effectiveStatus, keyword, categoryId, authorId, tagId);
        } else {
            articlePage = articleMapper.selectPage(pageObj, queryWrapper);
        }
        List<Article> articles = articlePage.getRecords();

        List<ArticleDTO> articleDTOs = articleDtoAssembler.batchConvertToDTO(articles);

        PageResult<ArticleDTO> pageResult = PageResult.of(
                articleDTOs,
                articlePage.getTotal(),
                page,
                size);

        return BusinessUtils.success(pageResult);
    }

    @Override
    public Result<ArticleDTO> getArticleDetail(Long articleId) {
        log.info("根据ID获取文章：{}", articleId);

        try {
            Article article = BusinessUtils.checkIdExist(articleId, articleMapper::selectById, ResultCode.ARTICLE_NOT_FOUND, "文章不存在");

            // 草稿状态（status=1）：仅作者或管理员可访问
            if (article.getStatus() == Article.STATUS_DRAFT) {
                Long currentUserId = AuthUtils.getCurrentUserIdOptional();
                boolean isAuthor = currentUserId != null && currentUserId.equals(article.getAuthorId());
                boolean isAdmin = AuthUtils.isAdmin();

                if (!isAuthor && !isAdmin) {
                    log.warn("权限拒绝：用户 {} 试图访问草稿文章 {}（作者：{}）",
                            currentUserId, articleId, article.getAuthorId());
                    return BusinessUtils.error("文章未发布或已删除");
                }
            }

            // 已发布文章（status=2）：公开访问
            // 其他状态（已下线/删除等）：仅作者或管理员可访问
            if (article.getStatus() != Article.STATUS_PUBLISHED) {
                Long currentUserId = AuthUtils.getCurrentUserIdOptional();
                boolean isAuthor = currentUserId != null && currentUserId.equals(article.getAuthorId());
                boolean isAdmin = AuthUtils.isAdmin();

                if (!isAuthor && !isAdmin) {
                    return BusinessUtils.error("文章未发布或已删除");
                }
            }

            ArticleDTO dto = articleDtoAssembler.convertToDTO(article);

            // 合并 Redis 中尚未同步到 DB 的浏览量增量
            int dbViewCount = article.getViewCount() != null ? article.getViewCount() : 0;
            int redisViewCount = redisCacheUtils.getArticleRedisViewCount(articleId);
            dto.setViewCount(dbViewCount + redisViewCount);

            return BusinessUtils.success(dto);
        } catch (RuntimeException e) {
            log.error("获取文章详情失败", e);
            return BusinessUtils.error(e.getMessage());
        }
    }

    @Override
    public Result<PageResult<ArticleDTO>> getUserArticles(Long userId, Integer page, Integer size) {
        log.info("获取用户文章列表：{}", userId);

        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Article::getAuthorId, userId);
        queryWrapper.orderByDesc(Article::getCreateTime);

        return getArticleListByQuery(queryWrapper, page, size);
    }

    @Override
    public Result<PageResult<ArticleDTO>> getUserLikedArticles(Long userId, Integer page, Integer size) {
        log.info("获取用户点赞文章列表：{}", userId);

        // 首先获取用户点赞的文章ID列表
        List<Long> articleIds = userLikeMapper.findArticleIdsByUserId(userId);

        if (articleIds.isEmpty()) {
            return BusinessUtils.success(PageResult.empty(page, size));
        }

        // 根据文章ID列表查询文章
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Article::getId, articleIds);
        queryWrapper.eq(Article::getStatus, 2); // 只获取已发布的文章
        queryWrapper.orderByDesc(Article::getPublishTime);

        return getArticleListByQuery(queryWrapper, page, size);
    }

    @Override
    public Result<PageResult<ArticleDTO>> getUserFavoriteArticles(Long userId, Integer page, Integer size) {
        log.info("获取用户收藏文章列表：{}", userId);

        // 首先获取用户收藏的文章ID列表
        List<Long> articleIds = userFavoriteMapper.findArticleIdsByUserId(userId);

        if (articleIds.isEmpty()) {
            return BusinessUtils.success(PageResult.empty(page, size));
        }

        // 根据文章ID列表查询文章
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Article::getId, articleIds);
        queryWrapper.eq(Article::getStatus, 2); // 只获取已发布的文章
        queryWrapper.orderByDesc(Article::getPublishTime);

        return getArticleListByQuery(queryWrapper, page, size);
    }

    /**
     * 根据查询条件获取文章列表
     */
    private Result<PageResult<ArticleDTO>> getArticleListByQuery(LambdaQueryWrapper<Article> queryWrapper, Integer page,
            Integer size) {
        // 添加分页逻辑
        if (page != null && size != null) {
            Page<Article> pageParam = PageUtils.createPage(page, size);
            IPage<Article> articlePage = articleMapper.selectPage(pageParam, queryWrapper);
            List<ArticleDTO> articleDTOs = articleDtoAssembler.batchConvertToDTO(articlePage.getRecords());
            PageResult<ArticleDTO> pageResult = PageResult.of(articleDTOs, articlePage.getTotal(), page, size);
            return BusinessUtils.success(pageResult);
        } else {
            List<Article> articles = articleMapper.selectList(queryWrapper);
            List<ArticleDTO> articleDTOs = articleDtoAssembler.batchConvertToDTO(articles);
            // For non-paginated queries, treat all results as a single page
            PageResult<ArticleDTO> pageResult = PageResult.of(articleDTOs, (long) articleDTOs.size(), 1,
                    articleDTOs.size());
            return BusinessUtils.success(pageResult);
        }
    }

    @Override
    public Result<PageResult<ArticleDTO>> searchArticles(String keyword, Integer page, Integer size) {
        log.info("搜索文章：{}", keyword);

        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(w -> w.like(Article::getTitle, keyword)
                .or()
                .like(Article::getContent, keyword));
        queryWrapper.eq(Article::getStatus, 2); // 只搜索已发布的文章
        queryWrapper.orderByDesc(Article::getPublishTime);

        return getArticleListByQuery(queryWrapper, page, size);
    }

    @Override
    public Result<PageResult<ArticleDTO>> getArticlesByCategory(Long categoryId, Integer page, Integer size) {
        log.info("根据分类获取文章列表：{}", categoryId);

        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Article::getCategoryId, categoryId);
        queryWrapper.eq(Article::getStatus, 2); // 只获取已发布的文章
        queryWrapper.orderByDesc(Article::getPublishTime);

        return getArticleListByQuery(queryWrapper, page, size);
    }

    @Override
    public Result<PageResult<ArticleDTO>> getFollowingArticles(Integer page, Integer size) {
        log.info("获取关注作者的文章列表，页码：{}，页大小：{}", page, size);

        try {
            // 获取当前登录用户ID
            Long currentUserId = AuthUtils.getCurrentUserId();

            // 获取用户关注的作者ID列表
            LambdaQueryWrapper<UserFollow> followWrapper = new LambdaQueryWrapper<>();
            followWrapper.eq(UserFollow::getFollowerId, currentUserId);
            followWrapper.eq(UserFollow::getDeleted, 0);
            List<UserFollow> userFollows = userFollowMapper.selectList(followWrapper);

            if (userFollows.isEmpty()) {
                return BusinessUtils.success(PageResult.empty(page, size));
            }

            // 提取关注的作者ID
            List<Long> followedAuthorIds = userFollows.stream()
                    .map(UserFollow::getFollowingId)
                    .collect(Collectors.toList());

            // 查询关注作者发布的文章
            LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(Article::getAuthorId, followedAuthorIds);
            queryWrapper.eq(Article::getStatus, 2); // 只获取已发布的文章
            queryWrapper.orderByDesc(Article::getPublishTime);

            return getArticleListByQuery(queryWrapper, page, size);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取关注作者的文章列表失败", e);
            return BusinessUtils.error("获取关注作者的文章列表失败");
        }
    }

    @Override
    public Result<List<ArticleDTO>> getRecommendedArticles(Integer limit) {
        log.info("获取推荐文章，数量限制：{}", limit);

        // 尝试从Redis缓存获取
        String cacheKey = "recommended:articles:" + limit;
        log.info("尝试从Redis缓存获取推荐文章，缓存键：{}", cacheKey);
        List<ArticleDTO> recommendedArticles = null;

        try {
            recommendedArticles = redisUtils.get(cacheKey);
            if (recommendedArticles != null) {
                log.info("从Redis缓存获取推荐文章成功，数量：{}", recommendedArticles.size());
                return BusinessUtils.success(recommendedArticles);
            } else {
                log.info("Redis缓存未命中，将从数据库查询");
            }
        } catch (Exception e) {
            log.error("从Redis缓存获取推荐文章失败，将从数据库查询，错误信息：{}", e.getMessage());
        }

        // 缓存未命中，从数据库查询
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Article::getStatus, 2) // 只查询已发布的文章
                .eq(Article::getIsRecommended, 2) // 推荐文章
                .orderByDesc(Article::getPublishTime)
                .last("LIMIT " + limit);

        List<Article> articles = articleMapper.selectList(queryWrapper);
        recommendedArticles = articleDtoAssembler.batchConvertToDTO(articles);

        // 写入Redis缓存，设置1小时过期时间（仅缓存非空列表，避免空数组反序列化问题）
        if (!recommendedArticles.isEmpty()) {
            try {
                boolean setResult = redisUtils.set(cacheKey, recommendedArticles, 1, TimeUnit.HOURS);
                if (setResult) {
                    log.info("推荐文章写入Redis缓存成功，数量：{}", recommendedArticles.size());
                } else {
                    log.error("推荐文章写入Redis缓存失败");
                }
            } catch (Exception e) {
                log.error("推荐文章写入Redis缓存失败，错误信息：{}", e.getMessage());
            }
        } else {
            log.info("推荐文章列表为空，跳过缓存");
        }

        return BusinessUtils.success(recommendedArticles);
    }

    @Override
    public long countPublishedByAuthor(Long authorId) {
        return articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                .eq(Article::getAuthorId, authorId)
                .eq(Article::getStatus, Article.STATUS_PUBLISHED));
    }

}
