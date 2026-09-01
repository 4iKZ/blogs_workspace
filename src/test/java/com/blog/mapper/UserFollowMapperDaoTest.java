package com.blog.mapper;

import com.blog.entity.UserFollow;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dao-test")
@DisplayName("UserFollowMapper DAO 直测")
class UserFollowMapperDaoTest {

    @Autowired
    private UserFollowMapper userFollowMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanup() {
        jdbcTemplate.execute("DELETE FROM user_follows WHERE follower_id = 2 AND following_id IN (1, 2)");
    }

    @Test
    @DisplayName("关注关系查询与恢复")
    void userFollow_shouldCheckAndRestore() {
        jdbcTemplate.execute("INSERT INTO user_follows (follower_id, following_id, deleted) VALUES (2, 1, 0)");

        UserFollow follow = userFollowMapper.selectByFollowerAndFollowingIncludingDeleted(2L, 1L);
        assertThat(follow).isNotNull();
        assertThat(follow.getDeleted()).isZero();

        Long id = follow.getId();
        jdbcTemplate.execute("UPDATE user_follows SET deleted = 1 WHERE id = " + id);

        follow = userFollowMapper.selectByFollowerAndFollowingIncludingDeleted(2L, 1L);
        assertThat(follow).isNotNull();
        assertThat(follow.getDeleted()).isOne();

        int restored = userFollowMapper.restoreFollow(id);
        assertThat(restored).isEqualTo(1);

        follow = userFollowMapper.selectByFollowerAndFollowingIncludingDeleted(2L, 1L);
        assertThat(follow.getDeleted()).isZero();
    }
}
