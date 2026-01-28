package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.dto.ArticleCreateDTO;
import com.blog.dto.ArticleDTO;
import com.blog.dto.CategoryDTO;
import com.blog.entity.*;
import com.blog.exception.BusinessException;
import com.blog.event.ArticleLikeCountChangeEvent;
import com.blog.event.ArticleViewCountChangeEvent;
import com.blog.mapper.*;
import com.blog.service.ArticleService;
import com.blog.service.FileUploadService;
import com.blog.service.UserService;
import com.blog.utils.AuthUtils;
import com.blog.utils.BusinessUtils;
import com.blog.utils.DTOConverter;
import com.blog.utils.PageUtils;
import com.blog.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.stream.IntStream;
import com.blog.service.ArticleStatisticsService;

/**
 * 文章服务实现类
 */
@Service
@Slf4j
public class ArticleServiceImpl implements ArticleService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private UserLikeMapper userLikeMapper;

    @Autowired
    private UserFavoriteMapper userFavoriteMapper;

    @Autowired
    private UserFollowMapper userFollowMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private ArticleStatisticsService articleStatisticsService;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private com.blog.service.ArticleRankService articleRankService;

    @Override
    public Result<PageResult<ArticleDTO>> getArticleList(Integer page, Integer size, String keyword,
            Long categoryId, Long tagId, Integer status, Long authorId, String sortBy) {
        log.info("获取文章列表，页码：{}，页大小：{}，关键词：{}，分类ID：{}，状态：{}，作者ID：{}，排序方式：{}", page, size, keyword, categoryId, status,
                authorId, sortBy);

        // 分页参数边界验证
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = 10;
        }
        // 限制最大每页数量，防止内存溢出
        if (size > 100) {
            size = 100;
        }

        Page<Article> pageObj = PageUtils.createPage(page, size);
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();

        // 添加查询条件
        if (categoryId != null) {
            queryWrapper.eq(Article::getCategoryId, categoryId);
        }

        if (StringUtils.hasText(keyword)) {
            queryWrapper.like(Article::getTitle, keyword)
                    .or()
                    .like(Article::getSummary, keyword);
        }

        if (status != null) {
            queryWrapper.eq(Article::getStatus, status);
        }

        if (authorId != null) {
            queryWrapper.eq(Article::getAuthorId, authorId);
        }

        // 优化：当请求首页“热门/推荐”且没有其他过滤条件时，优先使用 Redis ZSet 排行榜
        if ("popular".equals(sortBy) && categoryId == null && tagId == null && !StringUtils.hasText(keyword)) {
            log.info("使用 Redis ZSet 进行热门文章分页查询");
            // 默认使用周榜
            return articleRankService.getHotArticlesPage(page, size, "week");
        }

        // 添加排序逻辑
        queryWrapper.orderByDesc(Article::getIsTop);

        // 根据sortBy参数调整排序方式
        if ("popular".equals(sortBy)) {
            // 按浏览量倒序排序（作为兜底逻辑，或者当存在过滤条件时使用）
            queryWrapper.orderByDesc(Article::getViewCount);
        } else {
            // 默认按发布时间倒序排序
            queryWrapper.orderByDesc(Article::getPublishTime);
        }

        IPage<Article> articlePage = articleMapper.selectPage(pageObj, queryWrapper);

        List<ArticleDTO> articleDTOs = this.batchConvertToDTO(articlePage.getRecords());

        PageResult<ArticleDTO> pageResult = PageResult.of(
                articleDTOs,
                articlePage.getTotal(),
                page,
                size
        );

        return BusinessUtils.success(pageResult);
    }

    @Override
    public Result<ArticleDTO> getArticleDetail(Long articleId) {
        log.info("根据ID获取文章：{}", articleId);

        try {
            Article article = BusinessUtils.checkIdExist(articleId, articleMapper::selectById, "文章不存在");

            // 检查文章状态：只有已发布的文章(2)可以被公开访问
            if (article.getStatus() != 2) {
                // 文章未发布或已删除，检查是否为作者或管理员
                Long currentUserId = AuthUtils.getCurrentUserId();
                boolean isAuthor = currentUserId != null && currentUserId.equals(article.getAuthorId());
                boolean isAdmin = AuthUtils.isAdmin();

                if (!isAuthor && !isAdmin) {
                    return BusinessUtils.error("文章未发布或已删除");
                }
            }

            // 增加浏览量
            article.setViewCount(article.getViewCount() + 1);
            articleMapper.updateById(article);

            // 同步更新 Redis ZSet 热度分数（排除作者自己浏览）
            // 改为同步执行，避免异步线程导致 Redis 连接池问题
            Long authorId = article.getAuthorId();
            Long currentUserId = AuthUtils.getCurrentUserId();
            try {
                articleRankService.incrementViewScore(articleId, currentUserId, authorId);
            } catch (Exception e) {
                log.error("更新浏览热度分数失败", e);
            }

            // 发布文章浏览量变化事件，触发缓存更新
            eventPublisher.publishEvent(new ArticleViewCountChangeEvent(this, articleId));

            return BusinessUtils.success(convertToDTO(article));
        } catch (RuntimeException e) {
            log.error("获取文章详情失败", e);
            return BusinessUtils.error(e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<Long> publishArticle(ArticleCreateDTO articleCreateDTO, Long authorId) {
        log.info("创建文章：{}", articleCreateDTO.getTitle());

        try {
            // 检查作者是否存在
            User author = userService.getUserById(authorId);
            if (author == null) {
                return BusinessUtils.error("作者不存在");
            }

            // 检查分类是否存在，如果未指定分类则使用默认分类"技术分享"(ID=11)
            Long categoryId = articleCreateDTO.getCategoryId();
            if (categoryId == null || categoryId == 0) {
                categoryId = 11L; // 默认分类：技术分享
                log.info("未指定分类，使用默认分类：技术分享(ID=11)");
            }
            Category category = BusinessUtils.checkIdExist(categoryId, categoryMapper::selectById,
                    "分类不存在");

            Article article = DTOConverter.convert(articleCreateDTO, Article.class);
            article.setAuthorId(authorId);
            article.setStatus(2); // 已发布状态
            article.setViewCount(0);
            article.setLikeCount(0);
            article.setCommentCount(0);
            article.setFavoriteCount(0);
            article.setCreateTime(LocalDateTime.now());
            article.setUpdateTime(LocalDateTime.now());
            article.setPublishTime(LocalDateTime.now()); // 设置发布时间

            int result = articleMapper.insert(article);
            if (result <= 0) {
                return BusinessUtils.error("创建文章失败");
            }

            // 清除推荐文章缓存，确保数据一致性
            Set<String> recommendedArticleKeys = redisUtils.scanKeys("recommended:articles:*");
            if (recommendedArticleKeys != null && !recommendedArticleKeys.isEmpty()) {
                redisUtils.delete(recommendedArticleKeys);
                log.info("成功清除推荐文章缓存，数量：{}", recommendedArticleKeys.size());
            }

            // 初始化文章到排行榜 ZSet
            articleRankService.initializeArticle(article.getId());

            return BusinessUtils.success(article.getId());
        } catch (RuntimeException e) {
            log.error("发布文章失败", e);
            return BusinessUtils.error(e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<Void> editArticle(Long articleId, ArticleCreateDTO articleCreateDTO, Long currentUserId) {
        log.info("更新文章：{}", articleId);

        try {
            Article article = BusinessUtils.checkIdExist(articleId, articleMapper::selectById, "文章不存在");

            // 权限检查：管理员或文章作者可以编辑
            if (!AuthUtils.canManageArticle(article.getAuthorId())) {
                return BusinessUtils.error("无权编辑此文章，只有文章作者或管理员可以编辑");
            }

            // 检查分类是否存在
            Category category = BusinessUtils.checkIdExist(articleCreateDTO.getCategoryId(), categoryMapper::selectById,
                    "分类不存在");

            BeanUtils.copyProperties(articleCreateDTO, article);
            BusinessUtils.setUpdateTime(article);

            int result = articleMapper.updateById(article);
            if (result <= 0) {
                return BusinessUtils.error("更新文章失败");
            }

            // 清除推荐文章缓存，确保数据一致性
            Set<String> recommendedArticleKeys = redisUtils.scanKeys("recommended:articles:*");
            if (recommendedArticleKeys != null && !recommendedArticleKeys.isEmpty()) {
                redisUtils.delete(recommendedArticleKeys);
                log.info("成功清除推荐文章缓存，数量：{}", recommendedArticleKeys.size());
            }

            return BusinessUtils.success();
        } catch (RuntimeException e) {
            log.error("编辑文章失败", e);
            return BusinessUtils.error(e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<Void> deleteArticle(Long articleId, Long currentUserId) {
        log.info("删除文章：{}", articleId);

        try {
            Article article = BusinessUtils.checkIdExist(articleId, articleMapper::selectById, "文章不存在");

            // 权限检查：管理员或文章作者可以删除
            if (!AuthUtils.canManageArticle(article.getAuthorId())) {
                return BusinessUtils.error("无权删除此文章，只有文章作者或管理员可以删除");
            }

            int result = articleMapper.deleteById(articleId);
            if (result <= 0) {
                return BusinessUtils.error("删除文章失败");
            }

            try {
                int likeCleaned = userLikeMapper.deleteByArticleId(articleId);
                int favoriteCleaned = userFavoriteMapper.deleteByArticleId(articleId);
                log.info("删除文章后清理关联数据：likes={}, favorites={}", likeCleaned, favoriteCleaned);
            } catch (Exception e) {
                log.warn("清理文章关联的点赞/收藏失败，文章ID：{}，错误：{}", articleId, e.getMessage());
            }

            // 从排行榜 ZSet 中删除该文章
            try {
                articleRankService.removeFromRank(articleId);
                log.info("已从排行榜 ZSet 中删除文章，文章ID：{}", articleId);
            } catch (Exception e) {
                log.warn("从排行榜 ZSet 删除文章失败，文章ID：{}，错误：{}", articleId, e.getMessage());
            }

            // 清除推荐文章缓存，确保数据一致性
            Set<String> recommendedArticleKeys = redisUtils.scanKeys("recommended:articles:*");
            if (recommendedArticleKeys != null && !recommendedArticleKeys.isEmpty()) {
                redisUtils.delete(recommendedArticleKeys);
                log.info("成功清除推荐文章缓存，数量：{}", recommendedArticleKeys.size());
            }

            return BusinessUtils.success();
        } catch (RuntimeException e) {
            log.error("删除文章失败", e);
            return BusinessUtils.error(e.getMessage());
        }
    }

    @Override
    public Result<Void> publishArticle(Long articleId) {
        log.info("发布文章：{}", articleId);

        try {
            Article article = BusinessUtils.checkIdExist(articleId, articleMapper::selectById, "文章不存在");

            article.setStatus(2); // 已发布
            article.setPublishTime(LocalDateTime.now());
            BusinessUtils.setUpdateTime(article);

            int result = articleMapper.updateById(article);
            if (result <= 0) {
                return BusinessUtils.error("发布文章失败");
            }

            // 清除推荐文章缓存，确保数据一致性
            Set<String> recommendedArticleKeys = redisUtils.scanKeys("recommended:articles:*");
            if (recommendedArticleKeys != null && !recommendedArticleKeys.isEmpty()) {
                redisUtils.delete(recommendedArticleKeys);
                log.info("成功清除推荐文章缓存，数量：{}", recommendedArticleKeys.size());
            }

            return BusinessUtils.success();
        } catch (RuntimeException e) {
            log.error("发布文章失败", e);
            return BusinessUtils.error(e.getMessage());
        }
    }

    // Like/Unlike functionality moved to UserLikeService for proper user-article
    // relationship tracking. Use UserLikeService.likeArticle() and UserLikeService.unlikeArticle() instead.

    // Favorite functionality moved to UserFavoriteService for proper user-article
    // relationship tracking

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
            List<ArticleDTO> articleDTOs = this.batchConvertToDTO(articlePage.getRecords());
            PageResult<ArticleDTO> pageResult = PageResult.of(articleDTOs, articlePage.getTotal(), page, size);
            return BusinessUtils.success(pageResult);
        } else {
            List<Article> articles = articleMapper.selectList(queryWrapper);
            List<ArticleDTO> articleDTOs = this.batchConvertToDTO(articles);
            // For non-paginated queries, treat all results as a single page
            PageResult<ArticleDTO> pageResult = PageResult.of(articleDTOs, (long) articleDTOs.size(), 1, articleDTOs.size());
            return BusinessUtils.success(pageResult);
        }
    }

    @Override
    public Result<String> uploadCoverImage(MultipartFile file) {
        log.info("上传封面图片");

        try {
            // 调用文件上传服务上传图片
            Result<String> result = fileUploadService.uploadImage(file);
            return result;
        } catch (Exception e) {
            log.error("封面图片上传失败", e);
            return BusinessUtils.error("封面图片上传失败: " + e.getMessage());
        }
    }

    @Override
    public Result<PageResult<ArticleDTO>> searchArticles(String keyword, Integer page, Integer size) {
        log.info("搜索文章：{}", keyword);

        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Article::getTitle, keyword)
                .or()
                .like(Article::getContent, keyword);
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
    public void updateArticleViewCount(Long articleId) {
        log.info("更新文章浏览量，文章ID：{}", articleId);

        Article article = articleMapper.selectById(articleId);
        if (article != null) {
            article.setViewCount(article.getViewCount() + 1);
            articleMapper.updateById(article);
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
        recommendedArticles = this.batchConvertToDTO(articles);

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

    private ArticleDTO convertToDTO(Article article) {
        List<ArticleDTO> list = batchConvertToDTO(Collections.singletonList(article));
        return list.isEmpty() ? new ArticleDTO() : list.get(0);
    }

    /**
     * 批量转换文章为DTO（优化N+1查询）
     * @param articles 文章列表
     * @return ArticleDTO列表
     */
    List<ArticleDTO> batchConvertToDTO(List<Article> articles) {
        if (articles == null || articles.isEmpty()) {
            return Collections.emptyList();
        }

        long startTime = System.currentTimeMillis();
        log.info("开始批量转换文章DTO，数量：{}", articles.size());

        // 1. 收集所有需要的ID
        Set<Long> authorIds = new HashSet<>();
        Set<Long> categoryIds = new HashSet<>();
        List<Long> articleIds = new ArrayList<>();

        for (Article article : articles) {
            if (article.getAuthorId() != null) {
                authorIds.add(article.getAuthorId());
            }
            if (article.getCategoryId() != null) {
                categoryIds.add(article.getCategoryId());
            }
            articleIds.add(article.getId());
        }

        // 2. 批量查询用户和分类，转为Map以便快速查找
        Map<Long, User> userMap = Collections.emptyMap();
        Map<Long, Category> categoryMap = Collections.emptyMap();
        Set<Long> likedArticleIds = Collections.emptySet();
        Set<Long> favoritedArticleIds = Collections.emptySet();

        // 批量查询作者
        if (!authorIds.isEmpty()) {
            try {
                List<User> users = userMapper.selectBatchIds(authorIds);
                userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));
            } catch (Exception e) {
                log.error("批量查询用户失败", e);
            }
        }

        // 批量查询分类
        if (!categoryIds.isEmpty()) {
            try {
                List<Category> categories = categoryMapper.selectBatchIds(categoryIds);
                categoryMap = categories.stream().collect(Collectors.toMap(Category::getId, c -> c));
            } catch (Exception e) {
                log.error("批量查询分类失败", e);
            }
        }

        // 3. 批量查询当前用户的互动状态（仅当用户登录时）
        try {
            Long currentUserId = AuthUtils.getCurrentUserId();
            if (currentUserId != null) {
                // 批量查询点赞状态
                likedArticleIds = new HashSet<>(
                    userLikeMapper.findLikedArticleIdsByUserIdAndArticleIds(currentUserId, articleIds)
                );
                // 批量查询收藏状态
                favoritedArticleIds = new HashSet<>(
                    userFavoriteMapper.findFavoritedArticleIdsByUserIdAndArticleIds(currentUserId, articleIds)
                );
            }
        } catch (Exception e) {
            log.debug("未登录或无法获取用户ID，跳过点赞/收藏状态查询");
        }

        // 4. 组装DTO
        List<ArticleDTO> result = new ArrayList<>(articles.size());
        for (Article article : articles) {
            ArticleDTO dto = new ArticleDTO();
            BeanUtils.copyProperties(article, dto);

            // 设置分类信息
            Category category = categoryMap.get(article.getCategoryId());
            if (category != null) {
                CategoryDTO categoryDTO = new CategoryDTO();
                BeanUtils.copyProperties(category, categoryDTO);
                dto.setCategory(categoryDTO);
                dto.setCategoryName(category.getName());
            }

            // 设置作者信息
            User author = userMap.get(article.getAuthorId());
            if (author != null) {
                dto.setAuthorNickname(author.getNickname());
                dto.setAuthorAvatar(author.getAvatar());
            }

            // 设置互动状态
            dto.setLiked(likedArticleIds.contains(article.getId()));
            dto.setFavorited(favoritedArticleIds.contains(article.getId()));

            result.add(dto);
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("批量转换文章DTO完成，数量：{}，耗时：{} ms", result.size(), duration);

        return result;
    }

}
