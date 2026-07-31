package com.blog.mapper;

import com.blog.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dao-test")
@DisplayName("UserMapper DAO 直测")
class UserMapperDaoTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanup() {
        jdbcTemplate.execute("DELETE FROM users WHERE username LIKE 'dao-test-%'");
        jdbcTemplate.execute("DELETE FROM user_follows WHERE follower_id = 2 AND following_id = 1");
    }

    @Test
    @DisplayName("按用户名/邮箱/GitHubID查询用户")
    void selectUserByIdentifiers_shouldReturnExpectedUser() {
        assertThat(userMapper.selectByUsername("admin")).isNotNull();
        assertThat(userMapper.selectByEmail("admin@blog.com")).isNotNull();
        assertThat(userMapper.selectByGithubId(999999L)).isNull();
    }

    @Test
    @DisplayName("统计用户名/邮箱存在性")
    void countUserByIdentifiers_shouldReturnNonZeroForExistingUser() {
        assertThat(userMapper.countByUsername("admin")).isEqualTo(1);
        assertThat(userMapper.countByEmail("admin@blog.com")).isEqualTo(1);
        assertThat(userMapper.countByUsername("not-exist")).isEqualTo(0);
    }

    @Test
    @DisplayName("更新最后登录信息")
    void updateLastLoginInfo_shouldAffectRow() {
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        LocalDateTime now = LocalDateTime.now();
        int updated = userMapper.updateLastLoginInfo(userId, now, "127.0.0.1");
        assertThat(updated).isEqualTo(1);
    }

    @Test
    @DisplayName("粉丝数/关注数增减")
    void followCounts_shouldUpdate() {
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);

        userMapper.incrementFollowerCount(userId);
        userMapper.incrementFollowingCount(userId);
        userMapper.decrementFollowerCount(userId);
        userMapper.decrementFollowingCount(userId);
    }
}
