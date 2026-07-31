package com.blog.mapper;

import com.blog.entity.ArticleModerationLog;
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
@DisplayName("ArticleModerationLogMapper DAO 直测")
class ArticleModerationLogMapperDaoTest {

    @Autowired
    private ArticleModerationLogMapper articleModerationLogMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanup() {
        jdbcTemplate.execute("DELETE FROM article_moderation_log WHERE article_id = 9999");
    }

    @Test
    @DisplayName("审核日志插入与查询")
    void moderationLog_shouldPersistAndReturnRow() {
        ArticleModerationLog log = new ArticleModerationLog();
        log.setArticleId(9999L);
        log.setTitle("dao-test-title");
        log.setContent("dao-test-content");
        log.setPassed(ArticleModerationLog.PASSED);
        log.setConfidence(0.95);
        log.setCheckTime(LocalDateTime.now());

        int inserted = articleModerationLogMapper.insert(log);
        assertThat(inserted).isGreaterThan(0);
        assertThat(log.getId()).isNotNull();

        ArticleModerationLog selected = articleModerationLogMapper.selectById(log.getId());
        assertThat(selected).isNotNull();
        assertThat(selected.getTitle()).isEqualTo("dao-test-title");
        assertThat(selected.getConfidence()).isEqualTo(0.95);
    }
}
