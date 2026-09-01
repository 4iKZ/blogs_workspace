package com.blog.mapper;

import com.blog.entity.Comment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dao-test")
@DisplayName("CommentMapper DAO 直测")
class CommentMapperDaoTest {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanup() {
        jdbcTemplate.execute("DELETE FROM comments WHERE content LIKE 'dao-test-%'");
        jdbcTemplate.execute("DELETE FROM articles WHERE title = 'dao-test-comments-article'");
    }

    private Long adminId() {
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
    }

    private Long seedArticleId() {
        return jdbcTemplate.queryForObject("SELECT id FROM articles WHERE title = 'Spring Boot 快速入门指南'", Long.class);
    }

    private Long testArticleId() {
        Long categoryId = jdbcTemplate.queryForObject("SELECT id FROM categories WHERE name = '技术分享'", Long.class);
        jdbcTemplate.update(
                "INSERT INTO articles (title, content, summary, category_id, author_id, status, view_count, like_count, " +
                        "comment_count, favorite_count, is_top, is_recommend, publish_time) " +
                        "VALUES ('dao-test-comments-article', 'c', NULL, ?, ?, 2, 0, 0, 0, 0, 0, 0, NOW())",
                categoryId, adminId());
        return jdbcTemplate.queryForObject("SELECT id FROM articles WHERE title = 'dao-test-comments-article'", Long.class);
    }

    private Comment buildComment(Long articleId, Long userId, Long parentId, String content) {
        Comment comment = new Comment();
        comment.setArticleId(articleId);
        comment.setUserId(userId);
        comment.setParentId(parentId);
        comment.setReplyToCommentId(parentId == 0 ? null : parentId);
        comment.setContent(content);
        comment.setLikeCount(0);
        comment.setStatus(2);
        return comment;
    }

    @Test
    @DisplayName("文章评论查询、插入、更新、删除与计数字段")
    void commentLifecycle_shouldPersistAndReturnRows() {
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        Long articleId = jdbcTemplate.queryForObject("SELECT id FROM articles WHERE title = 'Spring Boot 快速入门指南'", Long.class);

        Comment comment = new Comment();
        comment.setArticleId(articleId);
        comment.setUserId(userId);
        comment.setParentId(0L);
        comment.setReplyToCommentId(null);
        comment.setContent("dao-test-comment");
        comment.setLikeCount(0);
        comment.setStatus(2);

        int inserted = commentMapper.insert(comment);
        assertThat(inserted).isGreaterThan(0);
        assertThat(comment.getId()).isNotNull();

        List<Comment> topLevel = commentMapper.selectTopLevelComments(articleId, 2);
        assertThat(topLevel).extracting(Comment::getId).contains(comment.getId());

        int updated = commentMapper.updateContent(comment.getId(), "dao-test-comment-updated");
        assertThat(updated).isEqualTo(1);

        int updatedStatus = commentMapper.batchUpdateCommentStatus(Arrays.asList(comment.getId()), 1);
        assertThat(updatedStatus).isEqualTo(1);

        int deleted = commentMapper.deleteById(comment.getId());
        assertThat(deleted).isEqualTo(1);
    }

    @Test
    @DisplayName("评论点赞数更新")
    void updateCommentLikeCount_shouldAffectRows() {
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        Long articleId = jdbcTemplate.queryForObject("SELECT id FROM articles WHERE title = 'Spring Boot 快速入门指南'", Long.class);

        Comment comment = new Comment();
        comment.setArticleId(articleId);
        comment.setUserId(userId);
        comment.setParentId(0L);
        comment.setReplyToCommentId(null);
        comment.setContent("like-count-comment");
        comment.setLikeCount(0);
        comment.setStatus(2);

        commentMapper.insert(comment);

        int increment = commentMapper.incrementLikeCount(comment.getId());
        assertThat(increment).isEqualTo(1);

        int decrement = commentMapper.decrementLikeCount(comment.getId());
        assertThat(decrement).isEqualTo(1);
    }

