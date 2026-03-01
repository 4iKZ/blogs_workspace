package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.UserFavorite;
import org.apache.ibatis.annotations.*;

import java.util.Collection;
import java.util.List;

/**
 * 用户收藏Mapper接口
 */
@Mapper
public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {

    /**
     * 查询用户的收藏文章
     * @param userId 用户ID
     * @return 收藏列表
     */
    @Select("SELECT uf.*, a.title, a.summary, a.cover_image, a.publish_time, u.nickname as author_name " +
            "FROM user_favorites uf " +
            "LEFT JOIN articles a ON uf.article_id = a.id " +
            "LEFT JOIN users u ON a.author_id = u.id " +
            "WHERE uf.user_id = #{userId} AND a.status = 2 " +
            "ORDER BY uf.create_time DESC")
    List<UserFavorite> selectUserFavorites(@Param("userId") Long userId);

    /**
     * 检查用户是否收藏了文章
     * @param userId 用户ID
     * @param articleId 文章ID
     * @return 收藏记录
     */
    @Select("SELECT * FROM user_favorites WHERE user_id = #{userId} AND article_id = #{articleId}")
    UserFavorite selectByUserAndArticle(@Param("userId") Long userId, @Param("articleId") Long articleId);

    /**
     * 统计用户的收藏数量
     * @param userId 用户ID
     * @return 收藏数量
     */
    @Select("SELECT COUNT(1) FROM user_favorites uf " +
            "LEFT JOIN articles a ON uf.article_id = a.id " +
            "WHERE uf.user_id = #{userId} AND a.status = 2")
    int countUserFavorites(@Param("userId") Long userId);

    /**
     * 统计文章的被收藏数量
     * @param articleId 文章ID
     * @return 收藏数量
     */
    @Select("SELECT COUNT(1) FROM user_favorites WHERE article_id = #{articleId}")
    int countArticleFavorites(@Param("articleId") Long articleId);

    /**
     * 批量取消收藏
     * @param userId 用户ID
     * @param articleIds 文章ID列表
     * @return 影响行数
     */
    @Delete("<script>" +
            "DELETE FROM user_favorites " +
            "WHERE user_id = #{userId} AND article_id IN " +
            "<foreach collection='articleIds' item='articleId' open='(' separator=',' close=')'>#{articleId}</foreach>" +
            "</script>")
    int batchDeleteFavorites(@Param("userId") Long userId, @Param("articleIds") List<Long> articleIds);
    
    /**
     * 根据用户ID和文章ID删除收藏记录
     * @param userId 用户ID
     * @param articleId 文章ID
     * @return 影响行数
     */
    @Delete("DELETE FROM user_favorites WHERE user_id = #{userId} AND article_id = #{articleId}")
    int deleteByUserIdAndArticleId(@Param("userId") Long userId, @Param("articleId") Long articleId);

    @Delete("DELETE FROM user_favorites WHERE article_id = #{articleId}")
    int deleteByArticleId(@Param("articleId") Long articleId);
    
    /**
     * 分页查询用户的收藏记录
     * @param userId 用户ID
     * @param offset 偏移量
     * @param size 查询数量
     * @return 收藏记录列表
     */
    @Select("SELECT uf.*, a.title, a.summary, a.cover_image, a.publish_time, u.nickname as author_name " +
            "FROM user_favorites uf " +
            "LEFT JOIN articles a ON uf.article_id = a.id " +
            "LEFT JOIN users u ON a.author_id = u.id " +
            "WHERE uf.user_id = #{userId} AND a.status = 2 " +
            "ORDER BY uf.create_time DESC " +
            "LIMIT #{offset}, #{size}")
    List<UserFavorite> selectByUserId(@Param("userId") Long userId, @Param("offset") Integer offset, @Param("size") Integer size);
    
    /**
     * 统计用户对指定文章的收藏数量
     * @param userId 用户ID
     * @param articleId 文章ID
     * @return 收藏数量
     */
    @Select("SELECT COUNT(1) FROM user_favorites " +
            "WHERE user_id = #{userId} AND article_id = #{articleId}")
    int countByUserIdAndArticleId(@Param("userId") Long userId, @Param("articleId") Long articleId);
    
    /**
     * 获取用户收藏的文章ID列表
     * @param userId 用户ID
     * @return 文章ID列表
     */
    @Select("SELECT article_id FROM user_favorites " +
            "WHERE user_id = #{userId} " +
            "ORDER BY create_time DESC")
    List<Long> findArticleIdsByUserId(@Param("userId") Long userId);
    
    /**
     * 统计用户的总收藏数量
     * @param userId 用户ID
     * @return 收藏数量
     */
    @Select("SELECT COUNT(1) FROM user_favorites uf " +
            "LEFT JOIN articles a ON uf.article_id = a.id " +
            "WHERE uf.user_id = #{userId} AND a.status = 2")
    int countByUserId(@Param("userId") Long userId);

    /**
     * 批量查询用户收藏了哪些文章
     * @param userId 用户ID
     * @param articleIds 文章ID列表
     * @return 已收藏的文章ID列表
     */
    @Select("<script>" +
            "SELECT article_id FROM user_favorites " +
            "WHERE user_id = #{userId} " +
            "AND article_id IN " +
            "<foreach collection='articleIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<Long> findFavoritedArticleIdsByUserIdAndArticleIds(
        @Param("userId") Long userId,
        @Param("articleIds") Collection<Long> articleIds
    );

    /**
     * 查询最近的收藏记录（用于缓存一致性验证）
     * 
     * @param limit 查询数量
     * @return 收藏记录列表
     */
    @Select("SELECT * FROM user_favorites ORDER BY create_time DESC LIMIT #{limit}")
    List<UserFavorite> selectRecentRecords(@Param("limit") int limit);
}
