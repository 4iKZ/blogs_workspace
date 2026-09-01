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

    private User insertTestUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@blog.com");
        user.setPassword("dao-test-password");
        user.setStatus(1);
        user.setRole(1);
        userMapper.insert(user);
        return user;
    }

    @Test
    @DisplayName("用户总数/活跃数/今日新增统计")
    void userCounts_shouldReturnValidNumbers() {
        User user = insertTestUser("dao-test-count-" + System.nanoTime());
        jdbcTemplate.execute("UPDATE users SET last_login_time = NOW() WHERE id = " + user.getId());

        assertThat(userMapper.countTotalUsers()).isGreaterThanOrEqualTo(2);
        assertThat(userMapper.countActiveUsers()).isGreaterThanOrEqualTo(1);
        assertThat(userMapper.countNewUsersToday()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("分页查询用户列表（关键词/状态过滤）")
    void selectUserList_shouldFilterByKeyword() {
        String token = "daolist" + System.nanoTime();
        User user = insertTestUser("dao-test-" + token);

        List<User> byKeyword = userMapper.selectUserList(0, 10, "dao-test-" + token, null);
        assertThat(byKeyword).extracting(User::getId).contains(user.getId());

        List<User> byStatus = userMapper.selectUserList(0, 10, null, 1);
        assertThat(byStatus).extracting(User::getId).contains(user.getId());
    }

    @Test
    @DisplayName("更新用户状态与密码（同时自增令牌版本）")
    void updateStatus_andPasswordShouldIncrementTokenVersion() {
        User user = insertTestUser("dao-test-upd-" + System.nanoTime());

        assertThat(userMapper.updateStatus(user.getId(), 2)).isEqualTo(1);
        User updated = userMapper.selectById(user.getId());
        assertThat(updated.getStatus()).isEqualTo(2);

        int before = updated.getTokenVersion() == null ? 0 : updated.getTokenVersion();
        assertThat(userMapper.updatePasswordAndIncrementTokenVersion(user.getId(), "new-password", LocalDateTime.now()))
                .isEqualTo(1);
        User after = userMapper.selectById(user.getId());
        assertThat(after.getTokenVersion()).isEqualTo(before + 1);
    }
}