    @Test
    @DisplayName("子评论查询")
    void childComments_shouldReturnReplies() {
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        Long articleId = jdbcTemplate.queryForObject("SELECT id FROM articles WHERE title = 'Spring Boot 快速入门指南'", Long.class);

        Comment parent = new Comment();
        parent.setArticleId(articleId);
        parent.setUserId(userId);
        parent.setParentId(0L);
        parent.setReplyToCommentId(null);
        parent.setContent("parent-comment");
        parent.setLikeCount(0);
        parent.setStatus(2);
        commentMapper.insert(parent);

        Comment child = new Comment();
        child.setArticleId(articleId);
        child.setUserId(userId);
        child.setParentId(parent.getId());
        child.setReplyToCommentId(parent.getId());
        child.setContent("child-comment");
        child.setLikeCount(0);
        child.setStatus(2);
        commentMapper.insert(child);

        List<Comment> children = commentMapper.selectChildComments(parent.getId(), 2);
        assertThat(children).extracting(Comment::getId).contains(child.getId());

        commentMapper.deleteById(child.getId());
        commentMapper.deleteById(parent.getId());
    }

    @Test
    @DisplayName("文章评论列表、分页与顶级评论分页")
    void articleCommentList_andPagination() {
        Long userId = adminId();
        Long articleId = testArticleId();

        Comment top = buildComment(articleId, userId, 0L, "dao-test-list-top-" + System.nanoTime());
        commentMapper.insert(top);
        Comment child = buildComment(articleId, userId, top.getId(), "dao-test-list-child-" + System.nanoTime());
        commentMapper.insert(child);

        assertThat(commentMapper.selectCommentsByArticleId(articleId, 2))
                .extracting(Comment::getId).contains(top.getId(), child.getId());
        assertThat(commentMapper.selectCommentsByArticleIdWithPagination(articleId, 2, 0, 10))
                .extracting(Comment::getId).contains(top.getId(), child.getId());

        assertThat(commentMapper.selectCommentsList())
                .extracting(Comment::getId).contains(top.getId());

        List<Comment> topLevelPage = commentMapper.selectTopLevelCommentsWithPagination(articleId, 2, 0, 10);
        assertThat(topLevelPage).extracting(Comment::getId).contains(top.getId()).doesNotContain(child.getId());
    }

    @Test
    @DisplayName("用户评论查询、分页与批量子评论查询")
    void userComments_andChildBatch() {
        Long userId = adminId();
        Long articleId = seedArticleId();

        Comment parent = buildComment(articleId, userId, 0L, "dao-test-user-parent-" + System.nanoTime());
        commentMapper.insert(parent);
        Comment child = buildComment(articleId, userId, parent.getId(), "dao-test-user-child-" + System.nanoTime());
        commentMapper.insert(child);
        Comment grandchild = buildComment(articleId, userId, child.getId(), "dao-test-user-grandchild-" + System.nanoTime());
        commentMapper.insert(grandchild);

        assertThat(commentMapper.selectCommentsByUserId(userId, 2))
                .extracting(Comment::getId).contains(parent.getId(), child.getId(), grandchild.getId());
        assertThat(commentMapper.selectCommentsByUserIdWithPagination(userId, 2, 0, 10000))
                .extracting(Comment::getId).contains(parent.getId(), child.getId(), grandchild.getId());

        assertThat(commentMapper.selectDirectChildComments(parent.getId()))
                .extracting(Comment::getId).contains(child.getId()).doesNotContain(grandchild.getId());

        assertThat(commentMapper.selectChildCommentsByParentIds(List.of(parent.getId(), child.getId()), 2))
                .extracting(Comment::getId).contains(child.getId(), grandchild.getId());
    }

    @Test
    @DisplayName("递归查询所有子评论 ID")
    void recursiveChildIds_shouldIncludeAllDescendants() {
        Long userId = adminId();
        Long articleId = seedArticleId();

        Comment parent = buildComment(articleId, userId, 0L, "dao-test-rec-parent-" + System.nanoTime());
        commentMapper.insert(parent);
        Comment child = buildComment(articleId, userId, parent.getId(), "dao-test-rec-child-" + System.nanoTime());
        commentMapper.insert(child);
        Comment grandchild = buildComment(articleId, userId, child.getId(), "dao-test-rec-grandchild-" + System.nanoTime());
        commentMapper.insert(grandchild);

        List<Long> ids = commentMapper.selectAllChildCommentIdsRecursive(parent.getId());
        assertThat(ids).contains(child.getId(), grandchild.getId());
    }
}
