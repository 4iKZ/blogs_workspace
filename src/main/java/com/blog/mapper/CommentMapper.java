package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.Comment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 评论Mapper接口
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    /**
     * 查询文章的评论（包含子评论）
     * @param articleId 文章ID
     * @param status 评论状态
     * @return 评论列表
     */
    @Select("<script>"
            + "SELECT c.*, u.nickname, u.avatar "
            + "FROM comments c "
            + "LEFT JOIN users u ON c.user_id = u.id "
            + "WHERE c.article_id = #{articleId} AND c.deleted = 0 "
            + "<if test='status != null'>AND c.status = #{status}</if> "
            + "ORDER BY c.create_time ASC"
            + "</script>")
    List<Comment> selectCommentsByArticleId(@Param("articleId") Long articleId, @Param("status") Integer status);

    /**
     * 查询文章的评论（包含子评论）- 分页
     * @param articleId 文章ID
     * @param status 评论状态
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 评论列表
     */
    @Select("<script>"
            + "SELECT c.*, u.nickname, u.avatar "
            + "FROM comments c "
            + "LEFT JOIN users u ON c.user_id = u.id "
            + "WHERE c.article_id = #{articleId} AND c.deleted = 0 "
            + "<if test='status != null'>AND c.status = #{status}</if> "
            + "ORDER BY c.create_time ASC "
            + "LIMIT #{offset}, #{limit}"
            + "</script>")
    List<Comment> selectCommentsByArticleIdWithPagination(@Param("articleId") Long articleId, @Param("status") Integer status, @Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * 查询顶级评论（不包含子评论）
     * @param articleId 文章ID
     * @param status 评论状态
     * @return 评论列表
     */
    @Select("SELECT c.*, u.nickname, u.avatar " +
            "FROM comments c " +
            "LEFT JOIN users u ON c.user_id = u.id " +
            "WHERE c.article_id = #{articleId} AND c.parent_id = 0 AND c.status = #{status} AND c.deleted = 0 " +
            "ORDER BY c.create_time ASC")
    List<Comment> selectTopLevelComments(@Param("articleId") Long articleId, @Param("status") Integer status);

    /**
     * 查询子评论
     * @param parentId 父评论ID
     * @param status 评论状态
     * @return 子评论列表
     */
    @Select("SELECT c.*, u.nickname, u.avatar " +
            "FROM comments c " +
            "LEFT JOIN users u ON c.user_id = u.id " +
            "WHERE c.parent_id = #{parentId} AND c.status = #{status} AND c.deleted = 0 " +
            "ORDER BY c.create_time ASC")
    List<Comment> selectChildComments(@Param("parentId") Long parentId, @Param("status") Integer status);

    /**
     * 查询用户的评论
     * @param userId 用户ID
     * @param status 评论状态（可选）
     * @return 评论列表
     */
    @Select("<script>" +
            "SELECT c.*, a.title as article_title " +
            "FROM comments c " +
            "LEFT JOIN articles a ON c.article_id = a.id " +
            "WHERE c.user_id = #{userId} AND c.deleted = 0 " +
            "<if test='status != null'>AND c.status = #{status}</if>" +
            "ORDER BY c.create_time DESC" +
            "</script>")
    List<Comment> selectCommentsByUserId(@Param("userId") Long userId, @Param("status") Integer status);

    /**
     * 查询用户的评论 - 分页
     * @param userId 用户ID
     * @param status 评论状态（可选）
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 评论列表
     */
    @Select("<script>" +
            "SELECT c.*, a.title as article_title " +
            "FROM comments c " +
            "LEFT JOIN articles a ON c.article_id = a.id " +
            "WHERE c.user_id = #{userId} AND c.deleted = 0 " +
            "<if test='status != null'>AND c.status = #{status}</if>" +
            "ORDER BY c.create_time DESC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<Comment> selectCommentsByUserIdWithPagination(@Param("userId") Long userId, @Param("status") Integer status, @Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * 查询待审核的评论
     * @return 待审核评论列表
     */
    @Select("SELECT c.*, u.nickname, a.title as article_title " +
            "FROM comments c " +
            "LEFT JOIN users u ON c.user_id = u.id " +
            "LEFT JOIN articles a ON c.article_id = a.id " +
            "WHERE c.status = 1 AND c.deleted = 0 " +
            "ORDER BY c.create_time DESC")
    List<Comment> selectPendingComments();

    @Select("<script>"
            + "SELECT c.*, u.nickname, u.avatar "
            + "FROM comments c "
            + "LEFT JOIN users u ON c.user_id = u.id "
            + "WHERE c.article_id = #{articleId} AND c.parent_id = 0 AND c.deleted = 0 "
            + "<if test='status != null'>AND c.status = #{status}</if> "
            + "ORDER BY c.create_time ASC "
            + "LIMIT #{offset}, #{limit}"
            + "</script>")
    List<Comment> selectTopLevelCommentsWithPagination(@Param("articleId") Long articleId, @Param("status") Integer status, @Param("offset") Integer offset, @Param("limit") Integer limit);

    @Select("<script>"
            + "SELECT c.*, u.nickname, u.avatar "
            + "FROM comments c "
            + "LEFT JOIN users u ON c.user_id = u.id "
            + "WHERE c.deleted = 0 "
            + "AND c.parent_id IN "
            + "<foreach collection='parentIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
            + "<if test='status != null'>AND c.status = #{status}</if> "
            + "ORDER BY c.create_time ASC"
            + "</script>")
    List<Comment> selectChildCommentsByParentIds(@Param("parentIds") List<Long> parentIds, @Param("status") Integer status);

    @Select("UPDATE comments SET content = #{content}, update_time = NOW() WHERE id = #{commentId} AND deleted = 0")
    int updateContent(@Param("commentId") Long commentId, @Param("content") String content);

    /**
     * 增加评论点赞数
     * @param commentId 评论ID
     * @return 影响行数
     */
    @Update("UPDATE comments SET like_count = like_count + 1, update_time = NOW() WHERE id = #{commentId} AND deleted = 0")
    Integer incrementLikeCount(@Param("commentId") Long commentId);

    /**
     * 减少评论点赞数
     * @param commentId 评论ID
     * @return 影响行数
     */
    @Update("UPDATE comments SET like_count = CASE WHEN like_count > 0 THEN like_count - 1 ELSE 0 END, update_time = NOW() WHERE id = #{commentId} AND deleted = 0")
    Integer decrementLikeCount(@Param("commentId") Long commentId);

    /**
     * 批量更新评论状态
     * @param commentIds 评论ID列表
     * @param status 新状态
     * @return 影响行数
     */
    @Select("<script>" +
            "UPDATE comments SET status = #{status}, update_time = NOW() " +
            "WHERE id IN " +
            "<foreach collection='commentIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "AND deleted = 0" +
            "</script>")
    int batchUpdateCommentStatus(@Param("commentIds") List<Long> commentIds, @Param("status") Integer status);

    /**
     * 递归查询所有子评论ID（包括子评论的子评论）
     * @param parentId 父评论ID
     * @return 所有子评论ID列表
     */
    @Select("WITH RECURSIVE comment_tree AS ("
            + "SELECT id FROM comments WHERE parent_id = #{parentId} AND deleted = 0 "
            + "UNION ALL "
            + "SELECT c.id FROM comments c "
            + "INNER JOIN comment_tree ct ON c.parent_id = ct.id "
            + "WHERE c.deleted = 0 "
            + ") "
            + "SELECT id FROM comment_tree")
    List<Long> selectAllChildCommentIdsRecursive(@Param("parentId") Long parentId);

    /**
     * 查询指定父评论的直接子评论（非递归）
     * @param parentId 父评论ID
     * @return 子评论列表
     */
    @Select("SELECT * FROM comments WHERE parent_id = #{parentId} AND deleted = 0 ORDER BY create_time ASC")
    List<Comment> selectDirectChildComments(@Param("parentId") Long parentId);
}
