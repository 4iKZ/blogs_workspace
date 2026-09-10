package com.blog.service.impl;

import com.blog.common.Result;
import com.blog.common.ResultCode;
import com.blog.dto.ArticleCreateDTO;
import com.blog.entity.*;
import com.blog.exception.BusinessException;

import com.blog.mapper.*;
import com.blog.service.ArticleService;
import com.blog.service.CommentService;
import com.blog.service.FileUploadService;
import com.blog.service.UserService;
import com.blog.utils.AuthUtils;
import com.blog.utils.BusinessUtils;
import com.blog.utils.DTOConverter;
import com.blog.utils.RedisCacheUtils;
import com.blog.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import com.blog.service.ArticleModerationSubmissionService;
import com.blog.service.ArticleStatisticsService;
import com.blog.service.SensitiveWordService;
import com.blog.event.ModerationEvent;

/**
 * 文章服务实现类
 */
@Service
@Slf4j
public class ArticleServiceImpl implements ArticleService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private UserLikeMapper userLikeMapper;

    @Autowired
    private UserFavoriteMapper userFavoriteMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private CommentLikeMapper commentLikeMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private ArticleStatisticsService articleStatisticsService;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private RedisCacheUtils redisCacheUtils;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private com.blog.service.ArticleRankService articleRankService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private com.blog.mapper.ArticleViewMapper articleViewMapper;

    @Autowired
    private com.blog.mapper.ArticleModerationSubmissionMapper moderationSubmissionMapper;

    @Autowired
    private SensitiveWordService sensitiveWordService;

    @Autowired
    private ArticleModerationSubmissionService moderationSubmissionService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Result<Long> publishArticle(ArticleCreateDTO articleCreateDTO, Long authorId) {
        log.info("创建文章：{}", articleCreateDTO.getTitle());

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
        BusinessUtils.checkIdExist(categoryId, categoryMapper::selectById, ResultCode.CATEGORY_NOT_FOUND, "分类不存在");

        // 敏感词检测（标题 + 内容 + 摘要）
        String textToCheck = articleCreateDTO.getTitle() + " " +
                articleCreateDTO.getContent() + " " +
                (articleCreateDTO.getSummary() != null ? articleCreateDTO.getSummary() : "");
        Result<Void> sensitiveResult = sensitiveWordService.validateContent(textToCheck);
        if (!sensitiveResult.isSuccess()) {
            return BusinessUtils.error(sensitiveResult.getMessage());
        }

        // 创建文章，先保存为草稿状态，等待AI审核结果
        Article article = DTOConverter.convert(articleCreateDTO, Article.class);
        article.setAuthorId(authorId);
        article.setStatus(1); // 草稿状态，等待AI审核
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setCommentCount(0);
        article.setFavoriteCount(0);
        article.setCreateTime(LocalDateTime.now());
        article.setUpdateTime(LocalDateTime.now());

        int result = articleMapper.insert(article);
        if (result <= 0) {
            return BusinessUtils.error("创建文章失败");
        }

        String submissionToken = moderationSubmissionService.submitNew(article);
        eventPublisher.publishEvent(new ModerationEvent(this, submissionToken));
        log.info("文章已保存为草稿，发布异步审核事件: articleId={}", article.getId());

        return Result.success("文章已提交审核，请等待AI审核结果", article.getId());
    }

    @Override
    @Transactional
    public Result<Void> editArticle(Long articleId, ArticleCreateDTO articleCreateDTO, Long currentUserId) {
        log.info("更新文章：{}", articleId);

        Article article = BusinessUtils.checkIdExist(articleId, articleMapper::selectById, ResultCode.ARTICLE_NOT_FOUND, "文章不存在");

        // 权限检查：管理员或文章作者可以编辑
        if (!AuthUtils.canManageArticle(article.getAuthorId())) {
            return BusinessUtils.error("无权编辑此文章，只有文章作者或管理员可以编辑");
        }

        // 检查分类是否存在
        BusinessUtils.checkIdExist(articleCreateDTO.getCategoryId(), categoryMapper::selectById, ResultCode.CATEGORY_NOT_FOUND, "分类不存在");

        // 敏感词检测（标题 + 内容 + 摘要）
        String textToCheck = articleCreateDTO.getTitle() + " " +
                articleCreateDTO.getContent() + " " +
                (articleCreateDTO.getSummary() != null ? articleCreateDTO.getSummary() : "");
        Result<Void> sensitiveResult = sensitiveWordService.validateContent(textToCheck);
        if (!sensitiveResult.isSuccess()) {
            return BusinessUtils.error(sensitiveResult.getMessage());
        }

        Article candidate = DTOConverter.convert(articleCreateDTO, Article.class);
        candidate.setTopicId(article.getTopicId());
        candidate.setAllowComment(articleCreateDTO.getAllowComment());
        if (article.getStatus() == Article.STATUS_PUBLISHED) {
            // Public rows remain untouched until the edit snapshot passes moderation.
            String submissionToken = moderationSubmissionService.submitEdit(article, candidate);
            eventPublisher.publishEvent(new ModerationEvent(this, submissionToken));
            return Result.success("文章已提交审核，请等待AI审核结果", null);
        }

        Integer originalStatus = article.getStatus();
        BeanUtils.copyProperties(articleCreateDTO, article);
        // 状态由服务端控制，禁止客户端通过编辑接口直接改状态（防止草稿绕过AI审核直接发布）
        article.setStatus(originalStatus);
        BusinessUtils.setUpdateTime(article);
        int result = articleMapper.updateById(article);
        if (result <= 0) return BusinessUtils.error("更新文章失败");

        // 清除推荐文章缓存，确保数据一致性
        Set<String> recommendedArticleKeys = redisUtils.scanKeys("recommended:articles:*");
        if (recommendedArticleKeys != null && !recommendedArticleKeys.isEmpty()) {
            redisUtils.delete(recommendedArticleKeys);
            log.info("成功清除推荐文章缓存，数量：{}", recommendedArticleKeys.size());
        }

        if (article.getStatus() == Article.STATUS_DRAFT) {
            String submissionToken = moderationSubmissionService.submitNew(article);
            eventPublisher.publishEvent(new ModerationEvent(this, submissionToken));
            log.info("文章已更新，发布异步审核事件: articleId={}", articleId);
            return Result.success("文章已提交审核，请等待AI审核结果", null);
        }

        return BusinessUtils.success();
    }

    @Override
    @Transactional
    public Result<Void> deleteArticle(Long articleId, Long currentUserId) {
        log.info("删除文章：{}", articleId);

        Article article = BusinessUtils.checkIdExist(articleId, articleMapper::selectById, ResultCode.ARTICLE_NOT_FOUND, "文章不存在");

        // 权限检查：管理员或文章作者可以删除
        if (!AuthUtils.canManageArticle(article.getAuthorId())) {
            return BusinessUtils.error("无权删除此文章，只有文章作者或管理员可以删除");
        }

        // 先清理引用本文章的子表数据（comments、user_favorites、article_views、
        // article_moderation_submissions 均存在外键引用 articles，必须先于文章删除）
        int likeCleaned = userLikeMapper.deleteByArticleId(articleId);
        int favoriteCleaned = userFavoriteMapper.deleteByArticleId(articleId);

        // 清理文章关联的评论及其点赞记录（评论数无需扣减：文章已删除）
        List<Comment> articleComments = commentMapper.selectCommentsByArticleId(articleId, null);
        for (Comment c : articleComments) {
            commentLikeMapper.deleteByCommentId(c.getId());
            commentMapper.deleteById(c.getId());
            redisCacheUtils.deleteCache(RedisCacheUtils.generateCommentDetailKey(c.getId()));
        }

        // 物理清理浏览记录与审核提交记录
        int viewCleaned = articleViewMapper.deleteByArticleId(articleId);
        int submissionCleaned = moderationSubmissionMapper.deleteByArticleId(articleId);
        log.info("删除文章前清理关联数据：likes={}, favorites={}, comments={}, views={}, moderationSubmissions={}",
                likeCleaned, favoriteCleaned, articleComments.size(), viewCleaned, submissionCleaned);

        int result = articleMapper.deleteById(articleId);
        if (result <= 0) {
            throw new BusinessException(ResultCode.ARTICLE_NOT_FOUND, "文章不存在");
        }

        // 清除文章评论列表/计数/热门缓存
        try {
            commentService.clearCommentCache(articleId);
        } catch (Exception e) {
            log.warn("清除文章评论缓存失败，文章ID：{}，错误：{}", articleId, e.getMessage());
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
    }

    // Like/Unlike functionality moved to UserLikeService for proper user-article
    // relationship tracking. Use UserLikeService.likeArticle() and
    // UserLikeService.unlikeArticle() instead.

    // Favorite functionality moved to UserFavoriteService for proper user-article
    // relationship tracking

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
    public void updateArticleViewCount(Long articleId) {
        log.info("更新文章浏览量，文章ID：{}", articleId);
        articleStatisticsService.incrementViewCount(articleId);
    }

}
