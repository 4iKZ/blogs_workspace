package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章Mapper接口
 */
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    /**
     * 分页查询已发布的文章
     * @param page 分页对象
     * @param status 文章状态
     * @param categoryId 分类ID（可选）
     * @param tagId 标签ID（可选）
     * @param keyword 关键词（可选）
     * @return 分页文章列表
     */
    @Select({"<script>",
            "SELECT a.*, u.nickname as author_name, c.name as category_name ",
            "FROM articles a ",
            "LEFT JOIN users u ON a.author_id = u.id ",
            "LEFT JOIN categories c ON a.category_id = c.id ",
            "WHERE a.status = #{status} ",
            "<if test=\"categoryId != null\">AND a.category_id = #{categoryId}</if>",
            "<if test=\"tagId != null\">",
            "AND a.id IN (SELECT article_id FROM article_tags WHERE tag_id = #{tagId}) ",
            "</if>",
            "<if test=\"keyword != null and keyword != ''\">",
            "AND (a.title LIKE CONCAT('%', #{keyword}, '%') OR a.summary LIKE CONCAT('%', #{keyword}, '%')) ",
            "</if>",
            "ORDER BY a.is_top DESC, a.publish_time DESC",
            "</script>"})
    IPage<Article> selectPublishedArticles(Page<Article> page, 
                                         @Param("status") Integer status,
                                         @Param("categoryId") Long categoryId,
                                         @Param("tagId") Long tagId,
                                         @Param("keyword") String keyword);

    /**
     * 查询置顶文章
     * @param limit 查询数量
     * @return 置顶文章列表
     */
    @Select("SELECT a.*, u.nickname as author_name, c.name as category_name " +
            "FROM articles a " +
            "LEFT JOIN users u ON a.author_id = u.id " +
            "LEFT JOIN categories c ON a.category_id = c.id " +
            "WHERE a.status = 2 AND a.is_top = 2 " +
            "ORDER BY a.publish_time DESC LIMIT #{limit}")
    List<Article> selectTopArticles(@Param("limit") Integer limit);

    /**
     * 查询推荐文章
     * @param limit 查询数量
     * @return 推荐文章列表
     */
    @Select("SELECT a.*, u.nickname as author_name, c.name as category_name " +
            "FROM articles a " +
            "LEFT JOIN users u ON a.author_id = u.id " +
            "LEFT JOIN categories c ON a.category_id = c.id " +
            "WHERE a.status = 2 AND a.is_recommend = 2 " +
            "ORDER BY a.publish_time DESC LIMIT #{limit}")
    List<Article> selectRecommendedArticles(@Param("limit") Integer limit);

    /**
     * 查询热门文章（按浏览量排序）
     * @param limit 查询数量
     * @return 热门文章列表
     */
    @Select("SELECT a.*, u.nickname as author_name, c.name as category_name " +
            "FROM articles a " +
            "LEFT JOIN users u ON a.author_id = u.id " +
            "LEFT JOIN categories c ON a.category_id = c.id " +
            "WHERE a.status = 2 " +
            "ORDER BY a.view_count DESC, a.publish_time DESC LIMIT #{limit}")
    List<Article> selectHotArticles(@Param("limit") Integer limit);

    /**
     * 查询用户的文章
     * @param userId 用户ID
     * @param status 文章状态（可选）
     * @return 文章列表
     */
    @Select({"<script>",
            "SELECT a.*, c.name as category_name ",
            "FROM articles a ",
            "LEFT JOIN categories c ON a.category_id = c.id ",
            "WHERE a.author_id = #{userId} ",
            "<if test=\"status != null\">AND a.status = #{status}</if>",
            "ORDER BY a.create_time DESC ",
            "</script>"})
    List<Article> selectArticlesByUserId(@Param("userId") Long userId, @Param("status") Integer status);

    /**
     * 增加文章浏览量
     * @param articleId 文章ID
     * @return 影响行数
     */
    @Update("UPDATE articles SET view_count = view_count + 1, update_time = NOW() WHERE id = #{articleId}")
    int incrementViewCount(@Param("articleId") Long articleId);

    /**
     * 批量增加文章浏览量
     * @param articleId 文章ID
     * @param increment 增量
     * @return 影响行数
     */
    @Update("UPDATE articles SET view_count = view_count + #{increment}, update_time = NOW() WHERE id = #{articleId}")
    int incrementViewCountBatch(@Param("articleId") Long articleId, @Param("increment") int increment);

    /**
     * 更新文章点赞数
     * @param articleId 文章ID
     * @param increment 增量（正数增加，负数减少）
     * @return 影响行数
     */
    @Update("UPDATE articles SET like_count = like_count + #{increment}, update_time = NOW() WHERE id = #{articleId}")
    int updateLikeCount(@Param("articleId") Long articleId, @Param("increment") Integer increment);

    /**
     * 安全减少文章点赞数（使用GREATEST防止负数）
     * @param articleId 文章ID
     * @return 影响行数
     */
    @Update("UPDATE articles SET like_count = GREATEST(0, like_count - 1), update_time = NOW() WHERE id = #{articleId} AND like_count > 0")
    int decrementLikeCountSafely(@Param("articleId") Long articleId);

    /**
     * 更新文章评论数
     * @param articleId 文章ID
     * @param increment 增量（正数增加，负数减少）
     * @return 影响行数
     */
    @Update("UPDATE articles SET comment_count = comment_count + #{increment}, update_time = NOW() WHERE id = #{articleId}")
    int updateCommentCount(@Param("articleId") Long articleId, @Param("increment") Integer increment);

    /**
     * 安全减少文章评论数（使用GREATEST防止负数）
     * @param articleId 文章ID
     * @return 影响行数
     */
    @Update("UPDATE articles SET comment_count = GREATEST(0, comment_count - 1), update_time = NOW() WHERE id = #{articleId} AND comment_count > 0")
    int decrementCommentCountSafely(@Param("articleId") Long articleId);

    /**
     * 更新文章收藏数
     * @param articleId 文章ID
     * @param increment 增量（正数增加，负数减少）
     * @return 影响行数
     */
    @Update("UPDATE articles SET favorite_count = favorite_count + #{increment}, update_time = NOW() WHERE id = #{articleId}")
    int updateFavoriteCount(@Param("articleId") Long articleId, @Param("increment") Integer increment);

    /**
     * 安全减少文章收藏数（使用GREATEST防止负数）
     * @param articleId 文章ID
     * @return 影响行数
     */
    @Update("UPDATE articles SET favorite_count = GREATEST(0, favorite_count - 1), update_time = NOW() WHERE id = #{articleId} AND favorite_count > 0")
    int decrementFavoriteCountSafely(@Param("articleId") Long articleId);
    
    /**
     * 统计文章总数
     * @return 文章总数
     */
    @Select("SELECT COUNT(1) FROM articles")
    int countTotalArticles();
    
    /**
     * 统计已发布文章数
     * @return 已发布文章数
     */
    @Select("SELECT COUNT(1) FROM articles WHERE status = 2")
    int countPublishedArticles();
    
    /**
     * 统计今日新增文章数
     * @return 今日新增文章数
     */
    @Select("SELECT COUNT(1) FROM articles WHERE DATE(create_time) = CURDATE()")
    int countNewArticlesToday();
    
    /**
     * 分页查询文章列表
     * @param offset 偏移量
     * @param size 查询数量
     * @param keyword 关键词（可选）
     * @param status 状态（可选）
     * @return 文章列表
     */
    @Select({"<script>",
            "SELECT * FROM articles ",
            "WHERE 1=1 ",
            "<if test=\"keyword != null and keyword != ''\">",
            "AND (title LIKE CONCAT('%', #{keyword}, '%') OR summary LIKE CONCAT('%', #{keyword}, '%')) ",
            "</if>",
            "<if test=\"status != null\">AND status = #{status}</if>",
            "ORDER BY create_time DESC ",
            "LIMIT #{offset}, #{size} ",
            "</script>"})
    List<com.blog.dto.ArticleDTO> selectArticleList(@Param("offset") Integer offset, 
                                                  @Param("size") Integer size, 
                                                  @Param("keyword") String keyword, 
                                                  @Param("status") Integer status);
    
    /**
     * 更新文章状态
     * @param articleId 文章ID
     * @param status 状态
     * @return 影响行数
     */
    @Update("UPDATE articles SET status = #{status}, update_time = NOW() WHERE id = #{articleId}")
    int updateStatus(@Param("articleId") Long articleId, @Param("status") Integer status);
    

    
    /**
     * 根据关键词搜索文章
     * @param keyword 关键词
     * @param offset 偏移量
     * @param size 查询数量
     * @return 文章列表
     */
    @Select("SELECT * FROM articles WHERE status = 2 AND " +
            "(title LIKE CONCAT('%', #{keyword}, '%') OR content LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY create_time DESC LIMIT #{offset}, #{size}")
    List<Article> searchByKeyword(@Param("keyword") String keyword, @Param("offset") Integer offset, @Param("size") Integer size);
    
    /**
     * 根据分类ID查询文章
     * @param categoryId 分类ID
     * @param offset 偏移量
     * @param size 查询数量
     * @return 文章列表
     */
    @Select("SELECT * FROM articles WHERE status = 2 AND category_id = #{categoryId} " +
            "ORDER BY create_time DESC LIMIT #{offset}, #{size}")
    List<Article> selectByCategoryId(@Param("categoryId") Long categoryId, @Param("offset") Integer offset, @Param("size") Integer size);
    
    /**
     * 根据标签ID查询文章
     * @param tagId 标签ID
     * @param offset 偏移量
     * @param size 查询数量
     * @return 文章列表
     */
    @Select("SELECT a.* FROM articles a " +
            "LEFT JOIN article_tags at ON a.id = at.article_id " +
            "WHERE a.status = 2 AND at.tag_id = #{tagId} " +
            "ORDER BY a.create_time DESC LIMIT #{offset}, #{size}")
    List<Article> selectByTagId(@Param("tagId") Long tagId, @Param("offset") Integer offset, @Param("size") Integer size);
    
    /**
     * 根据作者ID查询文章
     * @param authorId 作者ID
     * @param offset 偏移量
     * @param size 查询数量
     * @return 文章列表
     */
    @Select("SELECT * FROM articles WHERE status = 2 AND author_id = #{authorId} " +
            "ORDER BY create_time DESC LIMIT #{offset}, #{size}")
    List<Article> selectByAuthorId(@Param("authorId") Long authorId, @Param("offset") Integer offset, @Param("size") Integer size);
    
    /**
     * 高级搜索文章
     * @param keyword 关键词
     * @param categoryId 分类ID
     * @param tagId 标签ID
     * @param authorId 作者ID
     * @param searchScope 搜索范围
     * @param sortBy 排序方式
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param offset 偏移量
     * @param size 查询数量
     * @return 文章列表
     */
    @Select({"<script>",
            "SELECT a.* FROM articles a ",
            "LEFT JOIN article_tags at ON a.id = at.article_id ",
            "WHERE a.status = 2 ",
            "<if test=\"keyword != null and keyword != ''\">",
            "<choose>",
            "<when test=\"searchScope == 'title'\">",
            "AND a.title LIKE CONCAT('%', #{keyword}, '%') ",
            "</when>",
            "<when test=\"searchScope == 'content'\">",
            "AND a.content LIKE CONCAT('%', #{keyword}, '%') ",
            "</when>",
            "<otherwise>",
            "AND (a.title LIKE CONCAT('%', #{keyword}, '%') OR a.content LIKE CONCAT('%', #{keyword}, '%')) ",
            "</otherwise>",
            "</choose>",
            "</if>",
            "<if test=\"categoryId != null\">",
            "AND a.category_id = #{categoryId} ",
            "</if>",
            "<if test=\"tagId != null\">",
            "AND at.tag_id = #{tagId} ",
            "</if>",
            "<if test=\"authorId != null\">",
            "AND a.author_id = #{authorId} ",
            "</if>",
            "<if test=\"startDate != null\">",
            "AND a.create_time &gt;= #{startDate} ",
            "</if>",
            "<if test=\"endDate != null\">",
            "AND a.create_time &lt;= #{endDate} ",
            "</if>",
            "GROUP BY a.id ",
            "<choose>",
            "<when test=\"sortBy == 'relevance'\">",
            "ORDER BY (CASE WHEN a.title LIKE CONCAT('%', #{keyword}, '%') THEN 1 ELSE 0 END) DESC, a.create_time DESC ",
            "</when>",
            "<when test=\"sortBy == 'view'\">",
            "ORDER BY a.view_count DESC, a.create_time DESC ",
            "</when>",
            "<otherwise>",
            "ORDER BY a.create_time DESC ",
            "</otherwise>",
            "</choose>",
            "LIMIT #{offset}, #{size} ",
            "</script>"})
    List<Article> advancedSearch(@Param("keyword") String keyword, 
                                @Param("categoryId") Long categoryId, 
                                @Param("tagId") Long tagId, 
                                @Param("authorId") Long authorId, 
                                @Param("searchScope") String searchScope, 
                                @Param("sortBy") String sortBy, 
                                @Param("startDate") LocalDateTime startDate, 
                                @Param("endDate") LocalDateTime endDate, 
                                @Param("offset") Integer offset, 
                                @Param("size") Integer size);
    
    /**
     * 获取搜索建议
     * @param keyword 关键词
     * @return 搜索建议列表
     */
    @Select("SELECT DISTINCT title FROM articles " +
            "WHERE status = 2 AND title LIKE CONCAT(#{keyword}, '%') " +
            "ORDER BY view_count DESC LIMIT 10")
    List<String> getSearchSuggestions(@Param("keyword") String keyword);
    
    /**
     * 获取热门搜索关键词
     * @param limit 数量限制
     * @return 热门关键词列表
     */
    @Select("SELECT title FROM articles " +
            "WHERE status = 2 " +
            "ORDER BY view_count DESC " +
            "LIMIT #{limit}")
    List<String> getHotSearchKeywords(@Param("limit") Integer limit);

    /**
     * 在指定时间范围内查询热门文章（按浏览量与点赞数排序）
     * @param limit 查询数量
     * @param start 开始时间（包含）
     * @param end 结束时间（包含）
     * @return 热门文章列表
     */
    @Select("SELECT a.*, u.nickname as author_name, c.name as category_name " +
            "FROM articles a " +
            "LEFT JOIN users u ON a.author_id = u.id " +
            "LEFT JOIN categories c ON a.category_id = c.id " +
            "WHERE a.status = 2 AND a.publish_time BETWEEN #{start} AND #{end} " +
            "ORDER BY a.view_count DESC, a.like_count DESC, a.publish_time DESC LIMIT #{limit}")
    List<Article> selectHotArticlesByRange(@Param("limit") Integer limit,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);
}
