package com.blog.mapper;

import com.blog.entity.CommentLike;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dao-test")
@DisplayName("CommentLikeMapper DAO 直测")
class CommentLikeMapperDaoTest {

    @Autowired
    private CommentLikeMapper commentLikeMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long userId;

    @AfterEach
    void cleanup() {
        if (userId != null) {
            jdbcTemplate.execute("DELETE FROM comment_likes WHERE user_id = " + userId);
        }
    }

    private Long adminId() {
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
    }

    private Long anyCommentId() {
        return jdbcTemplate.queryForObject("SELECT id FROM comments ORDER BY id LIMIT 1", Long.class);
    }

    @Test
    @DisplayName("点赞检查、计数、批量查询与单条删除")
    void commentLikeQueries_shouldReturnInsertedRows() {
        userId = adminId();
        Long commentId = anyCommentId();

        commentLikeMapper.insert(new CommentLike(commentId, userId));

        assertThat(commentLikeMapper.checkUserLikedComment(commentId, userId)).isTrue();
        assertThat(commentLikeMapper.checkUserLikedComment(commentId, 999999L)).isFalse();
        assertThat(commentLikeMapper.getCommentLikeCount(commentId)).isGreaterThan(0);

        List<Long> liked = commentLikeMapper.batchCheckUserLikedComments(List.of(commentId), userId);
        assertThat(liked).contains(commentId);

        int deleted = commentLikeMapper.deleteByCommentIdAndUserId(commentId, userId);
        assertThat(deleted).isEqualTo(1);
        assertThat(commentLikeMapper.checkUserLikedComment(commentId, userId)).isFalse();
    }

    @Test
    @DisplayName("按评论 ID 与按用户 ID 批量删除")
    void deleteByCommentId_and_deleteByUserId() {
        userId = adminId();
        Long commentId = anyCommentId();

        commentLikeMapper.insert(new CommentLike(commentId, userId));
        int deletedByComment = commentLikeMapper.deleteByCommentId(commentId);
        assertThat(deletedByComment).isGreaterThan(0);
        assertThat(commentLikeMapper.checkUserLikedComment(commentId, userId)).isFalse();

        commentLikeMapper.insert(new CommentLike(commentId, userId));
        int deletedByUser = commentLikeMapper.deleteByUserId(userId);
        assertThat(deletedByUser).isGreaterThan(0);
        assertThat(commentLikeMapper.checkUserLikedComment(commentId, userId)).isFalse();
    }
}
