package com.blog.service.impl;

import com.blog.common.Result;
import com.blog.dto.ArticleStatisticsDTO;
import com.blog.entity.Article;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.UserLikeMapper;
import com.blog.service.ArticleStatisticsService;
import com.blog.utils.RedisCacheUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PreDestroy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Result<ArticleStatisticsDTO> getArticleStatistics(Long articleId) {
        log.info("获取文章统计信息，文章ID: {}", articleId);

        try {
            Article article = articleMapper.selectById(articleId);
            if (article == null) {
                log.warn("文章不存在，文章ID: {}", articleId);
                return Result.error("文章不存在");
            }

            int dbViewCount = article.getViewCount() != null ? article.getViewCount() : 0;
            int redisViewCount = getRedisViewCount(articleId);
            int totalViewCount = dbViewCount + redisViewCount;

            ArticleStatisticsDTO statistics = new ArticleStatisticsDTO();
            statistics.setArticleId(articleId);
            statistics.setViewCount(totalViewCount);
            statistics.setLikeCount(article.getLikeCount());
            statistics.setCommentCount(article.getCommentCount());
            statistics.setFavoriteCount(article.getFavoriteCount());

            log.info("获取文章统计信息成功，文章ID: {}, DB浏览量: {}, Redis浏览量: {}, 总浏览量: {}",
                    articleId, dbViewCount, redisViewCount, totalViewCount);
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取文章统计信息异常，文章ID: {}", articleId, e);
            return Result.error("获取文章统计信息失败");
        }
    }

    @Override
    public Result<Void> incrementViewCount(Long articleId) {
        log.debug("增加文章浏览量，文章ID: {}", articleId);

        try {
            Article article = articleMapper.selectById(articleId);
            if (article == null) {
                log.warn("文章不存在，无法增加浏览量，文章ID: {}", articleId);
                return Result.error("文章不存在");
            }

            if (article.getStatus() != 2) {
                log.warn("文章未发布，无法增加浏览量，文章ID: {}, 状态: {}", articleId, article.getStatus());
                return Result.error("文章未发布");
            }

            String viewCountKey = RedisCacheUtils.generateArticleViewCountKey(articleId);
            redisTemplate.opsForValue().increment(viewCountKey, 1);

            redisTemplate.opsForSet().add(RedisCacheUtils.ARTICLE_VIEW_QUEUE_KEY, articleId);

            log.debug("文章浏览量已写入Redis，文章ID: {}", articleId);
            return Result.success();
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
            int result = articleMapper.updateLikeCount(articleId, 1);
            if (result > 0) {
                log.info("成功增加文章点赞数，文章ID: {}", articleId);
                return Result.success();
            } else {
                log.warn("文章不存在，无法增加点赞数，文章ID: {}", articleId);
                return Result.error("文章不存在");
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
            int result = articleMapper.decrementLikeCountSafely(articleId);
            if (result > 0) {
                log.info("成功减少文章点赞数，文章ID: {}", articleId);
                return Result.success();
            } else {
                log.debug("文章点赞数已为0或文章不存在，文章ID: {}", articleId);
                return Result.success();
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
            int result = articleMapper.updateCommentCount(articleId, 1);
            if (result > 0) {
                log.info("成功增加文章评论数，文章ID: {}", articleId);
                return Result.success();
            } else {
                log.warn("文章不存在，无法增加评论数，文章ID: {}", articleId);
                return Result.error("文章不存在");
            }
        } catch (Exception e) {
            log.error("增加文章评论数异常，文章ID: {}", articleId, e);
            return Result.error("增加文章评论数失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> decrementCommentCount(Long articleId) {
        return decrementCommentCount(articleId, 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> decrementCommentCount(Long articleId, int count) {
        log.info("减少文章评论数，文章ID: {}, 数量: {}", articleId, count);

        if (count <= 0) {
            return Result.success();
        }

        try {
            int result = articleMapper.updateCommentCount(articleId, -count);
            if (result > 0) {
                log.info("成功减少文章评论数，文章ID: {}, 数量: {}", articleId, count);
                return Result.success();
            } else {
                log.debug("文章不存在，文章ID: {}", articleId);
                return Result.success();
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
            int result = articleMapper.updateFavoriteCount(articleId, 1);
            if (result > 0) {
                log.info("成功增加文章收藏数，文章ID: {}", articleId);
                return Result.success();
            } else {
                log.warn("文章不存在，无法增加收藏数，文章ID: {}", articleId);
                return Result.error("文章不存在");
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
            int result = articleMapper.decrementFavoriteCountSafely(articleId);
            if (result > 0) {
                log.info("成功减少文章收藏数，文章ID: {}", articleId);
                return Result.success();
            } else {
                log.debug("文章收藏数已为0或文章不存在，文章ID: {}", articleId);
                return Result.success();
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
                int dbViewCount = article.getViewCount() != null ? article.getViewCount() : 0;
                int redisViewCount = getRedisViewCount(article.getId());
                statistics.setViewCount(dbViewCount + redisViewCount);
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
                int dbViewCount = article.getViewCount() != null ? article.getViewCount() : 0;
                int redisViewCount = getRedisViewCount(article.getId());
                statistics.setViewCount(dbViewCount + redisViewCount);
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
                int dbViewCount = article.getViewCount() != null ? article.getViewCount() : 0;
                int redisViewCount = getRedisViewCount(article.getId());
                statistics.setViewCount(dbViewCount + redisViewCount);
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

    private int getRedisViewCount(Long articleId) {
        try {
            String viewCountKey = RedisCacheUtils.generateArticleViewCountKey(articleId);
            Object value = redisTemplate.opsForValue().get(viewCountKey);
            if (value != null) {
                return Integer.parseInt(value.toString());
            }
        } catch (Exception e) {
            log.warn("获取Redis浏览量失败，文章ID: {}", articleId, e);
        }
        return 0;
    }

    @PreDestroy
    public void onShutdown() {
        log.info("应用关闭前强制同步浏览量...");
        try {
            syncViewCountToDatabase();
            log.info("应用关闭前浏览量同步完成");
        } catch (Exception e) {
            log.error("应用关闭前浏览量同步失败", e);
        }
    }

    @Scheduled(fixedRate = 30000)
    public void syncViewCountToDatabase() {
        try {
            List<Object> syncData = atomicPopViewCounts(1000);

            if (syncData == null || syncData.isEmpty()) {
                return;
            }

            log.info("开始同步浏览量到数据库，待同步文章数: {}", syncData.size());

            Set<Long> processedArticleIds = new HashSet<>();
            int successCount = 0;
            int totalIncrement = 0;

            for (Object item : syncData) {
                try {
                    if (!(item instanceof List)) {
                        continue;
                    }

                    List<?> pair = (List<?>) item;
                    if (pair.size() < 2) {
                        continue;
                    }

                    Long articleId = Long.parseLong(pair.get(0).toString());
                    int increment = Integer.parseInt(pair.get(1).toString());

                    if (processedArticleIds.contains(articleId)) {
                        continue;
                    }
                    processedArticleIds.add(articleId);

                    if (increment > 0) {
                        int result = articleMapper.incrementViewCountBatch(articleId, increment);
                        if (result > 0) {
                            successCount++;
                            totalIncrement += increment;
                            log.debug("浏览量同步成功，文章ID: {}, 增量: {}", articleId, increment);
                        }
                    }
                } catch (Exception e) {
                    log.error("同步浏览量失败，数据项: {}", item, e);
                }
            }

            log.info("浏览量同步完成，成功同步文章数: {}, 总浏览量增量: {}", successCount, totalIncrement);
        } catch (Exception e) {
            log.error("浏览量同步任务异常", e);
        }
    }

    /**
     * 使用 Lua 脚本原子性地从队列弹出文章ID并获取对应的浏览量
     * 解决 pop 和 getAndDelete 操作之间的竞态条件
     * 
     * @param batchSize 批量处理数量
     * @return 包含 [articleId, viewCount] 对的列表
     */
    @SuppressWarnings("unchecked")
    private List<Object> atomicPopViewCounts(int batchSize) {
        String luaScript = """
                local queueKey = KEYS[1]
                local countPrefix = ARGV[1]
                local batchSize = tonumber(ARGV[2])

                -- 从队列中弹出文章ID
                local articleIds = redis.call('SPOP', queueKey, batchSize)

                if not articleIds or #articleIds == 0 then
                    return {}
                end

                local result = {}

                -- 遍历文章ID，获取并删除对应的浏览量计数
                for i, articleId in ipairs(articleIds) do
                    local viewCountKey = countPrefix .. articleId
                    local count = redis.call('GET', viewCountKey)

                    if count then
                        redis.call('DEL', viewCountKey)
                        table.insert(result, {articleId, count})
                    end
                end

                return result
                """;

        try {
            DefaultRedisScript<List> script = new DefaultRedisScript<>(luaScript, List.class);

            List<Object> results = redisTemplate.execute(
                    script,
                    Collections.singletonList(RedisCacheUtils.ARTICLE_VIEW_QUEUE_KEY),
                    RedisCacheUtils.ARTICLE_VIEW_COUNT_PREFIX,
                    String.valueOf(batchSize));

            if (results != null && !results.isEmpty()) {
                log.debug("Lua脚本执行成功，获取到 {} 条浏览量记录", results.size());
            }

            return results != null ? results : Collections.emptyList();
        } catch (Exception e) {
            log.error("执行浏览量同步Lua脚本失败", e);
            return Collections.emptyList();
        }
    }
}