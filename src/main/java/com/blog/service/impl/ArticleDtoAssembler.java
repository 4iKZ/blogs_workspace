package com.blog.service.impl;

import com.blog.dto.ArticleDTO;
import com.blog.dto.CategoryDTO;
import com.blog.entity.Article;
import com.blog.entity.Category;
import com.blog.entity.User;
import com.blog.mapper.CategoryMapper;
import com.blog.mapper.UserFavoriteMapper;
import com.blog.mapper.UserLikeMapper;
import com.blog.mapper.UserMapper;
import com.blog.utils.AuthUtils;
import com.blog.utils.RedisCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文章DTO组装组件
 */
@Component
@Slf4j
public class ArticleDtoAssembler {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private UserLikeMapper userLikeMapper;

    @Autowired
    private UserFavoriteMapper userFavoriteMapper;

    @Autowired
    private RedisCacheUtils redisCacheUtils;

    public ArticleDTO convertToDTO(Article article) {
        List<ArticleDTO> list = batchConvertToDTO(Collections.singletonList(article));
        return list.isEmpty() ? new ArticleDTO() : list.get(0);
    }

    /**
     * 批量转换文章为DTO（优化N+1查询）
     * 
     * @param articles 文章列表
     * @return ArticleDTO列表
     */
    public List<ArticleDTO> batchConvertToDTO(List<Article> articles) {
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
                        userLikeMapper.findLikedArticleIdsByUserIdAndArticleIds(currentUserId, articleIds));
                // 批量查询收藏状态
                favoritedArticleIds = new HashSet<>(
                        userFavoriteMapper.findFavoritedArticleIdsByUserIdAndArticleIds(currentUserId, articleIds));
            }
        } catch (Exception e) {
            log.debug("未登录或无法获取用户ID，跳过点赞/收藏状态查询");
        }

        // 批量获取 Redis 浏览量增量
        Map<Long, Integer> redisViewCountMap = redisCacheUtils.batchGetArticleRedisViewCount(articleIds);

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

            // 合并 Redis 中尚未同步到 DB 的浏览量增量
            int dbViewCount = article.getViewCount() != null ? article.getViewCount() : 0;
            int redisViewCount = redisViewCountMap.getOrDefault(article.getId(), 0);
            dto.setViewCount(dbViewCount + redisViewCount);

            result.add(dto);
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("批量转换文章DTO完成，数量：{}，耗时：{} ms", result.size(), duration);

        return result;
    }
}
