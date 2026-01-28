package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.Result;
import com.blog.dto.ArticleDTO;
import com.blog.dto.CommentDTO;
import com.blog.dto.UserDTO;
import com.blog.entity.Article;
import com.blog.entity.Comment;
import com.blog.entity.User;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.UserFollowMapper;
import com.blog.mapper.UserMapper;
import com.blog.service.AdminService;
import com.blog.utils.BusinessUtils;
import com.blog.utils.DTOConverter;
import com.blog.utils.PageUtils;
import com.blog.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 后台管理服务实现类
 */
@Service
@Slf4j
public class AdminServiceImpl implements AdminService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserFollowMapper userFollowMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private com.blog.service.ArticleService articleService;

    @Override
    public Result<List<UserDTO>> getUserList(Integer page, Integer size, String keyword, Integer status) {
        log.info("获取用户列表，页码：{}，页大小：{}，关键词：{}，状态：{}", page, size, keyword, status);

        Page<User> userPage = PageUtils.createPage(page, size);
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            queryWrapper.like(User::getUsername, keyword)
                    .or()
                    .like(User::getNickname, keyword)
                    .or()
                    .like(User::getEmail, keyword);
        }

        if (status != null) {
            queryWrapper.eq(User::getStatus, status);
        }

        queryWrapper.orderByDesc(User::getCreateTime);

        IPage<User> pageResult = userMapper.selectPage(userPage, queryWrapper);

        List<UserDTO> userDTOs = PageUtils.convertList(pageResult.getRecords(),
                user -> DTOConverter.convert(user, UserDTO.class));

        return BusinessUtils.success(userDTOs);
    }

    @Override
    public Result<Void> updateUserStatus(Long userId, Integer status) {
        log.info("修改用户状态，用户ID：{}，状态：{}", userId, status);

        try {
            User user = BusinessUtils.checkIdExist(userId, userMapper::selectById, "用户不存在");
            user.setStatus(status);
            BusinessUtils.setUpdateTime(user);
            int result = userMapper.updateById(user);
            if (result <= 0) {
                return BusinessUtils.error("修改用户状态失败");
            }
            return BusinessUtils.success();
        } catch (RuntimeException e) {
            log.error("修改用户状态失败", e);
            return BusinessUtils.error(e.getMessage());
        }
    }

    @Override
    public Result<Void> deleteUser(Long userId) {
        log.info("删除用户，用户ID：{}", userId);

        try {
            User user = BusinessUtils.checkIdExist(userId, userMapper::selectById, "用户不存在");

            // 在删除用户前，同步更新关注计数
            // 1. 找到所有关注该用户的人，减少他们的 following_count
            LambdaQueryWrapper<com.blog.entity.UserFollow> followersQuery = new LambdaQueryWrapper<>();
            followersQuery.eq(com.blog.entity.UserFollow::getFollowingId, userId);
            List<com.blog.entity.UserFollow> followers = userFollowMapper.selectList(followersQuery);
            if (!followers.isEmpty()) {
                for (com.blog.entity.UserFollow follow : followers) {
                    userMapper.decrementFollowingCount(follow.getFollowerId());
                }
                log.info("更新关注者计数：{} 用户的 following_count 已减少", followers.size());
            }

            // 2. 找到该用户关注的所有人，减少他们的 follower_count
            LambdaQueryWrapper<com.blog.entity.UserFollow> followingQuery = new LambdaQueryWrapper<>();
            followingQuery.eq(com.blog.entity.UserFollow::getFollowerId, userId);
            List<com.blog.entity.UserFollow> following = userFollowMapper.selectList(followingQuery);
            if (!following.isEmpty()) {
                for (com.blog.entity.UserFollow follow : following) {
                    userMapper.decrementFollowerCount(follow.getFollowingId());
                }
                log.info("更新被关注者计数：{} 用户的 follower_count 已减少", following.size());
            }

            // 3. 执行删除用户操作（CASCADE 会自动删除 user_follows 记录）
            int result = userMapper.deleteById(userId);
            if (result <= 0) {
                return BusinessUtils.error("删除用户失败");
            }

            log.info("删除用户成功，已同步更新关注计数");
            return BusinessUtils.success();
        } catch (RuntimeException e) {
            log.error("删除用户失败", e);
            return BusinessUtils.error(e.getMessage());
        }
    }

    @Override
    public Result<List<ArticleDTO>> getArticleList(Integer page, Integer size, String keyword, Integer status) {
        log.info("获取文章列表，页码：{}，页大小：{}，关键词：{}，状态：{}", page, size, keyword, status);

        Page<Article> articlePage = PageUtils.createPage(page, size);
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            queryWrapper.like(Article::getTitle, keyword)
                    .or()
                    .like(Article::getSummary, keyword);
        }

        if (status != null) {
            queryWrapper.eq(Article::getStatus, status);
        }

        queryWrapper.orderByDesc(Article::getCreateTime);

        IPage<Article> pageResult = articleMapper.selectPage(articlePage, queryWrapper);

        List<ArticleDTO> articleDTOs = PageUtils.convertList(pageResult.getRecords(), article -> {
            ArticleDTO articleDTO = DTOConverter.convert(article, ArticleDTO.class);
            // 处理null值
            articleDTO.setViewCount(article.getViewCount() != null ? article.getViewCount() : 0);
            articleDTO.setLikeCount(article.getLikeCount() != null ? article.getLikeCount() : 0);
            articleDTO.setCommentCount(article.getCommentCount() != null ? article.getCommentCount() : 0);
            articleDTO.setFavoriteCount(article.getFavoriteCount() != null ? article.getFavoriteCount() : 0);
            return articleDTO;
        });

        return BusinessUtils.success(articleDTOs);
    }

    @Override
    public Result<Void> updateArticleStatus(Long articleId, Integer status) {
        log.info("修改文章状态，文章ID：{}，状态：{}", articleId, status);

        try {
            Article article = BusinessUtils.checkIdExist(articleId, articleMapper::selectById, "文章不存在");
            article.setStatus(status);
            BusinessUtils.setUpdateTime(article);
            int result = articleMapper.updateById(article);
            if (result <= 0) {
                return BusinessUtils.error("修改文章状态失败");
            }
            return BusinessUtils.success();
        } catch (RuntimeException e) {
            log.error("修改文章状态失败", e);
            return BusinessUtils.error(e.getMessage());
        }
    }

    @Override
    public Result<Void> deleteArticle(Long articleId) {
        log.info("删除文章，文章ID：{}", articleId);

        // 管理员删除文章，直接操作数据库，不需要权限检查
        try {
            Article article = BusinessUtils.checkIdExist(articleId, articleMapper::selectById, "文章不存在");
            int result = articleMapper.deleteById(articleId);
            if (result <= 0) {
                return BusinessUtils.error("删除文章失败");
            }

            // 清除推荐文章缓存
            Set<String> recommendedArticleKeys = redisUtils.scanKeys("recommended:articles:*");
            if (recommendedArticleKeys != null && !recommendedArticleKeys.isEmpty()) {
                redisUtils.delete(recommendedArticleKeys);
            }

            return BusinessUtils.success();
        } catch (RuntimeException e) {
            log.error("删除文章失败", e);
            return BusinessUtils.error(e.getMessage());
        }
    }

    @Override
    public Result<List<CommentDTO>> getCommentList(Integer page, Integer size, String keyword, Integer status,
            Long articleId) {
        log.info("获取评论列表，页码：{}，页大小：{}，关键词：{}，状态：{}，文章ID：{}", page, size, keyword, status, articleId);

        Page<Comment> commentPage = PageUtils.createPage(page, size);
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            queryWrapper.like(Comment::getContent, keyword);
        }

        if (status != null) {
            queryWrapper.eq(Comment::getStatus, status);
        }

        if (articleId != null) {
            queryWrapper.eq(Comment::getArticleId, articleId);
        }

        queryWrapper.eq(Comment::getDeleted, 0)
                .orderByDesc(Comment::getCreateTime);

        IPage<Comment> pageResult = commentMapper.selectPage(commentPage, queryWrapper);

        List<CommentDTO> commentDTOs = PageUtils.convertList(pageResult.getRecords(), comment -> {
            CommentDTO commentDTO = DTOConverter.convert(comment, CommentDTO.class);
            // 处理null值
            commentDTO.setLikeCount(comment.getLikeCount() != null ? comment.getLikeCount() : 0);
            return commentDTO;
        });

        return BusinessUtils.success(commentDTOs);
    }

    @Override
    public Result<Map<String, Object>> getWebsiteStatistics() {
        log.info("获取网站统计信息");

        Map<String, Object> statistics = new HashMap<>();

        // 获取用户统计
        Long totalUsers = userMapper.selectCount(null);
        statistics.put("totalUsers", totalUsers);

        // 获取文章统计
        Long totalArticles = articleMapper.selectCount(null);
        statistics.put("totalArticles", totalArticles);

        // 获取已发布文章数
        Long publishedArticles = articleMapper.selectCount(
                new LambdaQueryWrapper<Article>().eq(Article::getStatus, 2));
        statistics.put("publishedArticles", publishedArticles);

        // 获取草稿文章数
        Long draftArticles = articleMapper.selectCount(
                new LambdaQueryWrapper<Article>().eq(Article::getStatus, 1));
        statistics.put("draftArticles", draftArticles);

        // 获取活跃用户数（最近30天有登录记录）
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        Long activeUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .isNotNull(User::getLastLoginTime)
                        .ge(User::getLastLoginTime, thirtyDaysAgo));
        statistics.put("activeUsers", activeUsers);

        return BusinessUtils.success(statistics);
    }

    @Override
    public Result<Map<String, Object>> getVisitStatistics(String startDate, String endDate) {
        log.info("获取访问统计，开始日期：{}，结束日期：{}", startDate, endDate);

        Map<String, Object> visitStatistics = new HashMap<>();

        // TODO: 实现访问统计逻辑

        return BusinessUtils.success(visitStatistics);
    }

    @Override
    public Result<Map<String, String>> getSystemConfig() {
        log.info("获取系统配置");

        Map<String, String> config = new HashMap<>();

        // TODO: 实现获取系统配置逻辑

        return BusinessUtils.success(config);
    }

    @Override
    public Result<Void> updateSystemConfig(Map<String, String> config) {
        log.info("更新系统配置");

        // TODO: 实现更新系统配置逻辑

        return BusinessUtils.success();
    }

    @Override
    public Result<String> backupDatabase() {
        log.info("备份数据库");

        // TODO: 实现数据库备份逻辑

        return BusinessUtils.success("备份成功");
    }

    @Override
    public Result<Void> clearCache() {
        log.info("清理Redis缓存");

        try {
            // 清除热门文章缓存
            long hotArticlesDeleted = 0;
            long recommendedArticlesDeleted = 0;
            long captchasDeleted = 0;

            // 清除热门文章缓存（排除 ZSet 排行榜数据）
            Set<String> hotArticlesKeys = redisUtils.scanKeys("hot:articles:*");
            if (hotArticlesKeys != null && !hotArticlesKeys.isEmpty()) {
                // 过滤掉 ZSet 排行榜键，只删除查询结果缓存
                Set<String> keysToDelete = hotArticlesKeys.stream()
                        .filter(key -> !key.startsWith("hot:articles:zset:day:")
                                && !key.startsWith("hot:articles:zset:week:"))
                        .collect(java.util.stream.Collectors.toSet());

                if (!keysToDelete.isEmpty()) {
                    hotArticlesDeleted = redisUtils.delete(keysToDelete);
                    log.info("成功清除热门文章缓存，数量：{}（已保留排行榜数据）", hotArticlesDeleted);
                }
            }

            // 清除推荐文章缓存
            Set<String> recommendedArticlesKeys = redisUtils.scanKeys("recommended:articles:*");
            if (recommendedArticlesKeys != null && !recommendedArticlesKeys.isEmpty()) {
                recommendedArticlesDeleted = redisUtils.delete(recommendedArticlesKeys);
                log.info("成功清除推荐文章缓存，数量：{}", recommendedArticlesDeleted);
            }

            // 清除验证码缓存
            Set<String> captchaKeys = redisUtils.scanKeys("captcha:*");
            if (captchaKeys != null && !captchaKeys.isEmpty()) {
                captchasDeleted = redisUtils.delete(captchaKeys);
                log.info("成功清除验证码缓存，数量：{}", captchasDeleted);
            }

            log.info("Redis缓存清理完成，共清除热门文章缓存{}个，推荐文章缓存{}个，验证码缓存{}个",
                    hotArticlesDeleted, recommendedArticlesDeleted, captchasDeleted);
            return BusinessUtils.success();
        } catch (Exception e) {
            log.error("清理Redis缓存失败", e);
            return BusinessUtils.error("清理缓存失败");
        }
    }
}
