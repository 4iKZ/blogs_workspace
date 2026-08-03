package com.blog.mapper;

import com.blog.entity.UserLike;
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
@DisplayName("UserLikeMapper DAO 直测")
class UserLikeMapperDaoTest {

    @Autowired
    private UserLikeMapper userLikeMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long userId;

    @AfterEach
    void cleanup() {
        if (userId != null) {
            jdbcTemplate.execute("DELETE FROM user_likes WHERE user_id = " + userId);
        }
    }

    @Test
    @DisplayName("文章点赞 CRUD 与计数")
    void userLikeCRUD_shouldPersistAndReturnRows() {
        userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        Long articleId = jdbcTemplate.queryForObject("SELECT id FROM articles WHERE title = 'Spring Boot 快速入门指南'", Long.class);

        UserLike like = new UserLike();
        like.setUserId(userId);
        like.setArticleId(articleId);
        like.setTargetType(1);
        userLikeMapper.insert(like);

        UserLike found = userLikeMapper.findByUserIdAndArticleId(userId, articleId);
        assertThat(found).isNotNull();

        assertThat(userLikeMapper.countByArticleId(articleId)).isGreaterThan(0);
        assertThat(userLikeMapper.countByUserId(userId)).isGreaterThan(0);
        assertThat(userLikeMapper.countByUserIdAndArticleId(userId, articleId)).isGreaterThan(0);

        List<Long> userIds = userLikeMapper.findUserIdsByArticleId(articleId);
        assertThat(userIds).contains(userId);

        List<Long> articleIds = userLikeMapper.findArticleIdsByUserId(userId);
        assertThat(articleIds).contains(articleId);

        List<Long> likedIds = userLikeMapper.findLikedArticleIdsByUserIdAndArticleIds(userId, List.of(articleId));
        assertThat(likedIds).contains(articleId);

        List<UserLike> recent = userLikeMapper.selectRecentRecords(5);
        assertThat(recent).extracting(UserLike::getUserId).contains(userId);

        int deleted = userLikeMapper.deleteByUserIdAndArticleId(userId, articleId);
        assertThat(deleted).isEqualTo(1);
    }
}
