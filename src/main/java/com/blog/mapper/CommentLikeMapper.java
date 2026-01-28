package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.CommentLike;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 评论点赞Mapper接口
 */
@Mapper
public interface CommentLikeMapper extends BaseMapper<CommentLike> {

    /**
     * 检查用户是否已点赞评论
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 是否已点赞
     */
    @Select("SELECT COUNT(1) > 0 FROM comment_likes WHERE comment_id = #{commentId} AND user_id = #{userId}")
    boolean checkUserLikedComment(@Param("commentId") Long commentId, @Param("userId") Long userId);

    /**
     * 获取评论的点赞数
     * @param commentId 评论ID
     * @return 点赞数
     */
    @Select("SELECT COUNT(*) FROM comment_likes WHERE comment_id = #{commentId}")
    Integer getCommentLikeCount(@Param("commentId") Long commentId);

    /**
     * 根据评论ID删除点赞记录
     * @param commentId 评论ID
     * @return 影响行数
     */
    @Delete("DELETE FROM comment_likes WHERE comment_id = #{commentId}")
    int deleteByCommentId(@Param("commentId") Long commentId);

    /**
     * 根据用户ID删除点赞记录
     * @param userId 用户ID
     * @return 影响行数
     */
    @Delete("DELETE FROM comment_likes WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 根据评论ID和用户ID删除点赞记录
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 影响行数
     */
    @Delete("DELETE FROM comment_likes WHERE comment_id = #{commentId} AND user_id = #{userId}")
    int deleteByCommentIdAndUserId(@Param("commentId") Long commentId, @Param("userId") Long userId);

    /**
     * 批量检查用户对多个评论的点赞状态
     * @param commentIds 评论ID列表
     * @param userId 用户ID
     * @return 已点赞的评论ID列表
     */
    @Select("<script>" +
            "SELECT comment_id FROM comment_likes " +
            "WHERE user_id = #{userId} " +
            "AND comment_id IN " +
            "<foreach collection='commentIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Long> batchCheckUserLikedComments(@Param("commentIds") List<Long> commentIds, @Param("userId") Long userId);
}
