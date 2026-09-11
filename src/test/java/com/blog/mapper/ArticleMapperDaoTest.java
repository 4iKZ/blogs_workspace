package com.blog.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.dto.ArticleDTO;
import com.blog.entity.Article;
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
@DisplayName("ArticleMapper DAO 直测")
class ArticleMapperDaoTest {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanup() {
        jdbcTemplate.execute("DELETE FROM articles WHERE title LIKE 'dao-test-%'");
    }

    private Long adminId() {
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
    }

    private Long techCategoryId() {
        return jdbcTemplate.queryForObject("SELECT id FROM categories WHERE name = '技术分享'", Long.class);
    }

    private Article buildArticle(String title, int status, int isTop, int isRecommended) {
        Article article = new Article();
        article.setTitle(title);
        article.setContent("dao test content " + title);
        article.setSummary("dao test summary " + title);
        article.setCategoryId(techCategoryId());
        article.setAuthorId(adminId());
        article.setStatus(status);
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setCommentCount(0);
        article.setFavoriteCount(0);
        article.setIsTop(isTop);
        article.setIsRecommended(isRecommended);
        article.setPublishTime(LocalDateTime.now());
        return article;
    }

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

        article.setStatus(Article.STATUS_DRAFT);
        int updated = articleMapper.updateById(article);
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

