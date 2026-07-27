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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
}
