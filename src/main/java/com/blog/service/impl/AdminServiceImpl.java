package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.common.ResultCode;
import com.blog.dto.ArticleDTO;
import com.blog.dto.BackupInfoDTO;
import com.blog.dto.CommentDTO;
import com.blog.dto.UserDTO;
import com.blog.entity.Article;
import com.blog.entity.Comment;
import com.blog.entity.User;
import com.blog.entity.UserFollow;
import com.blog.entity.VisitStatistics;
import com.blog.exception.BusinessException;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.UserFavoriteMapper;
import com.blog.mapper.UserFollowMapper;
import com.blog.mapper.UserLikeMapper;
import com.blog.mapper.UserMapper;
import com.blog.mapper.VisitStatisticsMapper;
import com.blog.mapper.WebsiteAccessLogMapper;
import com.blog.service.AdminService;
import com.blog.service.AuthSessionRevocationService;
import com.blog.service.ArticleStatisticsService;
import com.blog.service.ArticleStatusTransitionService;
import com.blog.service.DataBackupService;
import com.blog.service.FollowCountService;
import com.blog.utils.AuthUtils;
import com.blog.utils.BusinessUtils;
import com.blog.utils.DTOConverter;
import com.blog.utils.HotArticleCacheEvictionService;
import com.blog.utils.PageUtils;
import com.blog.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

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

    private static final int ROLE_ADMIN = 2;
    private static final int ROLE_SUPER_ADMIN = 3;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserFollowMapper userFollowMapper;

    @Autowired
    private FollowCountService followCountService;

    @Autowired
    private UserLikeMapper userLikeMapper;

    @Autowired
    private UserFavoriteMapper userFavoriteMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private com.blog.service.ArticleService articleService;

    @Autowired
    private ArticleStatisticsService articleStatisticsService;

    @Autowired
    private HotArticleCacheEvictionService hotArticleCacheEvictionService;

    @Autowired
    private ArticleDtoAssembler articleDtoAssembler;

    @Autowired
    private VisitStatisticsMapper visitStatisticsMapper;

    @Autowired
    private WebsiteAccessLogMapper websiteAccessLogMapper;

    @Autowired
    private ArticleStatusTransitionService articleStatusTransition;

    @Autowired
    private AuthSessionRevocationService authSessionRevocationService;

    @Autowired
    private DataBackupService dataBackupService;

    @Override
    public Result<PageResult<UserDTO>> getUserList(Integer page, Integer size, String keyword, Integer status) {
        log.info("获取用户列表，页码：{}，页大小：{}，关键词：{}，状态：{}", page, size, keyword, status);

        Page<User> userPage = PageUtils.createPage(page, size);
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(w -> w.like(User::getUsername, keyword)
                    .or()
                    .like(User::getNickname, keyword)
                    .or()
                    .like(User::getEmail, keyword));
        }

        if (status != null) {
            queryWrapper.eq(User::getStatus, status);
        }

        queryWrapper.orderByDesc(User::getCreateTime);

        IPage<User> pageResult = userMapper.selectPage(userPage, queryWrapper);

        // 操作者只查一次，逐行计算能否管理，前端据此禁用不可操作的行
        Long operatorId = AuthUtils.getCurrentUserIdOptional();
        User operator = operatorId == null ? null : userMapper.selectById(operatorId);
        List<UserDTO> userDTOs = PageUtils.convertList(pageResult.getRecords(),
                user -> DTOConverter.convert(user, UserDTO.class, (source, target) -> {
                    target.setRole(toRoleName(source.getRole()));
                    target.setRoleLevel(source.getRole());
                    // 无法识别操作者时一律不可管理，不因缺少上下文放宽权限
                    target.setCanManage(operatorId != null
                            && !java.util.Objects.equals(operatorId, source.getId())
                            && manageDenied(operator, source) == null);
                }));

        PageResult<UserDTO> pageResultDTO = PageResult.of(userDTOs, pageResult.getTotal(), page, size);
        return BusinessUtils.success(pageResultDTO);
    }

    /**
     * 与 UserServiceImpl#convertToPublicDTO 保持一致：角色 >= 2 视为管理员
     */
    private static String toRoleName(Integer role) {
        return role != null && role >= ROLE_ADMIN ? "admin" : "user";
    }

    /**
     * 校验当前操作者能否管理目标账号：禁止操作自己、禁止操作已删除账号、
     * 禁止操作超级管理员；仅超级管理员可以操作管理员。
     */
    private User assertCanManageUser(Long targetUserId) {
        Long operatorId = AuthUtils.getCurrentUserId();
        if (java.util.Objects.equals(operatorId, targetUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "不能操作当前登录账号");
        }

        User target = BusinessUtils.checkIdExist(targetUserId, userMapper::selectById, ResultCode.USER_NOT_FOUND, "用户不存在");
        BusinessException denied = manageDenied(userMapper.selectById(operatorId), target);
        if (denied != null) {
            throw denied;
        }
        return target;
    }

    /**
     * 操作者是否有权管理目标账号（不含"是否为自己"的判断，由调用方处理）。
     * 返回拒绝原因，允许时返回 null。用户列表与守卫共用同一套规则，保证按钮状态与后端放行一致。
     */
    private BusinessException manageDenied(User operator, User target) {
        if (target.getStatus() != null && target.getStatus() == User.STATUS_DELETED) {
            return new BusinessException(ResultCode.BAD_REQUEST, "用户已删除");
        }
        Integer targetRole = target.getRole();
        if (targetRole != null && targetRole == ROLE_SUPER_ADMIN) {
            return new BusinessException(ResultCode.FORBIDDEN, "不能操作超级管理员");
        }
        if (targetRole != null && targetRole == ROLE_ADMIN
                && (operator == null || operator.getRole() == null || operator.getRole() != ROLE_SUPER_ADMIN)) {
            return new BusinessException(ResultCode.FORBIDDEN, "仅超级管理员可以操作管理员账号");
        }
        return null;
    }

    @Override
    @Transactional
    public Result<Void> updateUserStatus(Long userId, Integer status) {
        log.info("修改用户状态，用户ID：{}，状态：{}", userId, status);

        // 删除只能通过 deleteUser 完成，这里只允许正常/禁用两种状态
        if (status == null || (status != User.STATUS_ACTIVE && status != User.STATUS_DISABLED)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "无效的用户状态");
        }
        assertCanManageUser(userId);
        if (!authSessionRevocationService.updateStatusAndRevoke(userId, status)) {
            throw new BusinessException(ResultCode.ERROR, "修改用户状态失败");
        }
        return BusinessUtils.success();
    }

    /**
     * 软删除用户：状态置为已删除并吊销会话，文章与评论保留；关注关系物理清除并修正计数。
     */
    @Override
    @Transactional
    public Result<Void> deleteUser(Long userId) {
        log.info("删除用户（软删除），用户ID：{}", userId);

        assertCanManageUser(userId);
        if (!authSessionRevocationService.updateStatusAndRevoke(userId, User.STATUS_DELETED)) {
            throw new BusinessException(ResultCode.ERROR, "删除用户失败");
        }

        // 先修正受影响用户的关注计数（唯一入口 FollowCountService），再清除本人的关注关系，
        // 否则计数校正任务会把已删除用户的关注重新计入对方计数
        followCountService.detachUserRelations(userId);
        userFollowMapper.delete(new LambdaQueryWrapper<UserFollow>()
                .and(w -> w.eq(UserFollow::getFollowerId, userId)
                        .or()
                        .eq(UserFollow::getFollowingId, userId)));
        log.info("用户软删除完成，已修正关注计数并清除关注关系");
        return BusinessUtils.success();
    }

    @Override
    public Result<PageResult<ArticleDTO>> getArticleList(Integer page, Integer size, String keyword, Integer status,
            Long authorId) {
        log.info("获取文章列表，页码：{}，页大小：{}，关键词：{}，状态：{}，作者ID：{}", page, size, keyword, status, authorId);

        Page<Article> articlePage = PageUtils.createPage(page, size);
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(w -> w.like(Article::getTitle, keyword)
                    .or()
                    .like(Article::getSummary, keyword));
        }

        if (status != null) {
            queryWrapper.eq(Article::getStatus, status);
        }

        if (authorId != null) {
            queryWrapper.eq(Article::getAuthorId, authorId);
        }

        queryWrapper.orderByDesc(Article::getCreateTime);

        IPage<Article> pageResult = articleMapper.selectPage(articlePage, queryWrapper);

        List<Article> articles = pageResult.getRecords();
        List<ArticleDTO> articleDTOs = articleDtoAssembler.batchConvertToDTO(articles);

        PageResult<ArticleDTO> pageResultDTO = PageResult.of(articleDTOs, pageResult.getTotal(), page, size);
        return BusinessUtils.success(pageResultDTO);
    }

    @Override
    public Result<Void> updateArticleStatus(Long articleId, Integer status) {
        log.info("修改文章状态，文章ID：{}，状态：{}", articleId, status);

        // BusinessException 由 GlobalExceptionHandler 统一转换为响应，未知异常不向前端泄露内部信息
        articleStatusTransition.changeStatusByAdmin(articleId, status);
        return BusinessUtils.success();
    }

    @Override
    public Result<Void> deleteArticle(Long articleId) {
        log.info("管理员删除文章，文章ID：{}", articleId);

        // 管理员删除文章：委托给 ArticleService.deleteArticle（管理员无需权限检查即可通过其内部鉴权），
        // 复用同一套子表清理（评论/点赞/收藏/浏览/审核记录）与缓存清理逻辑，避免重复实现
        return articleService.deleteArticle(articleId, null);
    }

    @Override
    public Result<PageResult<CommentDTO>> getCommentList(Integer page, Integer size, String keyword,
            Long articleId) {
        log.info("获取评论列表，页码：{}，页大小：{}，关键词：{}，文章ID：{}", page, size, keyword, articleId);

        Page<Comment> commentPage = PageUtils.createPage(page, size);
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            queryWrapper.like(Comment::getContent, keyword);
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

        // 批量回填文章标题，避免逐条查询
        Set<Long> articleIds = commentDTOs.stream()
                .map(CommentDTO::getArticleId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        if (!articleIds.isEmpty()) {
            Map<Long, String> titleById = new HashMap<>();
            for (Article article : articleMapper.selectBatchIds(articleIds)) {
                titleById.put(article.getId(), article.getTitle());
            }
            commentDTOs.forEach(dto -> dto.setArticleTitle(titleById.get(dto.getArticleId())));
        }

        PageResult<CommentDTO> pageResultDTO = PageResult.of(commentDTOs, pageResult.getTotal(), page, size);
        return BusinessUtils.success(pageResultDTO);
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

        Map<String, Object> result = new HashMap<>();

        // 按日期范围查询聚合统计数据
        List<VisitStatistics> dailyList = visitStatisticsMapper.selectByDateRange(startDate, endDate);

        // 汇总指标
        long totalPv = dailyList.stream()
                .mapToLong(vs -> vs.getPageViews() != null ? vs.getPageViews() : 0)
                .sum();
        long totalUv = dailyList.stream()
                .mapToLong(vs -> vs.getUniqueVisitors() != null ? vs.getUniqueVisitors() : 0)
                .sum();
        long totalNewUsers = dailyList.stream()
                .mapToLong(vs -> vs.getNewUsers() != null ? vs.getNewUsers() : 0)
                .sum();
        long totalNewArticles = dailyList.stream()
                .mapToLong(vs -> vs.getNewArticles() != null ? vs.getNewArticles() : 0)
                .sum();
        long totalNewComments = dailyList.stream()
                .mapToLong(vs -> vs.getNewComments() != null ? vs.getNewComments() : 0)
                .sum();

        result.put("totalPageViews", totalPv);
        result.put("totalUniqueVisitors", totalUv);
        result.put("totalNewUsers", totalNewUsers);
        result.put("totalNewArticles", totalNewArticles);
        result.put("totalNewComments", totalNewComments);
        result.put("dailyList", dailyList);

        // 今日实时数据
        result.put("todayPageViews", websiteAccessLogMapper.countTodayPv());
        result.put("todayUniqueVisitors", websiteAccessLogMapper.countTodayUv());

        return BusinessUtils.success(result);
    }

    @Override
    public Result<String> backupDatabase() {
        log.info("触发数据库备份");
        // 复用已有的真实备份实现，避免管理端接口假成功
        Result<BackupInfoDTO> backupResult = dataBackupService.createDatabaseBackup(
                "admin_manual_backup", "管理端手动备份");
        if (!backupResult.isSuccess()) {
            return Result.error(backupResult.getMessage());
        }
        return Result.success("备份成功", backupResult.getData().getFileName());
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

            // 清除 Spring Cache 管理的热门文章结果缓存
            hotArticleCacheEvictionService.evictAll();
            log.info("成功清除 Spring Cache 热门文章结果缓存");

            log.info("Redis缓存清理完成，共清除热门文章缓存{}个，推荐文章缓存{}个，验证码缓存{}个",
                    hotArticlesDeleted, recommendedArticlesDeleted, captchasDeleted);
            return BusinessUtils.success();
        } catch (Exception e) {
            log.error("清理Redis缓存失败", e);
            return BusinessUtils.error("清理缓存失败");
        }
    }

}
