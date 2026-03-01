package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.dto.ArticleDTO;
import com.blog.entity.Article;
import com.blog.mapper.ArticleMapper;
import com.blog.service.ArticleRankService;
import com.blog.service.ArticleService;
import com.blog.utils.BusinessUtils;
import com.blog.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 文章热度排行服务实现类
 * 使用 Redis ZSet 实现实时热度计算
 *
 * 使用带日期的 Key 来隔离不同时间段的数据：
 * - 日榜：hot:articles:zset:day:2026-01-28
 * - 周榜：hot:articles:zset:week:2026-W04
 */
@Service
@Slf4j
public class ArticleRankServiceImpl implements ArticleRankService {

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    @Lazy
    private ArticleService articleService;

    // ZSet Key 前缀
    private static final String ZSET_KEY_DAY_PREFIX = "hot:articles:zset:day:";
    private static final String ZSET_KEY_WEEK_PREFIX = "hot:articles:zset:week:";

    // 日期格式化器
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 分数权重（public 供其他服务访问）
    public static final double SCORE_VIEW = 1.0; // 浏览 +1 分
    public static final double SCORE_LIKE = 5.0; // 点赞 +5 分
    public static final double SCORE_COMMENT = 10.0; // 评论 +10 分
    public static final double SCORE_FAVORITE = 8.0; // 收藏 +8 分

    // 排行榜过期时间（单位：天）
    private static final long TTL_DAY = 2; // 日榜保留 2 天
    private static final long TTL_WEEK = 14; // 周榜保留 14 天

    @Override
    public void incrementScore(Long articleId, double score) {
        if (articleId == null) {
            return;
        }
        // 只更新当前日期的日榜和当前周的周榜
        String dayKey = getDayKey(LocalDate.now());
        String weekKey = getWeekKey(LocalDate.now());

        // 使用 Lua 脚本原子性地更新日榜和周榜，确保数据一致性
        // Lua 脚本内部会处理过期时间设置，无需额外调用 setRankTtl
        Double newScore = redisUtils.zIncrByAtomic(dayKey, weekKey, articleId, score, TTL_DAY, TTL_WEEK);

        log.debug("文章热度分数增加（原子操作），文章ID：{}，分数：{}，新分数：{}，日榜Key：{}，周榜Key：{}",
                articleId, score, newScore, dayKey, weekKey);
    }

    @Override
    public void decrementScore(Long articleId, double score) {
        if (articleId == null) {
            return;
        }
        // 只更新当前日期的日榜和当前周的周榜
        String dayKey = getDayKey(LocalDate.now());
        String weekKey = getWeekKey(LocalDate.now());

        // 使用 Lua 脚本原子性地更新日榜和周榜（传入负数减少分数）
        // Lua 脚本内部会处理过期时间设置，无需额外调用 setRankTtl
        Double newScore = redisUtils.zIncrByAtomic(dayKey, weekKey, articleId, -score, TTL_DAY, TTL_WEEK);

        log.debug("文章热度分数减少（原子操作），文章ID：{}，分数：{}，新分数：{}，日榜Key：{}，周榜Key：{}",
                articleId, score, newScore, dayKey, weekKey);
    }

