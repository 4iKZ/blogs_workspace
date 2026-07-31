package com.blog.mapper;

import com.blog.entity.Comment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

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
}
