package com.blog.service.impl;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.dto.UserFavoriteDTO;
import com.blog.entity.Article;
import com.blog.entity.UserFavorite;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.UserFavoriteMapper;
import com.blog.service.ArticleStatisticsService;
import com.blog.service.UserFavoriteService;
import com.blog.utils.AuthUtils;
import com.blog.utils.CacheUtils;
import com.blog.utils.RedisCacheUtils;
import com.blog.utils.RedisDistributedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户收藏服务实现类
 */
@Service
public class UserFavoriteServiceImpl implements UserFavoriteService {

    private static final Logger log = LoggerFactory.getLogger(UserFavoriteServiceImpl.class);

    @Autowired
    private UserFavoriteMapper userFavoriteMapper;

    @Autowired
    private ArticleStatisticsService articleStatisticsService;
    
    @Autowired
    private ArticleMapper articleMapper;

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
    public Result<Long> favoriteArticle(Long articleId) {
        Long userId = AuthUtils.getCurrentUserId();
        String lockKey = "article:favorite:" + articleId + ":" + userId;
        String lockValue = null;

        try {
            lockValue = redisDistributedLock.tryLock(lockKey, 10, TimeUnit.SECONDS);
            if (lockValue == null) {
                log.warn("获取文章收藏锁失败，用户ID：{}，文章ID：{}", userId, articleId);
                return Result.error("操作过于频繁，请稍后重试");
            }

            // 验证文章是否存在
            Article article = articleMapper.selectById(articleId);
            if (article == null) {
                log.warn("文章不存在，无法收藏，文章ID：{}", articleId);
                return Result.error("文章不存在");
            }
            
            // 验证文章是否已发布
            if (article.getStatus() != 2) {
                log.warn("文章未发布，无法收藏，文章ID：{}，状态：{}", articleId, article.getStatus());
                return Result.error("无法收藏未发布的文章");
            }
            
            // 校验是否已收藏，避免重复收藏
            int exists = userFavoriteMapper.countByUserIdAndArticleId(userId, articleId);
            if (exists > 0) {
                log.info("用户已收藏该文章，跳过重复操作，用户ID：{}，文章ID：{}", userId, articleId);
                // 返回已存在的收藏ID
                UserFavorite existing = userFavoriteMapper.selectByUserAndArticle(userId, articleId);
                return Result.success(existing != null ? existing.getId() : null);
            }

            UserFavorite userFavorite = new UserFavorite();
            userFavorite.setUserId(userId);
            userFavorite.setArticleId(articleId);

            userFavoriteMapper.insert(userFavorite);
            articleStatisticsService.incrementFavoriteCount(articleId);

            String favoriteCacheKey = RedisCacheUtils.generateArticleFavoriteKey(articleId, userId);
            cacheUtils.deleteCacheWithDoubleDelete(favoriteCacheKey);

            Long authorId = article.getAuthorId();
            String finalFavoriteCacheKey = favoriteCacheKey;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        articleRankService.incrementFavoriteScore(articleId, userId, authorId);
                        cacheUtils.deleteCacheAsync(finalFavoriteCacheKey);
                    } catch (Exception e) {
                        log.error("更新收藏热度分数失败", e);
                    }
                }
            });

            log.info("用户收藏文章成功，用户ID：{}，文章ID：{}", userId, articleId);
            return Result.success(userFavorite.getId());
        } catch (Exception e) {
            log.error("用户收藏文章失败，文章ID：{}", articleId, e);
            return Result.error("收藏文章失败");
        } finally {
            if (lockValue != null) {
                redisDistributedLock.unlock(lockKey, lockValue);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> unfavoriteArticle(Long articleId) {
        Long userId = AuthUtils.getCurrentUserId();
        String lockKey = "article:favorite:" + articleId + ":" + userId;
        String lockValue = null;

        try {
            lockValue = redisDistributedLock.tryLock(lockKey, 10, TimeUnit.SECONDS);
            if (lockValue == null) {
                log.warn("获取文章收藏锁失败，用户ID：{}，文章ID：{}", userId, articleId);
                return Result.error("操作过于频繁，请稍后重试");
            }

            // 需要先获取文章作者ID，用于判断是否排除自己收藏
            Article article = articleMapper.selectById(articleId);
            Long authorId = article != null ? article.getAuthorId() : null;

            int result = userFavoriteMapper.deleteByUserIdAndArticleId(userId, articleId);
            if (result > 0) {
                articleStatisticsService.decrementFavoriteCount(articleId);

                String favoriteCacheKey = RedisCacheUtils.generateArticleFavoriteKey(articleId, userId);
                cacheUtils.deleteCacheWithDoubleDelete(favoriteCacheKey);

                Long finalAuthorId = authorId;
                String finalFavoriteCacheKey = favoriteCacheKey;
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            articleRankService.decrementFavoriteScore(articleId, userId, finalAuthorId);
                            cacheUtils.deleteCacheAsync(finalFavoriteCacheKey);
                        } catch (Exception e) {
                            log.error("更新收藏热度分数失败", e);
                        }
                    }
                });

                log.info("用户取消收藏文章成功，用户ID：{}，文章ID：{}", userId, articleId);
                return Result.success();
            } else {
                return Result.error("未找到收藏记录");
            }
        } catch (Exception e) {
            log.error("用户取消收藏文章失败，文章ID：{}", articleId, e);
            return Result.error("取消收藏失败");
        } finally {
            if (lockValue != null) {
                redisDistributedLock.unlock(lockKey, lockValue);
            }
        }
    }

    @Override
    public Result<PageResult<UserFavoriteDTO>> getUserFavorites(Integer page, Integer size) {
        try {
            Long userId = AuthUtils.getCurrentUserId();
            int offset = (page - 1) * size;
            
            // 获取总数
            int total = userFavoriteMapper.countByUserId(userId);
            
            // 获取列表
            List<UserFavorite> favorites = userFavoriteMapper.selectByUserId(userId, offset, size);
            
            List<UserFavoriteDTO> favoriteDTOs = favorites.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            PageResult<UserFavoriteDTO> pageResult = new PageResult<>();
            pageResult.setItems(favoriteDTOs);
            pageResult.setTotal((long) total);
            
            log.info("获取用户收藏列表成功，用户ID：{}，总数：{}，当前数量：{}", userId, total, favoriteDTOs.size());
            return Result.success(pageResult);
        } catch (Exception e) {
            log.error("获取用户收藏列表失败", e);
            return Result.error("获取收藏列表失败");
        }
    }

    @Override
    public Result<Boolean> isArticleFavorited(Long articleId) {
        try {
            Long userId = AuthUtils.getCurrentUserId();
            
            String favoriteCacheKey = RedisCacheUtils.generateArticleFavoriteKey(articleId, userId);
            Boolean favorited = (Boolean) redisCacheUtils.getCache(favoriteCacheKey);
            
            if (favorited != null) {
                return Result.success(favorited);
            }
            
            int count = userFavoriteMapper.countByUserIdAndArticleId(userId, articleId);
            boolean isFavorited = count > 0;
            redisCacheUtils.setCache(favoriteCacheKey, isFavorited, 7, TimeUnit.DAYS);
            
            return Result.success(isFavorited);
        } catch (Exception e) {
            log.error("检查文章是否已收藏失败，文章ID：{}", articleId, e);
            return Result.error("检查收藏状态失败");
        }
    }

    @Override
    public Result<Integer> getUserFavoriteCount() {
        try {
            Long userId = AuthUtils.getCurrentUserId();
            int count = userFavoriteMapper.countByUserId(userId);
            return Result.success(count);
        } catch (Exception e) {
            log.error("获取用户收藏数量失败", e);
            return Result.error("获取收藏数量失败");
        }
    }

    /**
     * 转换实体为DTO
     */
    private UserFavoriteDTO convertToDTO(UserFavorite userFavorite) {
        UserFavoriteDTO dto = new UserFavoriteDTO();
        dto.setFavoriteId(userFavorite.getId());
        dto.setUserId(userFavorite.getUserId());
        dto.setArticleId(userFavorite.getArticleId());
        
        // 设置 createdAt 字段 - 使用ISO格式日期字符串
        if (userFavorite.getCreateTime() != null) {
            dto.setCreatedAt(userFavorite.getCreateTime().toString());
        }
        
        // 设置文章信息
        if (userFavorite.getArticleId() != null) {
            try {
                Article article = articleMapper.selectById(userFavorite.getArticleId());
                if (article != null) {
                    com.blog.dto.ArticleDTO articleDTO = new com.blog.dto.ArticleDTO();
                    BeanUtils.copyProperties(article, articleDTO);
                    dto.setArticle(articleDTO);
                }
            } catch (Exception e) {
                log.error("获取文章信息失败，文章ID：{}", userFavorite.getArticleId(), e);
            }
        }
        
        return dto;
    }
}