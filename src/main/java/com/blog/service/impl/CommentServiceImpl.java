package com.blog.service.impl;

import com.blog.common.Result;
import com.blog.dto.CommentCreateDTO;
import com.blog.dto.CommentDTO;
import com.blog.entity.Comment;
import com.blog.entity.CommentLike;
import com.blog.entity.Notification;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CommentLikeMapper;
import com.blog.mapper.CommentMapper;
import com.blog.service.ArticleStatisticsService;
import com.blog.service.CommentService;
import com.blog.entity.Article;
import com.blog.event.NotificationEvent;
import com.blog.utils.AuthUtils;
import com.blog.utils.BusinessUtils;
import com.blog.utils.CacheUtils;
import com.blog.utils.DTOConverter;
import com.blog.utils.PageUtils;
import com.blog.utils.RedisCacheUtils;
import com.blog.utils.RedisDistributedLock;
import com.blog.utils.SensitiveWordFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 评论服务实现类
 */
@Service
public class CommentServiceImpl implements CommentService {
    private static final Logger log = LoggerFactory.getLogger(CommentServiceImpl.class);

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private CommentLikeMapper commentLikeMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private RedisCacheUtils redisCacheUtils;

    @Autowired
    private SensitiveWordFilter sensitiveWordFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ArticleStatisticsService articleStatisticsService;

    @Autowired
    private com.blog.service.ArticleRankService articleRankService;

    @Autowired
    private RedisDistributedLock redisDistributedLock;

    @Autowired
    private CacheUtils cacheUtils;

