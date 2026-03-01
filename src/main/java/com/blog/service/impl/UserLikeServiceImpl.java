package com.blog.service.impl;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.dto.ArticleDTO;
import com.blog.dto.UserLikeDTO;
import com.blog.entity.Article;
import com.blog.entity.Notification;
import com.blog.entity.UserLike;
import com.blog.event.NotificationEvent;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.UserLikeMapper;
import com.blog.service.ArticleStatisticsService;
import com.blog.service.UserLikeService;
import com.blog.utils.AuthUtils;
import com.blog.utils.CacheUtils;
import com.blog.utils.RedisCacheUtils;
import com.blog.utils.RedisDistributedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户点赞服务实现类
 */
@Service
public class UserLikeServiceImpl implements UserLikeService {

    private static final Logger log = LoggerFactory.getLogger(UserLikeServiceImpl.class);

    @Autowired
    private UserLikeMapper userLikeMapper;

    @Autowired
    private ArticleStatisticsService articleStatisticsService;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private com.blog.service.ArticleRankService articleRankService;

    @Autowired
    private RedisDistributedLock redisDistributedLock;

    @Autowired
    private RedisCacheUtils redisCacheUtils;

    @Autowired
    private CacheUtils cacheUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> likeArticle(Long articleId) {
        Long userId = AuthUtils.getCurrentUserId();
        String lockKey = "article:like:" + articleId + ":" + userId;
        String lockValue = null;

        try {
            lockValue = redisDistributedLock.tryLock(lockKey, 10, TimeUnit.SECONDS);
            if (lockValue == null) {
                log.warn("获取文章点赞锁失败，用户ID：{}，文章ID：{}", userId, articleId);
                return Result.error("操作过于频繁，请稍后重试");
            }

            // 【P0-1 修复】先检查文章是否存在且可点赞（已发布状态）
            Article article = articleMapper.selectById(articleId);
            if (article == null) {
                log.warn("文章不存在，无法点赞，文章ID：{}，用户ID：{}", articleId, userId);
                return Result.error("文章不存在");
            }
            if (article.getStatus() != 2) {
                log.warn("文章未发布，无法点赞，文章ID：{}，状态：{}", articleId, article.getStatus());
                return Result.error("文章未发布，无法点赞");
            }

            // 检查是否已点赞，如果已点赞则返回现有记录ID（幂等性）
            UserLike existingLike = userLikeMapper.findByUserIdAndArticleId(userId, articleId);
            if (existingLike != null) {
                log.info("用户已点赞该文章，返回现有记录ID，用户ID：{}，文章ID：{}，点赞ID：{}", userId, articleId, existingLike.getId());
                return Result.success(existingLike.getId());
            }

            // 创建新点赞记录
            UserLike userLike = new UserLike();
            userLike.setUserId(userId);
            userLike.setArticleId(articleId);

            userLikeMapper.insert(userLike);
            articleStatisticsService.incrementLikeCount(articleId);

            String likeCacheKey = RedisCacheUtils.generateArticleLikeKey(articleId, userId);
            cacheUtils.deleteCacheWithDoubleDelete(likeCacheKey);

            Long finalAuthorId = article.getAuthorId();
            String finalLikeCacheKey = likeCacheKey;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        articleRankService.incrementLikeScore(articleId, userId, finalAuthorId);
                        cacheUtils.deleteCacheAsync(finalLikeCacheKey);
                    } catch (Exception e) {
                        log.error("更新点赞热度分数失败", e);
                    }
                }
            });

            // 发布异步事件：创建通知（不阻塞主流程）
            try {
                // 【优化】复用之前查询的 article 对象，避免重复查询
                if (article.getAuthorId() != null
                        && !Objects.equals(article.getAuthorId(), userId)) {
                    String content = "点赞了你的文章";
                    eventPublisher.publishEvent(new NotificationEvent(
                            this,
                            article.getAuthorId(),  // 接收者：文章作者
                            userId,                 // 发送者：当前用户
                            Notification.TYPE_ARTICLE_LIKE,
                            articleId,
                            Notification.TARGET_TYPE_ARTICLE,
                            content
                    ));
                    log.debug("已发布文章点赞通知事件: articleId={}, authorId={}, likerId={}",
                            articleId, article.getAuthorId(), userId);
                }
            } catch (Exception e) {
                // 事件发布失败不应影响主业务，仅记录日志
                log.error("发布文章点赞通知事件失败", e);
            }

            log.info("用户点赞文章成功，用户ID：{}，文章ID：{}", userId, articleId);
            return Result.success(userLike.getId());
        } catch (Exception e) {
            log.error("用户点赞文章失败，文章ID：{}，错误：{}", articleId, e.getMessage(), e);
            // 手动标记事务回滚，确保数据一致性
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.error("点赞文章失败");
        } finally {
            if (lockValue != null) {
                redisDistributedLock.unlock(lockKey, lockValue);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> unlikeArticle(Long articleId) {
        Long userId = AuthUtils.getCurrentUserId();
        String lockKey = "article:like:" + articleId + ":" + userId;
        String lockValue = null;

        try {
            lockValue = redisDistributedLock.tryLock(lockKey, 10, TimeUnit.SECONDS);
            if (lockValue == null) {
                log.warn("获取文章点赞锁失败，用户ID：{}，文章ID：{}", userId, articleId);
                return Result.error("操作过于频繁，请稍后重试");
            }

            // 需要先获取文章作者ID，用于判断是否排除自己点赞
            Article article = articleMapper.selectById(articleId);
            Long authorId = article != null ? article.getAuthorId() : null;

            int result = userLikeMapper.deleteByUserIdAndArticleId(userId, articleId);
            if (result > 0) {
                articleStatisticsService.decrementLikeCount(articleId);

                String likeCacheKey = RedisCacheUtils.generateArticleLikeKey(articleId, userId);
                cacheUtils.deleteCacheWithDoubleDelete(likeCacheKey);

                Long finalAuthorId = authorId;
                String finalLikeCacheKey = likeCacheKey;
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            articleRankService.decrementLikeScore(articleId, userId, finalAuthorId);
                            cacheUtils.deleteCacheAsync(finalLikeCacheKey);
                        } catch (Exception e) {
                            log.error("更新点赞热度分数失败", e);
                        }
                    }
                });

                log.info("用户取消点赞文章成功，用户ID：{}，文章ID：{}", userId, articleId);
            } else {
                // 【P0-2 修复】未找到点赞记录时也返回成功（幂等性）
                // 可能是重复调用、网络重试或跨端操作，最终状态一致即可
                log.info("未找到点赞记录，可能已取消，用户ID：{}，文章ID：{}", userId, articleId);
            }
            return Result.success();
        } catch (Exception e) {
            log.error("用户取消点赞文章失败，文章ID：{}，错误：{}", articleId, e.getMessage(), e);
            // 手动标记事务回滚，确保数据一致性
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.error("取消点赞失败");
        } finally {
            if (lockValue != null) {
                redisDistributedLock.unlock(lockKey, lockValue);
            }
        }
    }

    @Override
    public Result<PageResult<UserLikeDTO>> getUserLikes(Integer page, Integer size) {
        try {
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

            Long userId = AuthUtils.getCurrentUserId();
            int offset = (page - 1) * size;
            List<UserLike> likes = userLikeMapper.selectByUserId(userId, offset, size);

            // 获取总数
            Long total = userLikeMapper.countByUserId(userId);

            List<UserLikeDTO> likeDTOs = likes.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            PageResult<UserLikeDTO> pageResult = PageResult.of(likeDTOs, total, page, size);

            log.info("获取用户点赞列表成功，用户ID：{}，数量：{}", userId, likeDTOs.size());
            return Result.success(pageResult);
        } catch (Exception e) {
            log.error("获取用户点赞列表失败，错误：{}", e.getMessage());
            return Result.error("获取点赞列表失败");
        }
    }

    @Override
    public Result<Boolean> isArticleLiked(Long articleId) {
        try {
            Long userId = AuthUtils.getCurrentUserId();
            
            String likeCacheKey = RedisCacheUtils.generateArticleLikeKey(articleId, userId);
            Boolean liked = (Boolean) redisCacheUtils.getCache(likeCacheKey);
            
            if (liked != null) {
                return Result.success(liked);
            }
            
            int count = userLikeMapper.countByUserIdAndArticleId(userId, articleId);
            boolean isLiked = count > 0;
            redisCacheUtils.setCache(likeCacheKey, isLiked, 7, TimeUnit.DAYS);
            
            return Result.success(isLiked);
        } catch (Exception e) {
            log.error("检查文章是否已点赞失败，文章ID：{}，错误：{}", articleId, e.getMessage());
            return Result.error("检查点赞状态失败");
        }
    }

    @Override
    public Result<Integer> getUserLikeCount() {
        try {
            Long userId = AuthUtils.getCurrentUserId();
            Long count = userLikeMapper.countByUserId(userId);
            return Result.success(count.intValue());
        } catch (Exception e) {
            log.error("获取用户点赞数量失败，错误：{}", e.getMessage());
            return Result.error("获取点赞数量失败");
        }
    }

    /**
     * 转换实体为DTO
     */
    private UserLikeDTO convertToDTO(UserLike userLike) {
        UserLikeDTO dto = new UserLikeDTO();
        BeanUtils.copyProperties(userLike, dto);
        
        // 设置 createdAt 字段 - 使用ISO格式日期字符串而不是epoch秒数
        if (userLike.getCreateTime() != null) {
            dto.setCreatedAt(userLike.getCreateTime().toString());
        }
        
        // 设置文章信息
        if (userLike.getArticleId() != null) {
            try {
                Article article = articleMapper.selectById(userLike.getArticleId());
                if (article != null) {
                    ArticleDTO articleDTO = new ArticleDTO();
                    BeanUtils.copyProperties(article, articleDTO);
                    dto.setArticle(articleDTO);
                }
            } catch (Exception e) {
                log.error("获取文章信息失败，文章ID：{}", userLike.getArticleId(), e);
            }
        }
        
        return dto;
    }
}