package com.blog.service.impl;

import com.blog.dto.ModerationResult;
import com.blog.entity.Article;
import com.blog.entity.ArticleModerationSubmission;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.ArticleModerationSubmissionMapper;
import com.blog.service.ArticleRankService;
import com.blog.service.ContentModerationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class ArticleModerationSubmissionTest {
    @Mock private ArticleModerationSubmissionMapper submissionMapper;
    @Mock private ArticleMapper articleMapper;
    @Mock private ContentModerationService contentModerationService;
    @Mock private ArticleRankService articleRankService;
    @InjectMocks private ArticleModerationSubmissionServiceImpl service;

    @Test
    void failedAiResultKeepsPublishedArticleUntouchedAndSchedulesRetry() {
        Article article = new Article();
        article.setId(7L);
        article.setStatus(Article.STATUS_PUBLISHED);
        article.setTitle("old title");
        article.setContent("old content");
        ArticleModerationSubmission submission = ArticleModerationSubmission.edit(7L, article, "new title", "new content", null, null, 1L);
        submission.setSubmissionToken("submission-token");
        when(submissionMapper.claimForProcessing("submission-token")).thenReturn(1);
        when(submissionMapper.selectBySubmissionToken("submission-token")).thenReturn(submission);
        when(contentModerationService.moderateArticle("new title", "new content")).thenThrow(new IllegalStateException("AI unavailable"));

        service.process("submission-token");

        verify(articleMapper, never()).updateById(any());
        verify(submissionMapper).scheduleRetry(eq("submission-token"), eq(1), any(), contains("AI unavailable"));
        verifyNoInteractions(articleRankService);
        assertThat(article.getTitle()).isEqualTo("old title");
    }

    @Test
    void passedEditAtomicallyAppliesSnapshotOnlyAfterClaimingTask() {
        Article current = new Article();
        current.setId(7L);
        current.setStatus(Article.STATUS_PUBLISHED);
        current.setTitle("old title");
        current.setContent("old content");
        ArticleModerationSubmission submission = ArticleModerationSubmission.edit(7L, current, "new title", "new content", "new summary", null, 1L);
        submission.setSubmissionToken("pass-token");
        when(submissionMapper.claimForProcessing("pass-token")).thenReturn(1);
        when(submissionMapper.selectBySubmissionToken("pass-token")).thenReturn(submission);
        when(contentModerationService.moderateArticle("new title", "new content")).thenReturn(com.blog.common.Result.success(ModerationResult.pass()));
        when(articleMapper.selectById(7L)).thenReturn(current);
        when(articleMapper.updateById(current)).thenReturn(1);
        when(submissionMapper.completeAi("pass-token", ArticleModerationSubmission.Status.PASSED, null)).thenReturn(1);

        service.process("pass-token");

        assertThat(current.getTitle()).isEqualTo("new title");
        assertThat(current.getContent()).isEqualTo("new content");
        assertThat(current.getStatus()).isEqualTo(Article.STATUS_PUBLISHED);
        verify(articleRankService).initializeArticle(7L);
    }

    @Test
    void fourthFailureMovesTaskToManualReviewWithoutPublishing() {
        Article current = new Article();
        current.setId(7L);
        current.setStatus(Article.STATUS_DRAFT);
        ArticleModerationSubmission submission = ArticleModerationSubmission.newSubmission(current);
        submission.setSubmissionToken("manual-token");
        submission.setRetryCount(3);
        when(submissionMapper.claimForProcessing("manual-token")).thenReturn(1);
        when(submissionMapper.selectBySubmissionToken("manual-token")).thenReturn(submission);
        when(contentModerationService.moderateArticle(any(), any())).thenReturn(com.blog.common.Result.success(null));

        service.process("manual-token");

        verify(submissionMapper).moveToManualReview(eq("manual-token"), contains("无效"));
        verify(articleMapper, never()).updateById(any());
        verifyNoInteractions(articleRankService);
    }

    @Test
    void duplicateEventCannotCallAiTwiceWhenConditionalClaimFails() {
        when(submissionMapper.claimForProcessing("already-claimed")).thenReturn(0);

        service.process("already-claimed");

        verifyNoInteractions(contentModerationService, articleMapper, articleRankService);
    }

    @Test
    void submissionLongerThanModerationLimitIsRejectedBeforePersistence() {
        Article article = new Article();
        article.setId(7L);
        article.setContent("safe".repeat(1000) + "<script>alert(1)</script>");

        assertThatThrownBy(() -> service.submitNew(article))
                .hasMessageContaining("不能超过4000");

        verifyNoInteractions(submissionMapper);
    }

    @Test
    void exactly4000CharactersCanCreateSubmission() {
        Article article = new Article();
        article.setId(7L);
        article.setContent("safe".repeat(1000));
        when(submissionMapper.insert(any())).thenReturn(1);

        String token = service.submitNew(article);

        assertThat(token).isNotBlank();
        verify(submissionMapper).insert(any(ArticleModerationSubmission.class));
    }

    @Test
    void aiPassCannotPublishLegacySnapshotBeyondModerationLimit() {
        Article article = new Article();
        article.setId(7L);
        article.setStatus(Article.STATUS_DRAFT);
        article.setContent("old content");
        ArticleModerationSubmission submission = ArticleModerationSubmission.newSubmission(article);
        submission.setSubmissionToken("long-ai-pass");
        submission.setContent("safe".repeat(1000) + "<script>alert(1)</script>");
        when(submissionMapper.claimForProcessing("long-ai-pass")).thenReturn(1);
        when(submissionMapper.selectBySubmissionToken("long-ai-pass")).thenReturn(submission);
        when(contentModerationService.moderateArticle(any(), any()))
                .thenReturn(com.blog.common.Result.success(ModerationResult.pass()));

        service.process("long-ai-pass");

        verify(articleMapper, never()).updateById(any());
        verify(submissionMapper).scheduleRetry(eq("long-ai-pass"), eq(1), any(), contains("不能超过4000"));
        verifyNoInteractions(articleRankService);
    }

    @Test
    void staleProcessingUsesTheSameRetryScheduleAndEventuallyRequiresManualReview() {
        ArticleModerationSubmission first = ArticleModerationSubmission.newSubmission(new Article());
        first.setSubmissionToken("stale-first");
        first.setRetryCount(0);
        ArticleModerationSubmission exhausted = ArticleModerationSubmission.newSubmission(new Article());
        exhausted.setSubmissionToken("stale-exhausted");
        exhausted.setRetryCount(3);
        when(submissionMapper.selectStaleProcessing(any())).thenReturn(List.of(first, exhausted));

        int recovered = service.recoverStaleProcessing();

        assertThat(recovered).isEqualTo(2);
        verify(submissionMapper).scheduleRetry(eq("stale-first"), eq(1), any(LocalDateTime.class), contains("中断"));
        verify(submissionMapper).moveToManualReview(eq("stale-exhausted"), contains("中断"));
    }

    @Test
    void manualRejectionRecordsAuditorAndReasonAndKeepsEditPublicVersion() {
        Article publicArticle = new Article();
        publicArticle.setId(7L);
        publicArticle.setStatus(Article.STATUS_PUBLISHED);
        publicArticle.setTitle("public title");
        ArticleModerationSubmission edit = ArticleModerationSubmission.edit(7L, publicArticle, "untrusted edit", "untrusted", null, null, 1L);
        edit.setSubmissionToken("manual-reject");
        when(submissionMapper.claimForManualDecision("manual-reject")).thenReturn(1);
        when(submissionMapper.selectBySubmissionToken("manual-reject")).thenReturn(edit);
        when(submissionMapper.completeManually("manual-reject", ArticleModerationSubmission.Status.REJECTED, 99L, "policy reason")).thenReturn(1);

        service.reject("manual-reject", 99L, "policy reason");

        verify(articleMapper, never()).updateById(any());
        verify(submissionMapper).completeManually("manual-reject", ArticleModerationSubmission.Status.REJECTED, 99L, "policy reason");
        assertThat(publicArticle.getTitle()).isEqualTo("public title");
    }

    @Test
    void manualRejectionOfNewSubmissionLeavesArticleAsDraftAndAuditsDecision() {
        Article draft = new Article();
        draft.setId(8L);
        draft.setStatus(Article.STATUS_DRAFT);
        ArticleModerationSubmission submission = ArticleModerationSubmission.newSubmission(draft);
        submission.setSubmissionToken("new-reject");
        when(submissionMapper.claimForManualDecision("new-reject")).thenReturn(1);
        when(submissionMapper.selectBySubmissionToken("new-reject")).thenReturn(submission);
        when(articleMapper.selectById(8L)).thenReturn(draft);
        when(articleMapper.updateById(draft)).thenReturn(1);
        when(submissionMapper.completeManually("new-reject", ArticleModerationSubmission.Status.REJECTED, 99L, "policy reason")).thenReturn(1);

        service.reject("new-reject", 99L, "policy reason");

        assertThat(draft.getStatus()).isEqualTo(Article.STATUS_DRAFT);
        verify(submissionMapper).completeManually("new-reject", ArticleModerationSubmission.Status.REJECTED, 99L, "policy reason");
    }

    @Test
    void manualDecisionCannotOverwriteSubmissionAlreadyClaimedByAi() {
        when(submissionMapper.claimForManualDecision("ai-claimed")).thenReturn(0);

        assertThatThrownBy(() -> service.approve("ai-claimed", 99L, "manual reason"))
                .hasMessageContaining("审核任务不存在或已被处理");

        verify(submissionMapper, never()).selectBySubmissionToken("ai-claimed");
        verifyNoInteractions(articleMapper, articleRankService);
    }

    @Test
    void manualApprovalCannotPublishLegacySnapshotBeyondModerationLimit() {
        Article article = new Article();
        article.setId(7L);
        ArticleModerationSubmission submission = ArticleModerationSubmission.newSubmission(article);
        submission.setSubmissionToken("long-manual-approve");
        submission.setContent("safe".repeat(1000) + "<script>alert(1)</script>");
        when(submissionMapper.claimForManualDecision("long-manual-approve")).thenReturn(1);
        when(submissionMapper.selectBySubmissionToken("long-manual-approve")).thenReturn(submission);

        assertThatThrownBy(() -> service.approve("long-manual-approve", 99L, "reviewed"))
                .hasMessageContaining("不能超过4000");

        verify(articleMapper, never()).updateById(any());
        verify(submissionMapper, never()).completeManually(any(), any(), any(), any());
        verifyNoInteractions(articleRankService);
    }

    @Test
    void manualApprovalClaimsSubmissionBeforeMutatingArticleAndWritesAuditAtomically() {
        Article current = new Article();
        current.setId(10L);
        current.setStatus(Article.STATUS_DRAFT);
        ArticleModerationSubmission submission = ArticleModerationSubmission.newSubmission(current);
        submission.setSubmissionToken("manual-approve");
        when(submissionMapper.claimForManualDecision("manual-approve")).thenReturn(1);
        when(submissionMapper.selectBySubmissionToken("manual-approve")).thenReturn(submission);
        when(articleMapper.selectById(10L)).thenReturn(current);
        when(articleMapper.updateById(current)).thenReturn(1);
        when(submissionMapper.completeManually("manual-approve", ArticleModerationSubmission.Status.PASSED, 99L, "reviewed")).thenReturn(1);

        service.approve("manual-approve", 99L, "reviewed");

        var order = inOrder(submissionMapper, articleMapper);
        order.verify(submissionMapper).claimForManualDecision("manual-approve");
        order.verify(submissionMapper).selectBySubmissionToken("manual-approve");
        order.verify(articleMapper).selectById(10L);
        order.verify(articleMapper).updateById(current);
        order.verify(submissionMapper).completeManually("manual-approve", ArticleModerationSubmission.Status.PASSED, 99L, "reviewed");
        assertThat(current.getStatus()).isEqualTo(Article.STATUS_PUBLISHED);
        verify(articleRankService).initializeArticle(10L);
    }
}
