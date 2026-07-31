package com.blog.mapper;

import com.blog.entity.SensitiveWord;
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
@DisplayName("SensitiveWordMapper DAO 直测")
class SensitiveWordMapperDaoTest {

    @Autowired
    private SensitiveWordMapper sensitiveWordMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanup() {
        jdbcTemplate.execute("DELETE FROM sensitive_words WHERE word LIKE 'dao-test-%'");
    }

    @Test
    @DisplayName("插入后可按级别/分类查询与存在性检查")
    void sensitiveWordQueries_shouldReturnInsertedWords() {
        SensitiveWord word = new SensitiveWord("dao-test-bad", "test", 2);
        sensitiveWordMapper.insert(word);

        List<String> all = sensitiveWordMapper.getAllSensitiveWords();
        assertThat(all).contains("dao-test-bad");

        assertThat(sensitiveWordMapper.getSensitiveWordsByLevel(2)).contains("dao-test-bad");
        assertThat(sensitiveWordMapper.getSensitiveWordsByCategory("test")).contains("dao-test-bad");
        assertThat(sensitiveWordMapper.existsSensitiveWord("dao-test-bad")).isTrue();
        assertThat(sensitiveWordMapper.existsSensitiveWord("dao-test-missing")).isFalse();
    }
}
