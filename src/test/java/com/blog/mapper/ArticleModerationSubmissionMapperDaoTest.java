package com.blog.mapper;

import com.blog.entity.ArticleModerationSubmission;
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
@DisplayName("ArticleModerationSubmissionMapper DAO 直测")
class ArticleModerationSubmissionMapperDaoTest {

    @Autowired
    private ArticleModerationSubmissionMapper submissionMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanup() {
        jdbcTemplate.execute("DELETE FROM article_moderation_submissions WHERE submission_token LIKE 'dao-test-%'");
    }

    private Long articleId() {
        return jdbcTemplate.queryForObject("SELECT id FROM articles WHERE title = 'Spring Boot 快速入门指南'", Long.class);
    }

    private Long adminId() {
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
    }

    private ArticleModerationSubmission newSubmission(String token, ArticleModerationSubmission.Status status) {
        ArticleModerationSubmission submission = new ArticleModerationSubmission();
        submission.setArticleId(articleId());
        submission.setActiveArticleId(articleId());
        submission.setSubmissionToken(token);
        submission.setTitle("dao-test-title");
        submission.setContent("dao-test-content");
        submission.setSubmissionType(ArticleModerationSubmission.SubmissionType.NEW);
        submission.setStatus(status);
        submission.setRetryCount(0);
        submission.setSubmittedAt(LocalDateTime.now());
        return submission;
    }

    @Test
    @DisplayName("认领处理并完成审核的生命周期")
    void submissionLifecycle_claimAndComplete() {
        String token = "dao-test-lc-" + System.nanoTime();
        submissionMapper.insert(newSubmission(token, ArticleModerationSubmission.Status.PENDING));

        ArticleModerationSubmission found = submissionMapper.selectBySubmissionToken(token);
        assertThat(found).isNotNull();
        assertThat(found.getStatus()).isEqualTo(ArticleModerationSubmission.Status.PENDING);

        assertThat(submissionMapper.claimForProcessing(token)).isEqualTo(1);
        assertThat(submissionMapper.selectBySubmissionToken(token).getStatus())
                .isEqualTo(ArticleModerationSubmission.Status.PROCESSING);

        assertThat(submissionMapper.completeAi(token, ArticleModerationSubmission.Status.PASSED, "dao-test-ok")).isEqualTo(1);
        ArticleModerationSubmission finished = submissionMapper.selectBySubmissionToken(token);
        assertThat(finished.getStatus()).isEqualTo(ArticleModerationSubmission.Status.PASSED);
        assertThat(finished.getActiveArticleId()).isNull();

        assertThat(submissionMapper.claimForProcessing(token)).isEqualTo(0);
    }

    @Test
    @DisplayName("人工认领并人工完成审核")
    void manualDecisionFlow_shouldPersistReviewInfo() {
        String token = "dao-test-manual-" + System.nanoTime();
        submissionMapper.insert(newSubmission(token, ArticleModerationSubmission.Status.PENDING));

        assertThat(submissionMapper.claimForManualDecision(token)).isEqualTo(1);
        assertThat(submissionMapper.selectBySubmissionToken(token).getStatus())
                .isEqualTo(ArticleModerationSubmission.Status.PROCESSING);

        Long adminId = adminId();
        assertThat(submissionMapper.completeManually(token, ArticleModerationSubmission.Status.REJECTED, adminId,
                "dao-test-reason")).isEqualTo(1);
        ArticleModerationSubmission finished = submissionMapper.selectBySubmissionToken(token);
        assertThat(finished.getStatus()).isEqualTo(ArticleModerationSubmission.Status.REJECTED);
        assertThat(finished.getReviewedBy()).isEqualTo(adminId);
        assertThat(finished.getReviewReason()).isEqualTo("dao-test-reason");
        assertThat(finished.getActiveArticleId()).isNull();
    }

    @Test
    @DisplayName("重试调度与升级人工复核")
    void retrySchedule_andEscalateToManualReview() {
        String token = "dao-test-retry-" + System.nanoTime();
        submissionMapper.insert(newSubmission(token, ArticleModerationSubmission.Status.PENDING));

        assertThat(submissionMapper.claimForProcessing(token)).isEqualTo(1);

        assertThat(submissionMapper.scheduleRetry(token, 1, LocalDateTime.now().minusMinutes(1), "dao-test-error"))
                .isEqualTo(1);
        ArticleModerationSubmission afterRetry = submissionMapper.selectBySubmissionToken(token);
        assertThat(afterRetry.getStatus()).isEqualTo(ArticleModerationSubmission.Status.RETRY);
        assertThat(afterRetry.getRetryCount()).isEqualTo(1);
        assertThat(afterRetry.getLastError()).isEqualTo("dao-test-error");

        assertThat(submissionMapper.claimForProcessing(token)).isEqualTo(1);
        assertThat(submissionMapper.moveToManualReview(token, "dao-test-limit")).isEqualTo(1);
        assertThat(submissionMapper.selectBySubmissionToken(token).getStatus())
                .isEqualTo(ArticleModerationSubmission.Status.MANUAL_REVIEW);
    }

    @Test
    @DisplayName("到期与卡死提交的批量查询")
    void dueAndStale_shouldBeSelected() {
        String pendingToken = "dao-test-due-" + System.nanoTime();
        ArticleModerationSubmission pending = newSubmission(pendingToken, ArticleModerationSubmission.Status.PENDING);
        pending.setActiveArticleId(null);
        submissionMapper.insert(pending);

        String retryDueToken = "dao-test-due-retry-" + System.nanoTime();
        ArticleModerationSubmission retryDue = newSubmission(retryDueToken, ArticleModerationSubmission.Status.RETRY);
        retryDue.setActiveArticleId(null);
        retryDue.setNextRetryAt(LocalDateTime.now().minusMinutes(5));
        submissionMapper.insert(retryDue);

        String retryFutureToken = "dao-test-due-future-" + System.nanoTime();
        ArticleModerationSubmission retryFuture = newSubmission(retryFutureToken, ArticleModerationSubmission.Status.RETRY);
        retryFuture.setActiveArticleId(null);
        retryFuture.setNextRetryAt(LocalDateTime.now().plusHours(2));
        submissionMapper.insert(retryFuture);

        List<String> dueTokens = submissionMapper.selectDueSubmissions().stream()
                .map(ArticleModerationSubmission::getSubmissionToken)
                .toList();
        assertThat(dueTokens).contains(pendingToken, retryDueToken).doesNotContain(retryFutureToken);

        String staleToken = "dao-test-stale-" + System.nanoTime();
        ArticleModerationSubmission stale = newSubmission(staleToken, ArticleModerationSubmission.Status.PROCESSING);
        stale.setActiveArticleId(null);
        stale.setProcessingStartedAt(LocalDateTime.now().minusHours(2));
        submissionMapper.insert(stale);

        List<String> staleTokens = submissionMapper.selectStaleProcessing(LocalDateTime.now().minusHours(1)).stream()
                .map(ArticleModerationSubmission::getSubmissionToken)
                .toList();
        assertThat(staleTokens).contains(staleToken);

        List<String> freshTokens = submissionMapper.selectStaleProcessing(LocalDateTime.now().minusHours(3)).stream()
                .map(ArticleModerationSubmission::getSubmissionToken)
                .toList();
        assertThat(freshTokens).doesNotContain(staleToken);
    }
}
