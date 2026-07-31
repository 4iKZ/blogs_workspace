package com.blog.mapper;

import com.blog.entity.ArticleView;
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
@DisplayName("ArticleViewMapper DAO 直测")
class ArticleViewMapperDaoTest {

    @Autowired
    private ArticleViewMapper articleViewMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanup() {
        jdbcTemplate.execute("DELETE FROM article_views WHERE ip_address = '127.0.0.2'");
    }

    @Test
    @DisplayName("文章浏览统计与今日浏览/访客聚合")
    void articleViewStats_shouldReturnInsertedRows() {
        Long articleId = jdbcTemplate.queryForObject("SELECT id FROM articles WHERE title = 'Spring Boot 快速入门指南'", Long.class);
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);

        jdbcTemplate.execute(
                "INSERT INTO article_views (article_id, user_id, ip_address, user_agent, referer, view_date, view_time, deleted) " +
                        "VALUES (" + articleId + "," + userId + ",'127.0.0.2','dao-test-agent','/ref',CURDATE(),NOW(),0)"
        );
        jdbcTemplate.execute(
                "INSERT INTO article_views (article_id, user_id, ip_address, user_agent, referer, view_date, view_time, deleted) " +
                        "VALUES (" + articleId + "," + userId + ",'127.0.0.2','dao-test-agent','/ref',CURDATE(),NOW(),0)"
        );

        assertThat(articleViewMapper.countArticleViews(articleId)).isGreaterThanOrEqualTo(2);
        assertThat(articleViewMapper.countTodayArticleViews(articleId)).isGreaterThanOrEqualTo(2);
        assertThat(articleViewMapper.countUserTodayViews(userId, articleId)).isGreaterThanOrEqualTo(1);
        assertThat(articleViewMapper.countIpTodayViews("127.0.0.2", articleId)).isGreaterThanOrEqualTo(1);
        assertThat(articleViewMapper.countTodayTotalViews()).isGreaterThan(0);
        assertThat(articleViewMapper.countTodayUniqueVisitors()).isGreaterThan(0);
    }
}
