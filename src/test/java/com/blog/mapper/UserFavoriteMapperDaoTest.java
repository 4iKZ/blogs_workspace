package com.blog.mapper;

import com.blog.entity.UserFavorite;
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
@DisplayName("UserFavoriteMapper DAO 直测")
class UserFavoriteMapperDaoTest {

    @Autowired
    private UserFavoriteMapper userFavoriteMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long userId;

    @AfterEach
    void cleanup() {
        if (userId != null) {
            jdbcTemplate.execute("DELETE FROM user_favorites WHERE user_id = " + userId);
        }
        jdbcTemplate.execute("DELETE FROM articles WHERE title = 'dao-test-draft'");
    }

    private Long adminId() {
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
    }

    private Long publishedArticleId() {
        return jdbcTemplate.queryForObject("SELECT id FROM articles WHERE title = 'Spring Boot 快速入门指南'", Long.class);
    }

    private Long insertDraftArticle(Long authorId) {
        jdbcTemplate.update(
                "INSERT INTO articles (title, content, summary, category_id, author_id, status, view_count, like_count, " +
                        "comment_count, favorite_count, is_top, is_recommend, publish_time) " +
                        "VALUES ('dao-test-draft', 'draft-content', NULL, 1, ?, 1, 0, 0, 0, 0, 0, 0, NULL)",
                authorId);
        return jdbcTemplate.queryForObject("SELECT id FROM articles WHERE title = 'dao-test-draft'", Long.class);
    }

    private void insertFavorite(Long userId, Long articleId) {
        UserFavorite favorite = new UserFavorite();
        favorite.setUserId(userId);
        favorite.setArticleId(articleId);
        userFavoriteMapper.insert(favorite);
    }

    @Test
    @DisplayName("收藏插入、查询、计数与删除")
    void favoriteCRUD_shouldPersistAndReturnRows() {
        userId = adminId();
        Long articleId = publishedArticleId();

        UserFavorite favorite = new UserFavorite();
        favorite.setUserId(userId);
        favorite.setArticleId(articleId);
        int inserted = userFavoriteMapper.insert(favorite);
        assertThat(inserted).isGreaterThan(0);
        assertThat(favorite.getId()).isNotNull();

        UserFavorite found = userFavoriteMapper.selectByUserAndArticle(userId, articleId);
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(favorite.getId());

        assertThat(userFavoriteMapper.countByUserIdAndArticleId(userId, articleId)).isEqualTo(1);
        assertThat(userFavoriteMapper.countArticleFavorites(articleId)).isGreaterThan(0);
        assertThat(userFavoriteMapper.countByUserId(userId)).isGreaterThan(0);

        List<Long> articleIds = userFavoriteMapper.findArticleIdsByUserId(userId);
        assertThat(articleIds).contains(articleId);

        List<Long> favorited = userFavoriteMapper.findFavoritedArticleIdsByUserIdAndArticleIds(userId, List.of(articleId));
        assertThat(favorited).contains(articleId);

        int deleted = userFavoriteMapper.deleteByUserIdAndArticleId(userId, articleId);
        assertThat(deleted).isEqualTo(1);
        assertThat(userFavoriteMapper.selectByUserAndArticle(userId, articleId)).isNull();
    }

    @Test
    @DisplayName("分页查询与批量取消收藏")
    void pagedFavorites_andBatchDelete() {
        userId = adminId();
        Long articleId = publishedArticleId();
        insertFavorite(userId, articleId);

        List<UserFavorite> page = userFavoriteMapper.selectByUserId(userId, 0, 10);
        assertThat(page).extracting(UserFavorite::getArticleId).contains(articleId);

        int deleted = userFavoriteMapper.batchDeleteFavorites(userId, List.of(articleId));
        assertThat(deleted).isEqualTo(1);
        assertThat(userFavoriteMapper.selectByUserAndArticle(userId, articleId)).isNull();
    }

    @Test
    @DisplayName("联表查询仅返回已发布文章，草稿被排除")
    void joinedQueries_onlyReturnPublishedArticles() {
        userId = adminId();
        Long publishedId = publishedArticleId();
        Long draftId = insertDraftArticle(userId);

        insertFavorite(userId, publishedId);
        insertFavorite(userId, draftId);

        List<UserFavorite> favorites = userFavoriteMapper.selectUserFavorites(userId);
        assertThat(favorites).extracting(UserFavorite::getArticleId)
                .contains(publishedId)
                .doesNotContain(draftId);

        assertThat(userFavoriteMapper.countUserFavorites(userId)).isEqualTo(1);
        assertThat(userFavoriteMapper.countByUserId(userId)).isEqualTo(1);
    }

    @Test
    @DisplayName("按文章 ID 删除收藏记录")
    void deleteByArticleId_shouldRemoveRows() {
        userId = adminId();
        Long draftId = insertDraftArticle(userId);
        insertFavorite(userId, draftId);

        int deleted = userFavoriteMapper.deleteByArticleId(draftId);
        assertThat(deleted).isGreaterThan(0);
        assertThat(userFavoriteMapper.selectByUserAndArticle(userId, draftId)).isNull();
    }

    @Test
    @DisplayName("查询最近收藏记录")
    void selectRecentRecords_shouldReturnInsertedRows() {
        userId = adminId();
        Long articleId = publishedArticleId();
        insertFavorite(userId, articleId);

        List<UserFavorite> recent = userFavoriteMapper.selectRecentRecords(10);
        assertThat(recent).extracting(UserFavorite::getUserId).contains(userId);
    }
}