    @Override
    public Result<List<ArticleDTO>> getHotArticles(Integer limit, String period) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }
        log.info("从 ZSet 获取热门文章，数量：{}，时间范围：{}", limit, period);

        String zsetKey = getZSetKey(period);
        log.info("查询排行榜，使用的 ZSet Key：{}", zsetKey);

        try {
            long zsetSize = redisUtils.zSize(zsetKey);
            log.info("排行榜 ZSet 大小：{}，Key：{}", zsetSize, zsetKey);

            int fetchLimit = (int) (limit * 1.5) + 10;
            Set<Object> articleIdSet = redisUtils.zReverseRange(zsetKey, 0, fetchLimit - 1);

            if (articleIdSet == null || articleIdSet.isEmpty()) {
                log.info("ZSet 为空，返回空列表，Key：{}", zsetKey);
                return BusinessUtils.success(new ArrayList<>());
            }

            List<Long> articleIds = articleIdSet.stream()
                    .map(id -> Long.parseLong(id.toString()))
                    .collect(Collectors.toList());

            log.info("从 ZSet 获取到的文章ID列表：{}，Key：{}", articleIds, zsetKey);

            List<Article> articles = articleMapper.selectBatchIds(articleIds);

            log.info("数据库查询返回的文章数量：{}，文章ID列表：{}",
                    articles != null ? articles.size() : 0,
                    articles != null ? articles.stream().map(Article::getId).collect(Collectors.toList()) : "null");

            Set<Long> existingArticleIds = articles.stream()
                    .map(Article::getId)
                    .collect(Collectors.toSet());

            Map<Long, Article> articleMap = articles.stream()
                    .collect(Collectors.toMap(Article::getId, a -> a));

            List<Article> orderedArticles = new ArrayList<>();
            List<Long> invalidArticleIds = new ArrayList<>();

            for (Long articleId : articleIds) {
                if (!existingArticleIds.contains(articleId)) {
                    invalidArticleIds.add(articleId);
                    continue;
                }

                Article article = articleMap.get(articleId);
                if (article != null) {
                    orderedArticles.add(article);
                }

                if (orderedArticles.size() >= limit) {
                    break;
                }
            }

            List<ArticleDTO> articleDTOs = articleService.batchConvertToDTO(orderedArticles);

            Map<Long, ArticleDTO> dtoMap = articleDTOs.stream()
                    .collect(Collectors.toMap(ArticleDTO::getId, d -> d));

            List<ArticleDTO> orderedDTOs = new ArrayList<>();
            for (Article article : orderedArticles) {
                ArticleDTO dto = dtoMap.get(article.getId());
                if (dto != null) {
                    Double score = redisUtils.zScore(zsetKey, article.getId());
                    if (score != null) {
                        dto.setHotScore(score);
                    }
                    orderedDTOs.add(dto);
                }
            }

            if (!orderedDTOs.isEmpty()) {
                log.info("热门文章排序结果（前{}篇）：", orderedDTOs.size());
                for (int i = 0; i < Math.min(orderedDTOs.size(), 5); i++) {
                    ArticleDTO dto = orderedDTOs.get(i);
                    log.info("  排名{}：文章ID={}, 标题={}, 热度分数={}",
                            i + 1, dto.getId(), dto.getTitle(), dto.getHotScore());
                }
            }

            if (!invalidArticleIds.isEmpty()) {
                log.warn("发现 {} 个无效文章ID将被清理：{}，Key：{}",
                        invalidArticleIds.size(), invalidArticleIds, zsetKey);
                for (Long invalidId : invalidArticleIds) {
                    redisUtils.zRemove(zsetKey, invalidId);
                }
                log.warn("已清理排行榜中的无效文章ID，数量：{}，key：{}", invalidArticleIds.size(), zsetKey);
            }

            log.info("从 ZSet 获取热门文章成功，数量：{}，Key：{}", orderedDTOs.size(), zsetKey);
            return BusinessUtils.success(orderedDTOs);

        } catch (Exception e) {
            log.error("从 ZSet 获取热门文章失败，Key：{}", zsetKey, e);
            return BusinessUtils.error("获取热门文章失败");
        }
    }

    @Override
    public Result<PageResult<ArticleDTO>> getHotArticlesPage(Integer page, Integer size, String period) {
        if (page == null || page < 1)
            page = 1;
        if (size == null || size < 1)
            size = 10;

        log.info("分页从 ZSet 获取热门文章，页码：{}，页大小：{}，时间范围：{}", page, size, period);

        String zsetKey = getZSetKey(period);
        try {
            long total = redisUtils.zSize(zsetKey);
            if (total == 0) {
                return BusinessUtils.success(PageResult.empty(page, size));
            }

            int start = (page - 1) * size;
            int end = start + size - 1;

            Set<Object> articleIdSet = redisUtils.zReverseRange(zsetKey, start, end);
            if (articleIdSet == null || articleIdSet.isEmpty()) {
                return BusinessUtils.success(PageResult.empty(page, size));
            }

            List<Long> articleIds = articleIdSet.stream()
                    .map(id -> Long.parseLong(id.toString()))
                    .collect(Collectors.toList());

            List<Article> articles = articleMapper.selectBatchIds(articleIds);
            Set<Long> existingIds = articles.stream().map(Article::getId).collect(Collectors.toSet());

            Map<Long, Article> articleMap = articles.stream()
                    .collect(Collectors.toMap(Article::getId, a -> a));

            List<Article> orderedArticles = new ArrayList<>();
            for (Long articleId : articleIds) {
                if (existingIds.contains(articleId)) {
                    Article article = articleMap.get(articleId);
                    if (article != null) {
                        orderedArticles.add(article);
                    }
                } else {
                    redisUtils.zRemove(zsetKey, articleId);
                }
            }

            List<ArticleDTO> articleDTOs = articleService.batchConvertToDTO(orderedArticles);

            Map<Long, ArticleDTO> dtoMap = articleDTOs.stream()
                    .collect(Collectors.toMap(ArticleDTO::getId, d -> d));

            List<ArticleDTO> orderedDTOs = new ArrayList<>();
            for (Article article : orderedArticles) {
                ArticleDTO dto = dtoMap.get(article.getId());
                if (dto != null) {
                    Double score = redisUtils.zScore(zsetKey, article.getId());
                    dto.setHotScore(score != null ? score : 0.0);
                    orderedDTOs.add(dto);
                }
            }

            return BusinessUtils.success(PageResult.of(orderedDTOs, total, page, size));
        } catch (Exception e) {
            log.error("分页获取热门文章失败，Key：{}", zsetKey, e);
            return BusinessUtils.error("获取热门文章失败");
        }
    }

    @Override
    public void resetRank(String period) {
        LocalDate today = LocalDate.now();

        if ("day".equalsIgnoreCase(period)) {
            // 重置日榜：初始化今天的日榜 ZSet
            String newDayKey = getDayKey(today);
            initializeZSetForPeriod(newDayKey);
            log.info("日榜已重置并初始化，Key：{}", newDayKey);
        } else if ("week".equalsIgnoreCase(period)) {
            // 重置周榜：初始化本周的周榜 ZSet
            String newWeekKey = getWeekKey(today);
            initializeZSetForPeriod(newWeekKey);
            log.info("周榜已重置并初始化，Key：{}", newWeekKey);
        }
    }

    @Override
    public void initializeArticle(Long articleId) {
        if (articleId == null) {
            return;
        }
        // 新文章初始化分数为 0（只初始化到当前日期的日榜和周榜）
        String dayKey = getDayKey(LocalDate.now());
        String weekKey = getWeekKey(LocalDate.now());

        redisUtils.zAdd(dayKey, articleId, 0);
        redisUtils.zAdd(weekKey, articleId, 0);

        // 设置过期时间
        setRankTtl(dayKey, "day");
        setRankTtl(weekKey, "week");

        log.info("新文章初始化到排行榜，文章ID：{}，日榜Key：{}，周榜Key：{}",
                articleId, dayKey, weekKey);
    }

    /**
     * 初始化所有已发布的文章到排行榜（首次启动时调用）
     * 只初始化当前日期的日榜和周榜
     */
    public void initializeAllArticles() {
        log.info("开始初始化所有已发布文章到排行榜");

        try {
            // 查询所有已发布的文章
            LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Article::getStatus, 2); // 只查询已发布的文章
            queryWrapper.select(Article::getId);
            List<Article> articles = articleMapper.selectList(queryWrapper);

            if (articles.isEmpty()) {
                log.info("没有已发布的文章需要初始化");
                return;
            }

            String dayKey = getDayKey(LocalDate.now());
            String weekKey = getWeekKey(LocalDate.now());

            int dayCount = 0;
            int weekCount = 0;

            for (Article article : articles) {
                Long articleId = article.getId();

                // 检查日榜
                Double dayScore = redisUtils.zScore(dayKey, articleId);
                if (dayScore == null) {
                    boolean added = redisUtils.zAdd(dayKey, articleId, 0);
                    if (added) {
                        Double verifyScore = redisUtils.zScore(dayKey, articleId);
                        if (verifyScore != null && verifyScore == 0) {
                            dayCount++;
                            log.debug("日榜初始化成功，文章ID：{}，Key：{}", articleId, dayKey);
                        }
                    }
                }

                // 检查周榜
                Double weekScore = redisUtils.zScore(weekKey, articleId);
                if (weekScore == null) {
                    boolean added = redisUtils.zAdd(weekKey, articleId, 0);
                    if (added) {
                        Double verifyScore = redisUtils.zScore(weekKey, articleId);
                        if (verifyScore != null && verifyScore == 0) {
                            weekCount++;
                            log.debug("周榜初始化成功，文章ID：{}，Key：{}", articleId, weekKey);
                        }
                    }
                }
            }

            // 初始化完成后设置过期时间
            setRankTtl(dayKey, "day");
            setRankTtl(weekKey, "week");

            log.info("初始化所有文章到排行榜完成，日榜新增：{}，周榜新增：{}，日榜Key：{}，周榜Key：{}",
                    dayCount, weekCount, dayKey, weekKey);

            // 打印 ZSet 大小用于调试
            long daySize = redisUtils.zSize(dayKey);
            long weekSize = redisUtils.zSize(weekKey);
            log.info("ZSet 大小统计，日榜：{}（Key：{}），周榜：{}（Key：{}）",
                    daySize, dayKey, weekSize, weekKey);

        } catch (Exception e) {
            log.error("初始化所有文章到排行榜失败", e);
        }
    }

    @Override
    public void removeFromRank(Long articleId) {
        if (articleId == null) {
            return;
        }
        // 只从当前日期的日榜和周榜中删除
        String dayKey = getDayKey(LocalDate.now());
        String weekKey = getWeekKey(LocalDate.now());

        redisUtils.zRemove(dayKey, articleId);
        redisUtils.zRemove(weekKey, articleId);
        log.info("已从排行榜删除文章，文章ID：{}，日榜Key：{}，周榜Key：{}",
                articleId, dayKey, weekKey);
    }

    // ==================== 私有辅助方法 ====================//

    /**
     * 根据时间范围获取当前日期的 ZSet Key
     * 
     * @param period day 或 week
     * @return 当前的 ZSet Key（如 hot:articles:zset:day:2026-01-28）
     */
    private String getZSetKey(String period) {
        LocalDate today = LocalDate.now();
        if ("week".equalsIgnoreCase(period)) {
            return getWeekKey(today);
        } else {
            return getDayKey(today);
        }
    }

    /**
     * 获取日榜的 Key（带日期）
     * 
     * @param date 日期
     * @return 如 hot:articles:zset:day:2026-01-28
     */
    private String getDayKey(LocalDate date) {
        return ZSET_KEY_DAY_PREFIX + date.format(DATE_FORMATTER);
    }

    /**
     * 获取周榜的 Key（带周数）
     * 修复跨年边界问题：确保新年第一周使用新年份
     *
     * @param date 日期（用于计算所属周）
     * @return 如 hot:articles:zset:week:2026-W04
     */
    private String getWeekKey(LocalDate date) {
        // 获取该日期所在周的周一
        LocalDate monday = date.with(DayOfWeek.MONDAY);

        // 处理跨年边界：如果周一是12月但日期在1月，调整为新年的第一周
        if (monday.getMonth().getValue() == 12 && date.getMonth().getValue() == 1) {
            // 找到新年第一天（1月1日）
            LocalDate newYearDay = LocalDate.of(date.getYear(), 1, 1);

            // 如果1月1日不是周一，找到它所在的周一（可能在上一年）
            LocalDate yearFirstMonday = newYearDay.with(DayOfWeek.MONDAY);

            // 如果第一个周一是上一年12月，我们仍然使用新年年份作为周键
            // 这样确保新年第一周始终使用新年份
            monday = newYearDay;
        }

        // 计算该年份的第一个周一（用于计算周数）
        int year = monday.getYear();
        LocalDate yearFirstDay = LocalDate.of(year, 1, 1);
        LocalDate yearFirstMonday = yearFirstDay.with(DayOfWeek.MONDAY);

        // 如果第一个周一是上一年12月，调整为新年第一个周一
        if (yearFirstMonday.getYear() < year) {
            // 找到新年第一个周一
            yearFirstMonday = yearFirstDay;
            while (yearFirstMonday.getDayOfWeek() != DayOfWeek.MONDAY) {
                yearFirstMonday = yearFirstMonday.plusDays(1);
            }
        }

        // 计算周数：从第一个周一开始计算
        long weekNumber = ChronoUnit.WEEKS.between(yearFirstMonday, monday) + 1;

        // 格式化周键
        return String.format("%s%d-W%02d", ZSET_KEY_WEEK_PREFIX, year, weekNumber);
    }

    /**
     * 为指定周期的 ZSet 初始化所有已发布的文章
     * 
     * @param zsetKey ZSet Key
     */
    private void initializeZSetForPeriod(String zsetKey) {
        try {
            // 查询所有已发布的文章
            LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Article::getStatus, 2);
            queryWrapper.select(Article::getId);
            List<Article> articles = articleMapper.selectList(queryWrapper);

            int count = 0;
            for (Article article : articles) {
                Long articleId = article.getId();
                // 初始化分数为 0
                redisUtils.zAdd(zsetKey, articleId, 0);
                count++;
            }

            // 设置过期时间
            if (zsetKey.contains(":day:")) {
                setRankTtl(zsetKey, "day");
            } else if (zsetKey.contains(":week:")) {
                setRankTtl(zsetKey, "week");
            }

            log.info("初始化 ZSet 完成，Key：{}，初始化文章数：{}", zsetKey, count);
        } catch (Exception e) {
            log.error("初始化 ZSet 失败，Key：{}", zsetKey, e);
        }
    }

    /**
     * 为排行榜 Key 设置过期时间
     * 
     * @param key    Redis Key
     * @param period 时间范围：day 或 week
     */
    private void setRankTtl(String key, String period) {
        try {
            long ttl = "week".equalsIgnoreCase(period) ? TTL_WEEK : TTL_DAY;
            boolean success = redisUtils.expire(key, ttl, TimeUnit.DAYS);
            if (success) {
                log.debug("设置排行榜 TTL 成功，Key：{}，TTL：{} 天", key, ttl);
            } else {
                log.warn("设置排行榜 TTL 失败（Key 可能不存在），Key：{}", key);
            }
        } catch (Exception e) {
            log.error("设置排行榜 TTL 异常，Key：{}", key, e);
        }
    }

    // ==================== 便捷方法：按行为类型增加分数 ====================//

    /**
     * 浏览文章时调用（需排除作者自己）
     */
    public void incrementViewScore(Long articleId, Long viewerId, Long authorId) {
        if (!Objects.equals(viewerId, authorId)) {
            incrementScore(articleId, SCORE_VIEW);
        }
    }

    /**
     * 点赞文章时调用（需排除作者自己）
     */
    public void incrementLikeScore(Long articleId, Long likerId, Long authorId) {
        if (!Objects.equals(likerId, authorId)) {
            incrementScore(articleId, SCORE_LIKE);
        }
    }

    /**
     * 取消点赞时调用（需排除作者自己）
     */
    public void decrementLikeScore(Long articleId, Long likerId, Long authorId) {
        if (!Objects.equals(likerId, authorId)) {
            decrementScore(articleId, SCORE_LIKE);
        }
    }

    /**
     * 评论文章时调用（需排除作者自己）
     */
    public void incrementCommentScore(Long articleId, Long commenterId, Long authorId) {
        if (!Objects.equals(commenterId, authorId)) {
            incrementScore(articleId, SCORE_COMMENT);
        }
    }

    /**
     * 删除评论时调用（需排除作者自己）
     */
    public void decrementCommentScore(Long articleId, Long commenterId, Long authorId) {
        if (!Objects.equals(commenterId, authorId)) {
            decrementScore(articleId, SCORE_COMMENT);
        }
    }

    /**
     * 收藏文章时调用（需排除作者自己）
     */
    public void incrementFavoriteScore(Long articleId, Long favoriterId, Long authorId) {
        if (!Objects.equals(favoriterId, authorId)) {
            incrementScore(articleId, SCORE_FAVORITE);
        }
    }

    /**
     * 取消收藏时调用（需排除作者自己）
     */
    public void decrementFavoriteScore(Long articleId, Long favoriterId, Long authorId) {
        if (!Objects.equals(favoriterId, authorId)) {
            decrementScore(articleId, SCORE_FAVORITE);
        }
    }

    @Override
    public Map<Long, Double> getArticleScores(List<Long> articleIds, String period) {
        if (articleIds == null || articleIds.isEmpty()) {
            return new HashMap<>();
        }

        String zsetKey = getZSetKey(period);
        Map<Long, Double> scores = new HashMap<>();

        try {
            for (Long articleId : articleIds) {
                Double score = redisUtils.zScore(zsetKey, articleId);
                if (score != null) {
                    scores.put(articleId, score);
                }
            }
            log.debug("批量获取文章热度分数，Key：{}，请求数量：{}，获取数量：{}", zsetKey, articleIds.size(), scores.size());
        } catch (Exception e) {
            log.error("批量获取文章热度分数失败，Key：{}", zsetKey, e);
        }

        return scores;
    }
}
