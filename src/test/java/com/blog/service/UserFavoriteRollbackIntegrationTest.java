package com.blog.service;

import com.blog.common.ResultCode;
import com.blog.exception.BusinessException;
import com.blog.utils.RedisDistributedLock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

/**
 * 回归测试：favoriteArticle 事务内统计更新失败必须让收藏记录随事务回滚，
 * 不得出现"收藏记录已落库、统计未更新"的半提交状态。
 */
@SpringBootTest
class UserFavoriteRollbackIntegrationTest {

    @Autowired
    private UserFavoriteService userFavoriteService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoSpyBean
    private ArticleStatisticsService statisticsService;

    @MockitoSpyBean
    private RedisDistributedLock redisDistributedLock;

    private Long userId;
    private Long articleId;

    @BeforeEach
    void setUp() {
        Long authorId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username='admin'", Long.class);
        userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username='demo_user'", Long.class);
        Long categoryId = jdbcTemplate.queryForObject(
                "SELECT id FROM categories WHERE name='技术分享'", Long.class);

        String title = "favorite-rollback-test-" + System.nanoTime();
        jdbcTemplate.update(
                "INSERT INTO articles (title, content, summary, category_id, author_id, status, "
                        + "view_count, like_count, comment_count, favorite_count, is_top, is_recommend, publish_time) "
                        + "VALUES (?, ?, ?, ?, ?, 2, 0, 0, 0, 0, 0, 0, CURRENT_TIMESTAMP)",
                title, "rollback test content", "rollback test summary", categoryId, authorId);
        articleId = jdbcTemplate.queryForObject("SELECT id FROM articles WHERE title = ?", Long.class, title);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", userId);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        doReturn("mock-lock-value")
                .when(redisDistributedLock).tryLock(anyString(), anyLong(), any(TimeUnit.class));
        doNothing().when(redisDistributedLock).releaseLock(anyString(), anyString());
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        if (articleId != null) {
            jdbcTemplate.update("DELETE FROM user_favorites WHERE article_id = ?", articleId);
            jdbcTemplate.update("DELETE FROM articles WHERE id = ?", articleId);
        }
    }

    @Test
    @DisplayName("收藏文章 - 统计更新失败时收藏记录必须回滚")
    void favoriteArticle_statisticsFailure_shouldRollbackFavoriteInsert() {
        doThrow(new BusinessException(ResultCode.ARTICLE_NOT_FOUND, "文章不存在"))
                .when(statisticsService).incrementFavoriteCount(anyLong());

        assertThatThrownBy(() -> userFavoriteService.favoriteArticle(articleId))
                .isInstanceOf(BusinessException.class);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM user_favorites WHERE user_id = ? AND article_id = ?",
                Integer.class, userId, articleId);
        assertThat(count).isZero();
    }
}