    @Test
    @DisplayName("安全扣减评论数 - 正常扣减")
    void decrementCommentCountSafely_shouldDecrementByCount() {
        Article article = buildArticle("dao-test-safedec-" + System.nanoTime(), Article.STATUS_PUBLISHED, 0, 0);
        article.setCommentCount(3);
        articleMapper.insert(article);

        int updated = articleMapper.decrementCommentCountSafelyByCount(article.getId(), 2);
        assertThat(updated).isEqualTo(1);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT comment_count FROM articles WHERE id = ?", Integer.class, article.getId());
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("安全扣减评论数 - 扣减超过现有数量时不下溢为负数")
    void decrementCommentCountSafely_shouldNotGoNegative() {
        Article article = buildArticle("dao-test-safedec-" + System.nanoTime(), Article.STATUS_PUBLISHED, 0, 0);
        article.setCommentCount(2);
        articleMapper.insert(article);

        int updated = articleMapper.decrementCommentCountSafelyByCount(article.getId(), 5);
        assertThat(updated).isEqualTo(1);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT comment_count FROM articles WHERE id = ?", Integer.class, article.getId());
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("时间范围热门文章查询")
    void selectHotArticlesByRange_shouldReturnInsertedRows() {
        String token = "daotok" + System.nanoTime();
        Article article = buildArticle("dao-test-hot-range-" + token, Article.STATUS_PUBLISHED, 0, 0);
        article.setViewCount(99);
        article.setPublishTime(LocalDateTime.now().minusMinutes(5));
        articleMapper.insert(article);

        List<Article> hits = articleMapper.selectHotArticlesByRange(10,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusMinutes(1));
        assertThat(hits).extracting(Article::getId).contains(article.getId());
    }

    @Test
    @DisplayName("按作者查询文章（可选状态过滤）")
    void selectArticlesByUserId_shouldFilterByStatus() {
        String token = "daotok" + System.nanoTime();
        Article published = buildArticle("dao-test-user-" + token, Article.STATUS_PUBLISHED, 0, 0);
        articleMapper.insert(published);
        Article draft = buildArticle("dao-test-user-draft-" + token, Article.STATUS_DRAFT, 0, 0);
        articleMapper.insert(draft);

        List<Article> all = articleMapper.selectArticlesByUserId(adminId(), null);
        assertThat(all).extracting(Article::getId).contains(published.getId(), draft.getId());

        List<Article> publishedOnly = articleMapper.selectArticlesByUserId(adminId(), Article.STATUS_PUBLISHED);
        assertThat(publishedOnly).extracting(Article::getId).contains(published.getId())
                .doesNotContain(draft.getId());
    }

    @Test
    @DisplayName("按分类/作者查询已发布文章")
    void selectByCategoryAndAuthor_shouldReturnInsertedRows() {
        String token = "daotok" + System.nanoTime();
        Article article = buildArticle("dao-test-cat-" + token, Article.STATUS_PUBLISHED, 0, 0);
        articleMapper.insert(article);

        assertThat(articleMapper.selectByCategoryId(techCategoryId(), 0, 10))
                .extracting(Article::getId).contains(article.getId());
        assertThat(articleMapper.selectByAuthorId(adminId(), 0, 10))
                .extracting(Article::getId).contains(article.getId());
    }

    @Test
    @DisplayName("批量浏览量增加")
    void incrementViewCountBatch_shouldAddIncrement() {
        Article article = buildArticle("dao-test-view-batch-" + System.nanoTime(), Article.STATUS_PUBLISHED, 0, 0);
        articleMapper.insert(article);

        assertThat(articleMapper.incrementViewCountBatch(article.getId(), 7)).isEqualTo(1);

        Article updated = articleMapper.selectById(article.getId());
        assertThat(updated.getViewCount()).isEqualTo(7);
    }

    @Test
    @DisplayName("计数安全递减与下限保护")
    void decrementCountsSafely_shouldNotGoNegative() {
        Article article = buildArticle("dao-test-decr-" + System.nanoTime(), Article.STATUS_PUBLISHED, 0, 0);
        article.setLikeCount(5);
        article.setCommentCount(5);
        article.setFavoriteCount(5);
        articleMapper.insert(article);

        assertThat(articleMapper.decrementLikeCountSafely(article.getId())).isEqualTo(1);
        assertThat(articleMapper.decrementCommentCountSafely(article.getId())).isEqualTo(1);
        assertThat(articleMapper.decrementFavoriteCountSafely(article.getId())).isEqualTo(1);

        Article updated = articleMapper.selectById(article.getId());
        assertThat(updated.getLikeCount()).isEqualTo(4);
        assertThat(updated.getCommentCount()).isEqualTo(4);
        assertThat(updated.getFavoriteCount()).isEqualTo(4);

        Article zeroArticle = buildArticle("dao-test-decr-zero-" + System.nanoTime(), Article.STATUS_PUBLISHED, 0, 0);
        articleMapper.insert(zeroArticle);
        assertThat(articleMapper.decrementLikeCountSafely(zeroArticle.getId())).isZero();
    }

    @Test
    @DisplayName("热门搜索关键词")
    void getHotSearchKeywords_shouldReturnInsertedTitle() {
        String token = "daosugg" + System.nanoTime();
        Article article = buildArticle("dao-test-sugg-" + token, Article.STATUS_PUBLISHED, 0, 0);
        article.setViewCount(9999);
        articleMapper.insert(article);

        assertThat(articleMapper.getHotSearchKeywords(10)).contains(article.getTitle());
    }

    @Test
    @DisplayName("分页查询已发布文章（关键词过滤）")
    void selectPublishedArticles_shouldReturnPagedRows() {
        String token = "daoarticle" + System.nanoTime();
        Article article = buildArticle("dao-test-page-" + token, Article.STATUS_PUBLISHED, 0, 0);
        articleMapper.insert(article);

        Page<Article> page = new Page<>(1, 10);
        IPage<Article> result = articleMapper.selectPublishedArticles(page, Article.STATUS_PUBLISHED, null, null, token);
        assertThat(result.getRecords()).extracting(Article::getId).contains(article.getId());
        assertThat(result.getTotal()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("全文索引分页查询已发布文章")
    void selectPublishedByFulltext_shouldMatchInsertedArticle() {
        String token = "daofulltext" + System.nanoTime();
        Article article = buildArticle("dao-test-ft-" + token, Article.STATUS_PUBLISHED, 0, 0);
        article.setContent("a distinctive dao fulltext keyword is " + token);
        articleMapper.insert(article);

        Page<Article> page = new Page<>(1, 10);
        IPage<Article> result = articleMapper.selectPublishedByFulltext(page, Article.STATUS_PUBLISHED,
                token, null, null, null);
        assertThat(result.getRecords()).extracting(Article::getId).contains(article.getId());
    }
}
