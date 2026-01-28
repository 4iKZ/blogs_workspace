package com.blog.service.impl;

import com.blog.common.Result;
import com.blog.dto.ArticleStatisticsDTO;
import com.blog.entity.Article;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.UserLikeMapper;
import com.blog.service.ArticleStatisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 文章统计服务实现类
 */
@Service
public class ArticleStatisticsServiceImpl implements ArticleStatisticsService {
    private static final Logger log = LoggerFactory.getLogger(ArticleStatisticsServiceImpl.class);

    @Autowired
    private ArticleMapper articleMapper;
    
    @Autowired
    private UserLikeMapper userLikeMapper;

    @Override
    public Result<ArticleStatisticsDTO> getArticleStatistics(Long articleId) {
        log.info("获取文章统计信息，文章ID: {}", articleId);
        
        try {
            Article article = articleMapper.selectById(articleId);
            if (article == null) {
                log.warn("文章不存在，文章ID: {}", articleId);
                return Result.error("文章不存在");
            }
            
            ArticleStatisticsDTO statistics = new ArticleStatisticsDTO();
            statistics.setArticleId(articleId);
            statistics.setViewCount(article.getViewCount());
            statistics.setLikeCount(article.getLikeCount());
            statistics.setCommentCount(article.getCommentCount());
            statistics.setFavoriteCount(article.getFavoriteCount());
            
            log.info("获取文章统计信息成功，文章ID: {}, 统计数据: {}", articleId, statistics);
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取文章统计信息异常，文章ID: {}", articleId, e);
            return Result.error("获取文章统计信息失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> incrementViewCount(Long articleId) {
        log.info("增加文章浏览量，文章ID: {}", articleId);
        
        try {
            // 检查文章是否存在
            Article article = articleMapper.selectById(articleId);
            if (article == null) {
                log.warn("文章不存在，无法增加浏览量，文章ID: {}", articleId);
                return Result.error("文章不存在");
            }
            
            // 增加浏览量
            int result = articleMapper.incrementViewCount(articleId);
            if (result > 0) {
                log.info("成功增加文章浏览量，文章ID: {}", articleId);
                return Result.success();
            } else {
                log.error("增加文章浏览量失败，文章ID: {}", articleId);
                return Result.error("增加文章浏览量失败");
            }
        } catch (Exception e) {
            log.error("增加文章浏览量异常，文章ID: {}", articleId, e);
            return Result.error("增加文章浏览量失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> incrementLikeCount(Long articleId) {
        log.info("增加文章点赞数，文章ID: {}", articleId);
        
        try {
            // 检查文章是否存在
            Article article = articleMapper.selectById(articleId);
            if (article == null) {
                log.warn("文章不存在，无法增加点赞数，文章ID: {}", articleId);
                return Result.error("文章不存在");
            }
            
            // 增加点赞数
            int result = articleMapper.updateLikeCount(articleId, 1);
            if (result > 0) {
                log.info("成功增加文章点赞数，文章ID: {}", articleId);
                return Result.success();
            } else {
                log.error("增加文章点赞数失败，文章ID: {}", articleId);
                return Result.error("增加文章点赞数失败");
            }
        } catch (Exception e) {
            log.error("增加文章点赞数异常，文章ID: {}", articleId, e);
            return Result.error("增加文章点赞数失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> decrementLikeCount(Long articleId) {
        log.info("减少文章点赞数，文章ID: {}", articleId);

        try {
            // 检查文章是否存在
            Article article = articleMapper.selectById(articleId);
            if (article == null) {
                log.warn("文章不存在，无法减少点赞数，文章ID: {}", articleId);
                return Result.error("文章不存在");
            }

            // 使用安全的方法减少点赞数，防止负数（原子操作，无竞态条件）
            int result = articleMapper.decrementLikeCountSafely(articleId);
            if (result > 0) {
                log.info("成功减少文章点赞数，文章ID: {}", articleId);
                return Result.success();
            } else {
                log.debug("文章点赞数已为0或文章不存在，文章ID: {}", articleId);
                return Result.success(); // 返回成功，因为最终状态是正确的（不会是负数）
            }
        } catch (Exception e) {
            log.error("减少文章点赞数异常，文章ID: {}", articleId, e);
            return Result.error("减少文章点赞数失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> incrementCommentCount(Long articleId) {
        log.info("增加文章评论数，文章ID: {}", articleId);
        
        try {
            // 检查文章是否存在
            Article article = articleMapper.selectById(articleId);
            if (article == null) {
                log.warn("文章不存在，无法增加评论数，文章ID: {}", articleId);
                return Result.error("文章不存在");
            }
            
            // 增加评论数
            int result = articleMapper.updateCommentCount(articleId, 1);
            if (result > 0) {
                log.info("成功增加文章评论数，文章ID: {}", articleId);
                return Result.success();
            } else {
                log.error("增加文章评论数失败，文章ID: {}", articleId);
                return Result.error("增加文章评论数失败");
            }
        } catch (Exception e) {
            log.error("增加文章评论数异常，文章ID: {}", articleId, e);
            return Result.error("增加文章评论数失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> decrementCommentCount(Long articleId) {
        log.info("减少文章评论数，文章ID: {}", articleId);

        try {
            // 检查文章是否存在
            Article article = articleMapper.selectById(articleId);
            if (article == null) {
                log.warn("文章不存在，无法减少评论数，文章ID: {}", articleId);
                return Result.error("文章不存在");
            }

            // 使用安全的方法减少评论数，防止负数（原子操作，无竞态条件）
            int result = articleMapper.decrementCommentCountSafely(articleId);
            if (result > 0) {
                log.info("成功减少文章评论数，文章ID: {}", articleId);
                return Result.success();
            } else {
                log.debug("文章评论数已为0或文章不存在，文章ID: {}", articleId);
                return Result.success(); // 返回成功，因为最终状态是正确的（不会是负数）
            }
        } catch (Exception e) {
            log.error("减少文章评论数异常，文章ID: {}", articleId, e);
            return Result.error("减少文章评论数失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> incrementFavoriteCount(Long articleId) {
        log.info("增加文章收藏数，文章ID: {}", articleId);
        
        try {
            // 检查文章是否存在
            Article article = articleMapper.selectById(articleId);
            if (article == null) {
                log.warn("文章不存在，无法增加收藏数，文章ID: {}", articleId);
                return Result.error("文章不存在");
            }
            
            // 增加收藏数
            int result = articleMapper.updateFavoriteCount(articleId, 1);
            if (result > 0) {
                log.info("成功增加文章收藏数，文章ID: {}", articleId);
                return Result.success();
            } else {
                log.error("增加文章收藏数失败，文章ID: {}", articleId);
                return Result.error("增加文章收藏数失败");
            }
        } catch (Exception e) {
            log.error("增加文章收藏数异常，文章ID: {}", articleId, e);
            return Result.error("增加文章收藏数失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> decrementFavoriteCount(Long articleId) {
        log.info("减少文章收藏数，文章ID: {}", articleId);

        try {
            // 检查文章是否存在
            Article article = articleMapper.selectById(articleId);
            if (article == null) {
                log.warn("文章不存在，无法减少收藏数，文章ID: {}", articleId);
                return Result.error("文章不存在");
            }

            // 使用安全的方法减少收藏数，防止负数（原子操作，无竞态条件）
            int result = articleMapper.decrementFavoriteCountSafely(articleId);
            if (result > 0) {
                log.info("成功减少文章收藏数，文章ID: {}", articleId);
                return Result.success();
            } else {
                log.debug("文章收藏数已为0或文章不存在，文章ID: {}", articleId);
                return Result.success(); // 返回成功，因为最终状态是正确的（不会是负数）
            }
        } catch (Exception e) {
            log.error("减少文章收藏数异常，文章ID: {}", articleId, e);
            return Result.error("减少文章收藏数失败");
        }
    }

    @Override
    public Result<List<ArticleStatisticsDTO>> getHotArticleStatistics(Integer limit) {
        log.info("获取热门文章统计信息，限制数量: {}", limit);
        
        try {
            // 获取热门文章列表（按浏览量排序）
            List<Article> hotArticles = articleMapper.selectHotArticles(limit);
            
            // 转换为统计DTO列表
            List<ArticleStatisticsDTO> statisticsList = hotArticles.stream().map(article -> {
                ArticleStatisticsDTO statistics = new ArticleStatisticsDTO();
                statistics.setArticleId(article.getId());
                statistics.setViewCount(article.getViewCount());
                statistics.setLikeCount(article.getLikeCount());
                statistics.setCommentCount(article.getCommentCount());
                statistics.setFavoriteCount(article.getFavoriteCount());
                return statistics;
            }).toList();
            
            log.info("成功获取热门文章统计信息，数量: {}", statisticsList.size());
            return Result.success(statisticsList);
        } catch (Exception e) {
            log.error("获取热门文章统计信息异常", e);
            return Result.error("获取热门文章统计信息失败");
        }
    }

    @Override
    public Result<List<ArticleStatisticsDTO>> getTopArticleStatistics(Integer limit) {
        log.info("获取置顶文章统计信息，限制数量: {}", limit);
        
        try {
            // 获取置顶文章列表
            List<Article> topArticles = articleMapper.selectTopArticles(limit);
            
            // 转换为统计DTO列表
            List<ArticleStatisticsDTO> statisticsList = topArticles.stream().map(article -> {
                ArticleStatisticsDTO statistics = new ArticleStatisticsDTO();
                statistics.setArticleId(article.getId());
                statistics.setViewCount(article.getViewCount());
                statistics.setLikeCount(article.getLikeCount());
                statistics.setCommentCount(article.getCommentCount());
                statistics.setFavoriteCount(article.getFavoriteCount());
                return statistics;
            }).toList();
            
            log.info("成功获取置顶文章统计信息，数量: {}", statisticsList.size());
            return Result.success(statisticsList);
        } catch (Exception e) {
            log.error("获取置顶文章统计信息异常", e);
            return Result.error("获取置顶文章统计信息失败");
        }
    }

    @Override
    public Result<List<ArticleStatisticsDTO>> getRecommendedArticleStatistics(Integer limit) {
        log.info("获取推荐文章统计信息，限制数量: {}", limit);
        
        try {
            // 获取推荐文章列表
            List<Article> recommendedArticles = articleMapper.selectRecommendedArticles(limit);
            
            // 转换为统计DTO列表
            List<ArticleStatisticsDTO> statisticsList = recommendedArticles.stream().map(article -> {
                ArticleStatisticsDTO statistics = new ArticleStatisticsDTO();
                statistics.setArticleId(article.getId());
                statistics.setViewCount(article.getViewCount());
                statistics.setLikeCount(article.getLikeCount());
                statistics.setCommentCount(article.getCommentCount());
                statistics.setFavoriteCount(article.getFavoriteCount());
                return statistics;
            }).toList();
            
            log.info("成功获取推荐文章统计信息，数量: {}", statisticsList.size());
            return Result.success(statisticsList);
        } catch (Exception e) {
            log.error("获取推荐文章统计信息异常", e);
            return Result.error("获取推荐文章统计信息失败");
        }
    }
}