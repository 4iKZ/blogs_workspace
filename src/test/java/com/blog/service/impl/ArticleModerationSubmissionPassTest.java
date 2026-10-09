package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.blog.dto.ModerationResult;
import com.blog.entity.Article;
import com.blog.entity.ArticleModerationSubmission;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.ArticleModerationSubmissionMapper;
import com.blog.service.ArticleStatusTransitionService;
import com.blog.service.ContentModerationService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    @BeforeAll
    static void initLambdaCache() {
        // 手动初始化 Article 的 Lambda 列缓存，避免纯 Mockito 下 LambdaUpdateWrapper 解析列名失败
        Configuration configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "test"), Article.class);
    }

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
        verify(articleMapper).update(any(), any());
        assertThat(current.getTitle()).isEqualTo("new title");
        assertThat(current.getContent()).isEqualTo("new content");
        assertThat(current.getStatus()).isEqualTo(Article.STATUS_DRAFT);
    }

    @Test
    void editPass_shouldApplyAllowCommentSnapshot() {
        Article current = new Article();
        current.setId(7L);
        current.setStatus(Article.STATUS_DRAFT);
        current.setAllowComment(0);
        ArticleModerationSubmission submission = ArticleModerationSubmission.edit(
                7L, current, "new title", "new content", "new summary", null, 1L);
        submission.setSubmissionToken("edit-allow");
        when(submissionMapper.claimForProcessing("edit-allow")).thenReturn(1);
        when(submissionMapper.selectBySubmissionToken("edit-allow")).thenReturn(submission);
        when(contentModerationService.moderateArticle("new title", "new content"))
                .thenReturn(com.blog.common.Result.success(ModerationResult.pass()));
        when(articleMapper.selectById(7L)).thenReturn(current);
        when(submissionMapper.completeAi("edit-allow", ArticleModerationSubmission.Status.PASSED, null))
                .thenReturn(1);

        service.process("edit-allow");

        // allowComment 已为真实库字段，快照非 null 时应写入 allow_comment
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaUpdateWrapper<Article>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(articleMapper).update(any(), captor.capture());
        assertThat(captor.getValue().getSqlSet()).contains("allow_comment");
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

    @Test
    void newPass_articleOfflinedByAdmin_shouldRejectWithoutPublishing() {
        Article current = new Article();
        current.setId(7L);
        current.setStatus(Article.STATUS_DELETED);
        ArticleModerationSubmission submission = ArticleModerationSubmission.newSubmission(current);
        submission.setSubmissionToken("new-offline");
        when(submissionMapper.claimForProcessing("new-offline")).thenReturn(1);
        when(submissionMapper.selectBySubmissionToken("new-offline")).thenReturn(submission);
        when(contentModerationService.moderateArticle(any(), any()))
                .thenReturn(com.blog.common.Result.success(ModerationResult.pass()));
        when(articleMapper.selectById(7L)).thenReturn(current);
        when(submissionMapper.completeAi("new-offline", ArticleModerationSubmission.Status.REJECTED,
                "文章已被管理员下线，不再发布")).thenReturn(1);

        service.process("new-offline");

        // 管理员已下线的文章：审核通过也不得发布，任务以拒绝结束
        verify(articleStatusTransition, never()).publish(any());
        verify(articleMapper, never()).update(any(), any());
        verify(submissionMapper).completeAi("new-offline", ArticleModerationSubmission.Status.REJECTED,
                "文章已被管理员下线，不再发布");
    }
}
