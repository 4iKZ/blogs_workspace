package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.UserLike;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface UserLikeMapper extends BaseMapper<UserLike> {

        /**
         * 根据用户ID和文章ID查询点赞记录
         */
        @Select("SELECT * FROM user_likes " +
                        "WHERE user_id = #{userId} AND target_id = #{articleId} AND target_type = 1 " +
                        "LIMIT 1")
        UserLike findByUserIdAndArticleId(@Param("userId") Long userId, @Param("articleId") Long articleId);

        /**
         * 根据文章ID查询点赞用户列表
         */
        @Select("SELECT user_id FROM user_likes " +
                        "WHERE target_id = #{articleId} AND target_type = 1 " +
                        "ORDER BY create_time DESC")
        List<Long> findUserIdsByArticleId(@Param("articleId") Long articleId);

        /**
         * 根据用户ID查询点赞的文章ID列表
         */
        @Select("SELECT target_id FROM user_likes " +
                        "WHERE user_id = #{userId} AND target_type = 1 " +
                        "ORDER BY create_time DESC")
        List<Long> findArticleIdsByUserId(@Param("userId") Long userId);

        /**
         * 根据文章ID统计点赞数
         */
        @Select("SELECT COUNT(1) FROM user_likes " +
                        "WHERE target_id = #{articleId} AND target_type = 1")
        Long countByArticleId(@Param("articleId") Long articleId);

        /**
         * 根据用户ID统计点赞数
         */
        @Select("SELECT COUNT(1) FROM user_likes " +
                        "WHERE user_id = #{userId} AND target_type = 1")
        Long countByUserId(@Param("userId") Long userId);

        /**
         * 批量删除点赞记录
         */
        @Delete("<script>" +
                        "DELETE FROM user_likes WHERE id IN " +
                        "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
                        "#{id}" +
                        "</foreach>" +
                        "</script>")
        int deleteBatchByIds(@Param("ids") List<Long> ids);

        /**
         * 根据用户ID和文章ID删除点赞记录
         * 
         * @param userId    用户ID
         * @param articleId 文章ID
         * @return 影响行数
         */
        @Delete("DELETE FROM user_likes " +
                        "WHERE user_id = #{userId} AND target_id = #{articleId} AND target_type = 1")
        int deleteByUserIdAndArticleId(@Param("userId") Long userId, @Param("articleId") Long articleId);

        @Delete("DELETE FROM user_likes WHERE target_id = #{articleId} AND target_type = 1")
        int deleteByArticleId(@Param("articleId") Long articleId);

        /**
         * 分页查询用户的点赞记录
         * 
         * @param userId 用户ID
         * @param offset 偏移量
         * @param size   查询数量
         * @return 点赞记录列表
         */
        @Select("SELECT ul.*, a.title, a.summary, a.cover_image, a.publish_time, u.nickname as author_name " +
                        "FROM user_likes ul " +
                        "LEFT JOIN articles a ON ul.target_id = a.id " +
                        "LEFT JOIN users u ON a.author_id = u.id " +
                        "WHERE ul.user_id = #{userId} AND ul.target_type = 1 AND a.status = 2 " +
                        "ORDER BY ul.create_time DESC " +
                        "LIMIT #{offset}, #{size}")
        List<UserLike> selectByUserId(@Param("userId") Long userId, @Param("offset") Integer offset,
                        @Param("size") Integer size);

        /**
         * 统计用户对指定文章的点赞数量
         * 
         * @param userId    用户ID
         * @param articleId 文章ID
         * @return 点赞数量
         */
        @Select("SELECT COUNT(1) FROM user_likes " +
                        "WHERE user_id = #{userId} AND target_id = #{articleId} AND target_type = 1")
        int countByUserIdAndArticleId(@Param("userId") Long userId, @Param("articleId") Long articleId);

        /**
         * 批量查询用户对哪些文章点赞了
         * 
         * @param userId     用户ID
         * @param articleIds 文章ID列表
         * @return 已点赞的文章ID列表
         */
        @Select("<script>" +
                        "SELECT target_id FROM user_likes " +
                        "WHERE user_id = #{userId} AND target_type = 1 " +
                        "AND target_id IN " +
                        "<foreach collection='articleIds' item='id' open='(' separator=',' close=')'>" +
                        "#{id}" +
                        "</foreach>" +
                        "</script>")
        List<Long> findLikedArticleIdsByUserIdAndArticleIds(
                        @Param("userId") Long userId,
                        @Param("articleIds") Collection<Long> articleIds);

        /**
         * 查询最近的点赞记录（用于缓存一致性验证）
         * 
         * @param limit 查询数量
         * @return 点赞记录列表
         */
        @Select("SELECT * FROM user_likes WHERE target_type = 1 ORDER BY create_time DESC LIMIT #{limit}")
        List<UserLike> selectRecentRecords(@Param("limit") int limit);

}
