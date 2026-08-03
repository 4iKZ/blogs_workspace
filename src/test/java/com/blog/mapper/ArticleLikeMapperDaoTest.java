package com.blog.mapper;

import com.blog.entity.ArticleLike;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dao-test")
@DisplayName("ArticleLikeMapper DAO 直测")
class ArticleLikeMapperDaoTest {

    @Autowired
    private ArticleLikeMapper articleLikeMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanup() {
        jdbcTemplate.execute("DELETE FROM article_like WHERE user_id = 999");
    }

    @Test
    @DisplayName("文章点赞插入与查询")
    void articleLike_shouldPersistAndReturnRow() {
        Long articleId = jdbcTemplate.queryForObject("SELECT id FROM articles WHERE title = 'Spring Boot 快速入门指南'", Long.class);

        ArticleLike like = new ArticleLike();
        like.setArticleId(articleId);
        like.setUserId(999L);
        like.setCreateTime(LocalDateTime.now());
        like.setUpdateTime(LocalDateTime.now());
        like.setDeleted(0);

        int inserted = articleLikeMapper.insert(like);
        assertThat(inserted).isGreaterThan(0);
        assertThat(like.getId()).isNotNull();

        ArticleLike selected = articleLikeMapper.selectById(like.getId());
        assertThat(selected).isNotNull();
        assertThat(selected.getArticleId()).isEqualTo(articleId);
    }
}