    @Override
    @Transactional
    public Result<Long> createComment(CommentCreateDTO commentCreateDTO) {
        try {
            // 检查关联文章是否存在
            Article article = articleMapper.selectById(commentCreateDTO.getArticleId());
            if (article == null) {
                return BusinessUtils.error("关联的文章不存在");
            }
            // 检查文章状态（不允许对未发布文章评论）
            if (article.getStatus() != 2) {
                return BusinessUtils.error("该文章未发布，无法评论");
            }

            // 敏感词检测
            if (sensitiveWordFilter.containsSensitiveWords(commentCreateDTO.getContent())) {
                return BusinessUtils.error("评论内容包含敏感词，请修改后重试");
            }

            Comment comment = DTOConverter.convert(commentCreateDTO, Comment.class);
            // 统一处理为两层结构：顶层(parent_id=0)与二级(挂在顶层)
            if (comment.getParentId() == null) {
                comment.setParentId(0L);
            }
            if (comment.getParentId() > 0) {
                Long targetId = commentCreateDTO.getReplyToCommentId() != null
                        ? commentCreateDTO.getReplyToCommentId()
                        : comment.getParentId();
                Comment target = commentMapper.selectById(targetId);
                if (target == null) {
                    return BusinessUtils.error("被回复的评论不存在");
                }
                Long rootId = (target.getParentId() == null || target.getParentId() == 0)
                        ? target.getId()
                        : target.getParentId();
                comment.setParentId(rootId);
                comment.setReplyToCommentId(target.getId());
            } else {
                comment.setReplyToCommentId(null);
            }
            // 设置用户ID，确保不为null
            if (comment.getUserId() == null) {
                return BusinessUtils.error("用户未登录");
            }
            comment.setLikeCount(0);
            comment.setStatus(2); // 已通过
            comment.setCreateTime(LocalDateTime.now());
            comment.setUpdateTime(LocalDateTime.now());
            // 逻辑删除字段由MyBatis Plus自动处理，无需手动设置

            int result = commentMapper.insert(comment);
            if (result > 0) {
                log.info("发表评论成功，文章ID：{}，用户ID：{}", comment.getArticleId(), comment.getUserId());

                // 在事务提交后异步更新 Redis ZSet 热度分数（排除作者自己）
                Long articleId = comment.getArticleId();
                Long commenterId = comment.getUserId();
                Long authorId = article.getAuthorId();

                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            articleRankService.incrementCommentScore(articleId, commenterId, authorId);
                        } catch (Exception e) {
                            log.error("更新评论热度分数失败", e);
                        }
                    }
                });

                // 发送评论通知
                sendCommentNotification(comment);

                // 清除相关缓存
                clearCommentCache(comment.getArticleId());

                articleStatisticsService.incrementCommentCount(comment.getArticleId());

                return BusinessUtils.success(comment.getId());
            } else {
                return BusinessUtils.error("发表评论失败");
            }
        } catch (Exception e) {
            log.error("发表评论失败", e);
            return BusinessUtils.error("发表评论失败");
        }
    }

    @Override
    public Result<List<CommentDTO>> getCommentList(Long articleId, Integer page, Integer size, Integer status,
            String sortBy, Long userId) {
        try {
            // 设置默认值
            page = PageUtils.getValidPage(page);
            size = PageUtils.getValidSize(size);
            if (status == null) {
                status = 2; // 默认只查询已通过的评论
            }
            if (sortBy == null) {
                sortBy = "time"; // 默认按时间排序
            }

            // 尝试从缓存获取
            String cacheKey = RedisCacheUtils.generateCommentListKey(articleId, page, size, sortBy);
            // 带用户ID的缓存不使用全局缓存，避免不同用户看到相同的点赞状态
            Object cachedData = userId == null ? redisCacheUtils.getCache(cacheKey) : null;
            if (cachedData != null) {
                try {
                    @SuppressWarnings("unchecked")
                    List<CommentDTO> commentDTOs = (List<CommentDTO>) cachedData;
                    return BusinessUtils.success(commentDTOs);
                } catch (ClassCastException e) {
                    log.warn("缓存数据类型异常，缓存键：{}，将重新查询", cacheKey, e);
                    // 继续执行查询逻辑
                }
            }

            // 计算偏移量（分页仅针对顶层评论）
            int offset = PageUtils.calculateOffset(page, size);

            // 查询顶层评论
            List<Comment> topLevel = commentMapper.selectTopLevelCommentsWithPagination(articleId, status, offset,
                    size);
            List<CommentDTO> rootComments = new ArrayList<>();

            // 查询所有子评论（仅二级）
            List<Long> rootIds = topLevel.stream().map(Comment::getId).collect(Collectors.toList());
            List<Comment> children = rootIds.isEmpty() ? List.of()
                    : commentMapper.selectChildCommentsByParentIds(rootIds, status);

            // 批量查询点赞状态（避免 N+1 查询）
            Map<Long, Boolean> likeStatusMap = new HashMap<>();
            if (userId != null) {
                // 收集所有评论ID（顶层 + 子评论）
                List<Long> allCommentIds = new ArrayList<>();
                allCommentIds.addAll(topLevel.stream().map(Comment::getId).collect(Collectors.toList()));
                allCommentIds.addAll(children.stream().map(Comment::getId).collect(Collectors.toList()));

                if (!allCommentIds.isEmpty()) {
                    Result<Map<Long, Boolean>> batchResult = batchCheckCommentLikeStatus(allCommentIds, userId);
                    if (batchResult != null && batchResult.getData() != null) {
                        likeStatusMap = batchResult.getData();
                    }
                }
            }

            // 使用批量查询结果转换顶层评论
            for (Comment c : topLevel) {
                rootComments.add(convertToDTOWithLikeStatus(c, likeStatusMap));
            }

            List<CommentDTO> childDTOs = new ArrayList<>();
            for (Comment ch : children) {
                childDTOs.add(convertToDTOWithLikeStatus(ch, likeStatusMap));
            }

            // 建立id->昵称字典（从DTO中取，避免实体缺字段问题）
            java.util.Map<Long, String> nicknameDict = childDTOs.stream()
                    .collect(Collectors.toMap(CommentDTO::getId, CommentDTO::getNickname, (a, b) -> a));
            for (CommentDTO rc : rootComments) {
                nicknameDict.putIfAbsent(rc.getId(), rc.getNickname());
            }

            // 组装二级列表并填充replyTo
            java.util.Map<Long, CommentDTO> rootMap = rootComments.stream()
                    .collect(Collectors.toMap(CommentDTO::getId, rc -> rc));
            for (int i = 0; i < children.size(); i++) {
                Comment ch = children.get(i);
                CommentDTO childDto = childDTOs.get(i);
                Long replyToId = ch.getReplyToCommentId() != null ? ch.getReplyToCommentId() : ch.getParentId();
                childDto.setReplyToCommentId(replyToId);
                String targetNickname = nicknameDict.get(replyToId);
                if (replyToId != null) {
                    Comment target = commentMapper.selectById(replyToId);
                    if (target != null) {
                        childDto.setReplyToUserId(target.getUserId());
                        if (targetNickname == null) {
                            // 若昵称未命中，保留为空；由前端回退展示为"评论"
                        }
                    }
                }
                childDto.setReplyToNickname(targetNickname);

                CommentDTO root = rootMap.get(ch.getParentId());
                if (root != null) {
                    if (root.getChildren() == null)
                        root.setChildren(new ArrayList<>());
                    root.getChildren().add(childDto);
                }
            }

            // 缓存结果，有效期1小时（仅当没有用户ID时缓存）
            if (userId == null) {
                redisCacheUtils.setCache(cacheKey, rootComments, 1, TimeUnit.HOURS);
            }

            return BusinessUtils.success(rootComments);
        } catch (Exception e) {
            log.error("获取评论列表失败", e);
            return BusinessUtils.error("获取评论列表失败");
        }
    }

    @Override
    public Result<CommentDTO> getCommentById(Long commentId) {
        try {
            // 尝试从缓存获取
            String cacheKey = RedisCacheUtils.generateCommentDetailKey(commentId);
            Object cachedData = redisCacheUtils.getCache(cacheKey);
            if (cachedData != null) {
                try {
                    CommentDTO commentDTO = (CommentDTO) cachedData;
                    return BusinessUtils.success(commentDTO);
                } catch (ClassCastException e) {
                    log.warn("缓存数据类型异常，缓存键：{}，将重新查询", cacheKey, e);
                    // 继续执行查询逻辑
                }
            }

            Comment comment = BusinessUtils.checkIdExist(commentId, commentMapper::selectById, "评论不存在");
            CommentDTO commentDTO = convertToDTO(comment);

            // 缓存结果，有效期24小时
            redisCacheUtils.setCache(cacheKey, commentDTO, 24, TimeUnit.HOURS);

            return BusinessUtils.success(commentDTO);
        } catch (Exception e) {
            log.error("获取评论详情失败", e);
            return BusinessUtils.error("获取评论详情失败");
        }
    }

    @Override
    @Transactional
    public Result<Void> deleteComment(Long commentId) {
        String lockKey = "comment:delete:" + commentId;
        String lockValue = null;

        try {
            lockValue = redisDistributedLock.tryLock(lockKey, 10, TimeUnit.SECONDS);
            if (lockValue == null) {
                log.warn("获取评论删除锁失败，评论ID：{}", commentId);
                return BusinessUtils.error("操作过于频繁，请稍后重试");
            }

            Comment comment = BusinessUtils.checkIdExist(commentId, commentMapper::selectById, "评论不存在");

            // 获取文章作者ID，用于判断是否排除自己评论
            Article article = articleMapper.selectById(comment.getArticleId());
            Long authorId = article != null ? article.getAuthorId() : null;
            Long articleId = comment.getArticleId();

            // 收集所有需要删除的评论（包括当前评论和所有子评论）
            List<Comment> commentsToDelete = new ArrayList<>();
            commentsToDelete.add(comment);

            // 递归收集所有子评论
            List<Comment> allChildComments = collectAllChildComments(commentId);
            commentsToDelete.addAll(allChildComments);

            log.info("准备删除评论，父评论ID：{}，子评论数量：{}，总评论数：{}",
                    commentId, allChildComments.size(), commentsToDelete.size());

            // 删除所有评论及其相关数据
            for (Comment c : commentsToDelete) {
                // 删除评论点赞记录
                commentLikeMapper.deleteByCommentId(c.getId());
                // 删除评论
                commentMapper.deleteById(c.getId());
                // 清除评论详情缓存
                redisCacheUtils.deleteCache(RedisCacheUtils.generateCommentDetailKey(c.getId()));
                log.debug("删除评论成功，评论ID：{}", c.getId());
            }

            // 计算需要扣减的总热度分数（排除作者自己的评论）
            double totalScoreToDecrement = 0.0;
            for (Comment c : commentsToDelete) {
                if (!Objects.equals(c.getUserId(), authorId)) {
                    totalScoreToDecrement += com.blog.service.impl.ArticleRankServiceImpl.SCORE_COMMENT;
                }
            }

            log.info("删除评论完成，总删除数量：{}，需要扣减的热度分数：{}（已排除作者自己的评论）",
                    commentsToDelete.size(), totalScoreToDecrement);

            // 在事务提交后异步更新 Redis ZSet 热度分数
            double finalScoreToDecrement = totalScoreToDecrement;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        if (finalScoreToDecrement > 0) {
                            articleRankService.decrementScore(articleId, finalScoreToDecrement);
                            log.info("已扣减文章热度分数，文章ID：{}，扣减分数：{}", articleId, finalScoreToDecrement);
                        } else {
                            log.info("无需扣减热度分数（所有评论均为作者本人所发），文章ID：{}", articleId);
                        }
                    } catch (Exception e) {
                        log.error("更新评论热度分数失败", e);
                    }
                }
            });

            // 清除文章评论列表缓存
            clearCommentCache(articleId);

            // 一次性扣减评论数（而非循环多次扣减）
            articleStatisticsService.decrementCommentCount(articleId, commentsToDelete.size());

            return BusinessUtils.success();
        } catch (Exception e) {
            log.error("删除评论失败", e);
            return BusinessUtils.error("删除评论失败");
        } finally {
            if (lockValue != null) {
                redisDistributedLock.unlock(lockKey, lockValue);
            }
        }
    }

    /**
     * 递归收集所有子评论
     * @param parentId 父评论ID
     * @return 所有子评论列表
     */
    private List<Comment> collectAllChildComments(Long parentId) {
        List<Comment> allChildren = new ArrayList<>();
        List<Comment> directChildren = commentMapper.selectDirectChildComments(parentId);

        for (Comment child : directChildren) {
            allChildren.add(child);
            // 递归获取子评论的子评论
            allChildren.addAll(collectAllChildComments(child.getId()));
        }

        return allChildren;
    }

    @Override
    public Result<Void> reviewComment(Long commentId, Integer status) {
        try {
            // 验证状态值是否合法（只允许1-待审核，2-已通过，3-已拒绝）
            if (status == null || !BusinessUtils.isValidStatus(status, 1, 2, 3)) {
                return BusinessUtils.error("评论状态值无效，仅允许1(待审核)、2(已通过)或3(已拒绝)");
            }

            Comment comment = BusinessUtils.checkIdExist(commentId, commentMapper::selectById, "评论不存在");
            Integer prevStatus = comment.getStatus();
            comment.setStatus(status);
            BusinessUtils.setUpdateTime(comment);

            int result = commentMapper.updateById(comment);
            if (result > 0) {
                log.info("审核评论成功，评论ID：{}，状态：{}", commentId, status);

                // 清除相关缓存
                clearCommentCache(comment.getArticleId());
                redisCacheUtils.deleteCache(RedisCacheUtils.generateCommentDetailKey(commentId));

                if ((prevStatus == null || prevStatus != 2) && status == 2) {
                    articleStatisticsService.incrementCommentCount(comment.getArticleId());
                } else if (prevStatus != null && prevStatus == 2 && status != 2) {
                    articleStatisticsService.decrementCommentCount(comment.getArticleId());
                }

                return BusinessUtils.success();
            } else {
                return BusinessUtils.error("审核评论失败");
            }
        } catch (Exception e) {
            log.error("审核评论失败", e);
            return BusinessUtils.error("审核评论失败");
        }
    }

    @Override
    public Result<Integer> getArticleCommentCount(Long articleId) {
        try {
            // 尝试从缓存获取
            String cacheKey = RedisCacheUtils.generateCommentCountKey(articleId);
            Object cachedData = redisCacheUtils.getCache(cacheKey);
            if (cachedData != null) {
                return BusinessUtils.success((Integer) cachedData);
            }

            // 只统计已通过审核的评论（状态为2）
            int count = commentMapper.selectCommentsByArticleId(articleId, 2).size();

            // 缓存结果，有效期5分钟
            redisCacheUtils.setCache(cacheKey, count, 5, TimeUnit.MINUTES);

            return BusinessUtils.success(count);
        } catch (Exception e) {
            log.error("获取文章评论数量失败", e);
            return BusinessUtils.error("获取文章评论数量失败");
        }
    }

    @Override
    public Result<List<CommentDTO>> getUserComments(Long userId, Integer page, Integer size) {
        try {
            // 设置默认值
            page = PageUtils.getValidPage(page);
            size = PageUtils.getValidSize(size);

            // 计算偏移量
            int offset = PageUtils.calculateOffset(page, size);

            // 查询分页数据
            List<Comment> comments = commentMapper.selectCommentsByUserIdWithPagination(userId, null, offset, size);
            List<CommentDTO> commentDTOList = PageUtils.convertList(comments, this::convertToDTO);
            // 处理parentId为0的情况
            commentDTOList.forEach(comment -> {
                if (comment.getParentId() == null) {
                    comment.setParentId(0L);
                }
            });
            return BusinessUtils.success(commentDTOList);
        } catch (Exception e) {
            log.error("获取用户评论列表失败", e);
            return BusinessUtils.error("获取用户评论列表失败");
        }
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public Result<Void> likeComment(Long commentId) {
        // 从当前请求上下文中获取用户ID
        Long userId = AuthUtils.getCurrentUserId();

        if (userId == null) {
            log.warn("点赞评论失败：用户未登录");
            return BusinessUtils.error("用户未登录");
        }

        // 生成分布式锁key
        String lockKey = RedisDistributedLock.generateCommentLikeLockKey(commentId, userId);
        String lockValue = null;
        String likeCacheKey = RedisCacheUtils.generateCommentLikeKey(commentId, userId);
        String commentDetailKey = RedisCacheUtils.generateCommentDetailKey(commentId);

        try {
            // 尝试获取分布式锁，防止并发点赞
            // 锁过期时间5秒，等待时间10秒
            lockValue = redisDistributedLock.tryLock(lockKey, 5, TimeUnit.SECONDS, 10, TimeUnit.SECONDS);

            if (lockValue == null) {
                // 获取锁失败，说明有其他请求正在处理该用户的该评论点赞
                log.warn("点赞评论失败：操作过于频繁，评论ID：{}，用户ID：{}", commentId, userId);
                return BusinessUtils.error("操作过于频繁，请稍后再试");
            }

            // 检查记录是否存在
            Comment comment = commentMapper.selectById(commentId);
            if (comment == null) {
                log.warn("点赞评论失败：评论不存在，评论ID：{}", commentId);
                return BusinessUtils.error("评论不存在或已被删除");
            }

            // 检查是否已点赞（先检查缓存，再检查数据库）
            Boolean liked = (Boolean) redisCacheUtils.getCache(likeCacheKey);

            // 如果缓存中没有，检查数据库
            if (!Boolean.TRUE.equals(liked)) {
                boolean existsInDb = commentLikeMapper.checkUserLikedComment(commentId, userId);
                if (existsInDb) {
                    // 数据库中已有记录，同步缓存并返回已点赞
                    redisCacheUtils.setCache(likeCacheKey, true, 7, TimeUnit.DAYS);
                    return BusinessUtils.error("您已点赞过该评论");
                }
            } else {
                return BusinessUtils.error("您已点赞过该评论");
            }

            // 添加点赞记录
            CommentLike commentLike = new CommentLike(commentId, userId);
            int insertResult = 0;
            try {
                insertResult = commentLikeMapper.insert(commentLike);
            } catch (DuplicateKeyException e) {
                // 唯一约束冲突，说明已经点赞过了（正常情况下不应到达这里）
                log.warn("尝试重复点赞，评论ID：{}，用户ID：{}", commentId, userId);
                boolean existsInDb = commentLikeMapper.checkUserLikedComment(commentId, userId);
                redisCacheUtils.setCache(likeCacheKey, existsInDb, 7, TimeUnit.DAYS);
                return BusinessUtils.error("您已点赞过该评论");
            }

            if (insertResult <= 0) {
                return BusinessUtils.error("评论点赞失败");
            }

            // 更新评论点赞数
            Integer updateResult = commentMapper.incrementLikeCount(commentId);
            if (updateResult == null || updateResult <= 0) {
                throw new RuntimeException("更新点赞数失败，事务回滚");
            }

            // 使用事务同步机制，在事务成功提交后再更新缓存
            final Long finalUserId = userId;
            final String finalLikeCacheKey = likeCacheKey;
            final String finalCommentDetailKey = commentDetailKey;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cacheUtils.deleteCacheWithDoubleDelete(finalLikeCacheKey);
                    cacheUtils.deleteCacheAsync(finalCommentDetailKey);
                    log.info("评论点赞成功，已更新缓存，评论ID：{}，用户ID：{}", commentId, finalUserId);
                }

                @Override
                public void afterCompletion(int status) {
                    if (status != TransactionSynchronization.STATUS_COMMITTED) {
                        log.warn("评论点赞事务回滚，缓存未更新，评论ID：{}，用户ID：{}", commentId, finalUserId);
                    }
                }
            });

            // 发送点赞通知（异步，不影响主流程）
            try {
                sendLikeNotification(commentId, userId);
            } catch (Exception e) {
                log.error("发送点赞通知失败，但不影响点赞操作", e);
            }

            log.info("评论点赞成功（待事务提交），评论ID：{}，用户ID：{}", commentId, userId);
            return BusinessUtils.success();
        } catch (DuplicateKeyException e) {
            // 处理唯一约束异常
            log.warn("点赞操作因唯一约束失败，评论ID：{}", commentId, e);
            return BusinessUtils.error("您已点赞过该评论");
        } catch (Exception e) {
            log.error("评论点赞失败", e);
            throw e; // 重新抛出异常以触发事务回滚
        } finally {
            // 释放分布式锁
            if (lockValue != null) {
                redisDistributedLock.unlock(lockKey, lockValue);
            }
        }
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public Result<Void> unlikeComment(Long commentId) {
        // 从当前请求上下文中获取用户ID
        Long userId = AuthUtils.getCurrentUserId();

        if (userId == null) {
            log.warn("取消点赞评论失败：用户未登录");
            return BusinessUtils.error("用户未登录");
        }

        // 生成分布式锁key
        String lockKey = RedisDistributedLock.generateCommentLikeLockKey(commentId, userId);
        String lockValue = null;
        String likeCacheKey = RedisCacheUtils.generateCommentLikeKey(commentId, userId);
        String commentDetailKey = RedisCacheUtils.generateCommentDetailKey(commentId);

        try {
            // 尝试获取分布式锁，防止并发取消点赞
            // 锁过期时间5秒，等待时间10秒
            lockValue = redisDistributedLock.tryLock(lockKey, 5, TimeUnit.SECONDS, 10, TimeUnit.SECONDS);

            if (lockValue == null) {
                // 获取锁失败，说明有其他请求正在处理该用户的该评论点赞
                log.warn("取消点赞评论失败：操作过于频繁，评论ID：{}，用户ID：{}", commentId, userId);
                return BusinessUtils.error("操作过于频繁，请稍后再试");
            }

            // 检查点赞状态（先检查缓存，再检查数据库）
            Boolean liked = (Boolean) redisCacheUtils.getCache(likeCacheKey);
            boolean existsInDb = false;

            if (liked == null || Boolean.FALSE.equals(liked)) {
                existsInDb = commentLikeMapper.checkUserLikedComment(commentId, userId);
                if (!existsInDb) {
                    // 记录不存在，返回成功（幂等性处理）
                    log.info("取消评论点赞成功（无记录需要取消），评论ID：{}，用户ID：{}", commentId, userId);
                    // 确保缓存状态一致
                    redisCacheUtils.setCache(likeCacheKey, false, 7, TimeUnit.DAYS);
                    return BusinessUtils.success();
                }
                // 缓存未命中但数据库存在，继续处理
            } else if (Boolean.TRUE.equals(liked)) {
                // 缓存显示已点赞，继续处理
                existsInDb = true;
            }

            // 删除点赞记录
            int deleteResult = commentLikeMapper.deleteByCommentIdAndUserId(commentId, userId);
            if (deleteResult <= 0) {
                log.error("取消评论点赞失败：数据库删除失败，评论ID：{}，用户ID：{}", commentId, userId);
                throw new RuntimeException("取消评论点赞失败，事务回滚");
            }

            // 更新评论点赞数
            Integer updateResult = commentMapper.decrementLikeCount(commentId);
            if (updateResult == null || updateResult <= 0) {
                log.warn("更新评论点赞数失败，评论ID：{}，用户ID：{}", commentId, userId);
            }

            // 使用事务同步机制，在事务成功提交后再更新缓存
            final String finalLikeCacheKey = likeCacheKey;
            final String finalCommentDetailKey = commentDetailKey;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cacheUtils.deleteCacheWithDoubleDelete(finalLikeCacheKey);
                    cacheUtils.deleteCacheAsync(finalCommentDetailKey);
                    log.info("取消评论点赞成功，已更新缓存，评论ID：{}，用户ID：{}", commentId, userId);
                }

                @Override
                public void afterCompletion(int status) {
                    if (status != TransactionSynchronization.STATUS_COMMITTED) {
                        log.warn("取消评论点赞事务回滚，缓存未更新，评论ID：{}，用户ID：{}", commentId, userId);
                    }
                }
            });

            log.info("取消评论点赞成功（待事务提交），评论ID：{}，用户ID：{}", commentId, userId);
            return BusinessUtils.success();
        } catch (Exception e) {
            log.error("取消评论点赞失败", e);
            throw e; // 重新抛出异常以触发事务回滚
        } finally {
            // 释放分布式锁
            if (lockValue != null) {
                redisDistributedLock.unlock(lockKey, lockValue);
            }
        }
    }

    @Override
    public Result<Boolean> checkCommentLikeStatus(Long commentId, Long userId) {
        try {
            // 尝试从缓存获取
            String likeCacheKey = RedisCacheUtils.generateCommentLikeKey(commentId, userId);
            Boolean liked = (Boolean) redisCacheUtils.getCache(likeCacheKey);

            if (liked != null) {
                return BusinessUtils.success(liked);
            }

            // 从数据库查询
            boolean exists = commentLikeMapper.checkUserLikedComment(commentId, userId);

            // 缓存结果，有效期7天
            redisCacheUtils.setCache(likeCacheKey, exists, 7, TimeUnit.DAYS);

            return BusinessUtils.success(exists);
        } catch (Exception e) {
            log.error("检查评论点赞状态失败", e);
            return BusinessUtils.error("检查评论点赞状态失败");
        }
    }

    @Override
    public Result<Map<Long, Boolean>> batchCheckCommentLikeStatus(List<Long> commentIds, Long userId) {
        try {
            Map<Long, Boolean> result = new HashMap<>();

            // 如果用户未登录或评论列表为空，返回空结果
            if (userId == null || commentIds == null || commentIds.isEmpty()) {
                return BusinessUtils.success(result);
            }

            // 批量查询数据库获取已点赞的评论ID列表
            List<Long> likedCommentIds = commentLikeMapper.batchCheckUserLikedComments(commentIds, userId);

            // 将已点赞的评论ID列表转换为Set以提高查找效率
            java.util.Set<Long> likedSet = new java.util.HashSet<>(likedCommentIds);

            // 构建结果Map：每个评论ID对应其点赞状态
            for (Long commentId : commentIds) {
                boolean isLiked = likedSet.contains(commentId);
                result.put(commentId, isLiked);

                // 缓存单个评论的点赞状态（用于后续的单个查询）
                String likeCacheKey = RedisCacheUtils.generateCommentLikeKey(commentId, userId);
                redisCacheUtils.setCache(likeCacheKey, isLiked, 7, TimeUnit.DAYS);
            }

            return BusinessUtils.success(result);
        } catch (Exception e) {
            log.error("批量检查评论点赞状态失败", e);
            return BusinessUtils.error("批量检查评论点赞状态失败");
        }
    }

    @Override
    public Result<List<CommentDTO>> getHotComments(Long articleId, Integer limit) {
        try {
            // 尝试从缓存获取
            String cacheKey = RedisCacheUtils.generateHotCommentsKey(articleId);
            Object cachedData = redisCacheUtils.getCache(cacheKey);
            if (cachedData != null) {
                List<CommentDTO> commentDTOs = (List<CommentDTO>) cachedData;
                return BusinessUtils.success(commentDTOs);
            }

            // 查询热门评论（按点赞数排序）
            List<Comment> comments = commentMapper.selectCommentsByArticleId(articleId, 2);
            List<CommentDTO> commentDTOs = PageUtils.convertList(comments, this::convertToDTO);

            // 按点赞数排序，取前limit条
            List<CommentDTO> hotComments = commentDTOs.stream()
                    .sorted((c1, c2) -> Integer.compare(c2.getLikeCount(), c1.getLikeCount()))
                    .limit(limit)
                    .collect(Collectors.toList());

            // 缓存结果，有效期30分钟
            redisCacheUtils.setCache(cacheKey, hotComments, 30, TimeUnit.MINUTES);

            return BusinessUtils.success(hotComments);
        } catch (Exception e) {
            log.error("获取热门评论失败", e);
            return BusinessUtils.error("获取热门评论失败");
        }
    }

    @Override
    public Result<Boolean> checkSensitiveWords(String content) {
        try {
            boolean contains = sensitiveWordFilter.containsSensitiveWords(content);
            return BusinessUtils.success(contains);
        } catch (Exception e) {
            log.error("检测敏感词失败", e);
            return BusinessUtils.error("检测敏感词失败");
        }
    }

    @Override
    public Result<String> replaceSensitiveWords(String content) {
        try {
            String result = sensitiveWordFilter.replaceSensitiveWords(content);
            return BusinessUtils.success(result);
        } catch (Exception e) {
            log.error("替换敏感词失败", e);
            return BusinessUtils.error("替换敏感词失败");
        }
    }

    @Override
    public Result<List<CommentDTO>> getChildComments(Long parentId, Integer page, Integer size) {
        try {
            // 设置默认值
            page = PageUtils.getValidPage(page);
            size = PageUtils.getValidSize(size);

            // 计算偏移量
            int offset = PageUtils.calculateOffset(page, size);

            // 查询子评论
            List<Comment> comments = commentMapper.selectChildCommentsByParentIds(List.of(parentId), 2);
            List<CommentDTO> commentDTOs = PageUtils.convertList(comments, this::convertToDTO);

            // 分页处理
            int start = Math.min(offset, commentDTOs.size());
            int end = Math.min(offset + size, commentDTOs.size());
            List<CommentDTO> paginatedComments = commentDTOs.subList(start, end);

            return BusinessUtils.success(paginatedComments);
        } catch (Exception e) {
            log.error("获取子评论失败", e);
            return BusinessUtils.error("获取子评论失败");
        }
    }

    private CommentDTO convertToDTO(Comment comment) {
        CommentDTO commentDTO = new CommentDTO();
        BeanUtils.copyProperties(comment, commentDTO);
        // 默认设置为未点赞
        commentDTO.setLiked(false);
        return commentDTO;
    }

    /**
     * 使用预获取的点赞状态映射转换评论为 DTO（避免 N+1 查询）
     */
    private CommentDTO convertToDTOWithLikeStatus(Comment comment, Map<Long, Boolean> likeStatusMap) {
        CommentDTO commentDTO = convertToDTO(comment);
        // 从批量查询的结果中获取点赞状态
        if (likeStatusMap != null && likeStatusMap.containsKey(comment.getId())) {
            commentDTO.setLiked(likeStatusMap.get(comment.getId()));
        }
        return commentDTO;
    }

    private CommentDTO convertToDTO(Comment comment, Long userId) {
        CommentDTO commentDTO = convertToDTO(comment);
        if (userId != null) {
            // 检查当前用户是否已点赞
            String likeCacheKey = RedisCacheUtils.generateCommentLikeKey(comment.getId(), userId);
            Boolean liked = (Boolean) redisCacheUtils.getCache(likeCacheKey);
            if (liked != null) {
                commentDTO.setLiked(liked);
            } else {
                boolean exists = commentLikeMapper.checkUserLikedComment(comment.getId(), userId);
                commentDTO.setLiked(exists);
                // 缓存结果，有效期7天
                redisCacheUtils.setCache(likeCacheKey, exists, 7, TimeUnit.DAYS);
            }
        }
        return commentDTO;
    }

    /**
     * 发送评论通知（异步事件发布）
     * 不再阻塞主线程，通知将在独立的事务后异步创建
     */
    private void sendCommentNotification(Comment comment) {
        try {
            // 获取文章作者ID
            Article article = articleMapper.selectById(comment.getArticleId());
            if (article == null) {
                return;
            }
            Long articleAuthorId = article.getAuthorId();

            // 如果是回复评论，获取被回复者ID
            Long replyUserId = null;
            if (comment.getParentId() != null && comment.getParentId() > 0) {
                Comment parentComment = commentMapper.selectById(comment.getParentId());
                if (parentComment != null) {
                    replyUserId = parentComment.getUserId();
                }
            }

            // 发布事件：发送通知给文章作者
            if (articleAuthorId != null && !Objects.equals(articleAuthorId, comment.getUserId())) {
                String content = "评论了你的文章";
                eventPublisher.publishEvent(new NotificationEvent(
                        this,
                        articleAuthorId,
                        comment.getUserId(),
                        Notification.TYPE_ARTICLE_COMMENT,
                        comment.getArticleId(),
                        Notification.TARGET_TYPE_ARTICLE,
                        content));
                log.debug("已发布文章评论通知事件: articleId={}, authorId={}, commenterId={}",
                        comment.getArticleId(), articleAuthorId, comment.getUserId());
            }

            // 发布事件：发送通知给被回复者
            if (replyUserId != null && !Objects.equals(replyUserId, comment.getUserId())) {
                String content = "回复了你的评论";
                eventPublisher.publishEvent(new NotificationEvent(
                        this,
                        replyUserId,
                        comment.getUserId(),
                        Notification.TYPE_COMMENT_REPLY,
                        comment.getId(),
                        Notification.TARGET_TYPE_COMMENT,
                        content));
                log.debug("已发布评论回复通知事件: commentId={}, replyUserId={}, replierId={}",
                        comment.getId(), replyUserId, comment.getUserId());
            }
        } catch (Exception e) {
            // 事件发布失败不应影响主业务，仅记录日志
            log.error("发布评论通知事件失败", e);
        }
    }

    /**
     * 发送点赞通知（异步事件发布）
     * 不再阻塞主线程，通知将在独立的事务后异步创建
     */
    private void sendLikeNotification(Long commentId, Long userId) {
        try {
            // 获取评论作者ID
            Comment comment = commentMapper.selectById(commentId);
            if (comment == null) {
                return;
            }

            // 不给自己发送通知
            if (Objects.equals(comment.getUserId(), userId)) {
                return;
            }

            // 发布事件：创建通知
            String content = "点赞了你的评论";
            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    comment.getUserId(),
                    userId,
                    Notification.TYPE_COMMENT_LIKE,
                    commentId,
                    Notification.TARGET_TYPE_COMMENT,
                    content));
            log.debug("已发布评论点赞通知事件: commentId={}, commentAuthorId={}, likerId={}",
                    commentId, comment.getUserId(), userId);
        } catch (Exception e) {
            // 事件发布失败不应影响主业务，仅记录日志
            log.error("发布评论点赞通知事件失败", e);
        }
    }

    /**
     * 清除文章相关的评论缓存
     */
    private void clearCommentCache(Long articleId) {
        // 清除评论计数缓存
        String countCacheKey = RedisCacheUtils.generateCommentCountKey(articleId);
        redisCacheUtils.deleteCache(countCacheKey);

        // 清除热门评论缓存
        String hotCacheKey = RedisCacheUtils.generateHotCommentsKey(articleId);
        redisCacheUtils.deleteCache(hotCacheKey);

        // 清除评论列表缓存（这里简化处理，实际应使用通配符删除）
        // 注意：生产环境中应使用Redis的SCAN命令来查找并删除相关缓存
    }
}
