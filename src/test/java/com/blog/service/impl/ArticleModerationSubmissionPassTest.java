package com.blog.service.impl;

import com.blog.dto.ModerationResult;
import com.blog.entity.Article;
import com.blog.entity.ArticleModerationSubmission;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.ArticleModerationSubmissionMapper;
import com.blog.service.ArticleStatusTransitionService;
import com.blog.service.ContentModerationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleModerationSubmissionPassTest {

    @Mock private ArticleModerationSubmissionMapper submissionMapper;
    @Mock private ArticleMapper articleMapper;
    @Mock private ContentModerationService contentModerationService;
    @Mock private ArticleStatusTransitionService articleStatusTransition;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private ArticleModerationSubmissionServiceImpl service;

    @Test
    void editPass_shouldApplySnapshotWithoutPublishing() {
        Article current = new Article();
        current.setId(7L);
        current.setStatus(Article.STATUS_DRAFT);
        current.setTitle("old title");
        current.setContent("old content");
        ArticleModerationSubmission submission = ArticleModerationSubmission.edit(
                7L, current, "new title", "new content", "new summary", null, 1L);
        submission.setSubmissionToken("edit-pass");
        when(submissionMapper.claimForProcessing("edit-pass")).thenReturn(1);
        when(submissionMapper.selectBySubmissionToken("edit-pass")).thenReturn(submission);
        when(contentModerationService.moderateArticle("new title", "new content"))
                .thenReturn(com.blog.common.Result.success(ModerationResult.pass()));
        when(articleMapper.selectById(7L)).thenReturn(current);
        when(submissionMapper.completeAi("edit-pass", ArticleModerationSubmission.Status.PASSED, null))
                .thenReturn(1);

        service.process("edit-pass");

        // EDIT 通过只落盘快照：publish 永不调用（管理员的下架状态必须保留）
        verify(articleStatusTransition, never()).publish(any());
        verify(articleMapper).updateById(current);
        assertThat(current.getTitle()).isEqualTo("new title");
        assertThat(current.getContent()).isEqualTo("new content");
        assertThat(current.getStatus()).isEqualTo(Article.STATUS_DRAFT);
    }

    @Test
    void newPass_shouldPublish() {
        Article current = new Article();
        current.setId(7L);
        current.setStatus(Article.STATUS_DRAFT);
        ArticleModerationSubmission submission = ArticleModerationSubmission.newSubmission(current);
        submission.setSubmissionToken("new-pass");
        when(submissionMapper.claimForProcessing("new-pass")).thenReturn(1);
        when(submissionMapper.selectBySubmissionToken("new-pass")).thenReturn(submission);
        when(contentModerationService.moderateArticle(any(), any()))
                .thenReturn(com.blog.common.Result.success(ModerationResult.pass()));
        when(articleMapper.selectById(7L)).thenReturn(current);
        when(submissionMapper.completeAi("new-pass", ArticleModerationSubmission.Status.PASSED, null))
                .thenReturn(1);

        service.process("new-pass");

        verify(articleStatusTransition).publish(current);
        verify(articleMapper, never()).updateById(any());
    }
}
