package com.blog.mapper;

import com.blog.dto.ArticleDTO;
import com.blog.entity.Article;
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
@DisplayName("ArticleMapper DAO 直测")
class ArticleMapperDaoTest {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("统计文章总数 / 已发布数 / 今日新增")
    void countArticles_shouldReturnValidNumbers() {
        assertThat(articleMapper.countTotalArticles()).isGreaterThanOrEqualTo(0);
        assertThat(articleMapper.countPublishedArticles()).isGreaterThanOrEqualTo(0);
        assertThat(articleMapper.countNewArticlesToday()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("文章列表与状态更新")
    void selectAndUpdateArticle_shouldPersistAndReturnRows() {
        Long authorId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        Long categoryId = jdbcTemplate.queryForObject("SELECT id FROM categories WHERE name = '技术分享'", Long.class);

        Article article = new Article();
        article.setTitle("DAO Test Article");
        article.setContent("dao test content");
        article.setSummary("dao test summary");
        article.setCategoryId(categoryId);
        article.setAuthorId(authorId);
        article.setStatus(Article.STATUS_PUBLISHED);
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setCommentCount(0);
        article.setFavoriteCount(0);
        article.setIsTop(0);
        article.setIsRecommended(0);
        article.setPublishTime(LocalDateTime.now());

        int inserted = articleMapper.insert(article);
        assertThat(inserted).isGreaterThan(0);
        assertThat(article.getId()).isNotNull();

        List<ArticleDTO> articles = articleMapper.selectArticleList(0, 10, null, Article.STATUS_PUBLISHED);
        assertThat(articles).extracting(ArticleDTO::getId).contains(article.getId());

        int updated = articleMapper.updateStatus(article.getId(), Article.STATUS_DRAFT);
        assertThat(updated).isEqualTo(1);

        Article updatedArticle = articleMapper.selectById(article.getId());
        assertThat(updatedArticle.getStatus()).isEqualTo(Article.STATUS_DRAFT);
    }

    @Test
    @DisplayName("全文检索能找到刚插入的文章")
    void searchByKeyword_shouldMatchInsertedArticle() {
        Long authorId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        Long categoryId = jdbcTemplate.queryForObject("SELECT id FROM categories WHERE name = '技术分享'", Long.class);

        Article article = new Article();
        article.setTitle("Fulltext DAO Test");
        article.setContent("fulltext searchable content");
        article.setSummary("fulltext summary");
        article.setCategoryId(categoryId);
        article.setAuthorId(authorId);
        article.setStatus(Article.STATUS_PUBLISHED);
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setCommentCount(0);
        article.setFavoriteCount(0);
        article.setIsTop(0);
        article.setIsRecommended(0);
        article.setPublishTime(LocalDateTime.now());

        articleMapper.insert(article);

        List<Article> hits = articleMapper.searchByKeyword("Fulltext", 0, 10);
        assertThat(hits).extracting(Article::getId).contains(article.getId());
    }

    @Test
    @DisplayName("更新文章计数字段")
    void updateArticleCounts_shouldAffectRows() {
        Long authorId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        Long categoryId = jdbcTemplate.queryForObject("SELECT id FROM categories WHERE name = '技术分享'", Long.class);

        Article article = new Article();
        article.setTitle("Count DAO Test");
        article.setContent("count content");
        article.setSummary("count summary");
        article.setCategoryId(categoryId);
        article.setAuthorId(authorId);
        article.setStatus(Article.STATUS_PUBLISHED);
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setCommentCount(0);
        article.setFavoriteCount(0);
        article.setIsTop(0);
        article.setIsRecommended(0);
        article.setPublishTime(LocalDateTime.now());

        articleMapper.insert(article);

        int viewResult = articleMapper.incrementViewCount(article.getId());
        assertThat(viewResult).isEqualTo(1);

        int likeResult = articleMapper.updateLikeCount(article.getId(), 1);
        assertThat(likeResult).isEqualTo(1);

        int commentResult = articleMapper.updateCommentCount(article.getId(), 1);
        assertThat(commentResult).isEqualTo(1);

        int favoriteResult = articleMapper.updateFavoriteCount(article.getId(), 1);
        assertThat(favoriteResult).isEqualTo(1);
    }
}
