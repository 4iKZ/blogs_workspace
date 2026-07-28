package com.blog.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UserMapperTokenVersionConcurrencyTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void concurrentInvalidations_shouldAtomicallyIncreaseTokenVersionByTwo() throws Exception {
        Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = 'admin'", Long.class);
        jdbcTemplate.update("UPDATE users SET token_version = 10 WHERE id = ?", userId);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);

        try {
            var first = executor.submit(() -> {
                start.await(5, TimeUnit.SECONDS);
                return userMapper.incrementTokenVersion(userId);
            });
            var second = executor.submit(() -> {
                start.await(5, TimeUnit.SECONDS);
                return userMapper.incrementTokenVersion(userId);
            });
            start.countDown();

            assertThat(first.get(5, TimeUnit.SECONDS)).isEqualTo(1);
            assertThat(second.get(5, TimeUnit.SECONDS)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }

        assertThat(jdbcTemplate.queryForObject(
                "SELECT token_version FROM users WHERE id = ?", Integer.class, userId))
                .isEqualTo(12);
    }

    @Test
    void disableThenReEnable_shouldKeepBothVersionBumpsSoOldTokensCannotBecomeValidAgain() {
        Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = 'demo_user'", Long.class);
        jdbcTemplate.update("UPDATE users SET status = 1, token_version = 20 WHERE id = ?", userId);

        assertThat(userMapper.updateStatusAndIncrementTokenVersion(userId, 2)).isEqualTo(1);
        assertThat(userMapper.updateStatusAndIncrementTokenVersion(userId, 1)).isEqualTo(1);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT token_version FROM users WHERE id = ?", Integer.class, userId))
                .isEqualTo(22);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM users WHERE id = ?", Integer.class, userId))
                .isEqualTo(1);
    }
}
