package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.Result;
import com.blog.entity.Article;
import com.blog.entity.ArticleModerationSubmission;
import com.blog.exception.BusinessException;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.ArticleModerationSubmissionMapper;
import com.blog.service.ArticleModerationSubmissionService;
import com.blog.service.ArticleRankService;
import com.blog.service.ContentModerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Durable, idempotent moderation state machine. No AI failure may publish content. */
@Service
@Slf4j
@RequiredArgsConstructor
public class ArticleModerationSubmissionServiceImpl implements ArticleModerationSubmissionService {
    private static final int[] RETRY_MINUTES = {1, 5, 15};

    private final ArticleModerationSubmissionMapper submissionMapper;
    private final ArticleMapper articleMapper;
    private final ContentModerationService contentModerationService;
    private final ArticleRankService articleRankService;

    @Override
    @Transactional
    public String submitNew(Article article) {
        return insert(ArticleModerationSubmission.newSubmission(article));
    }

    @Override
    @Transactional
    public String submitEdit(Article article, Article candidate) {
        return insert(ArticleModerationSubmission.snapshot(article.getId(), candidate, ArticleModerationSubmission.SubmissionType.EDIT));
    }

    private String insert(ArticleModerationSubmission submission) {
        try {
            if (submissionMapper.insert(submission) != 1) {
                throw new BusinessException("创建审核任务失败");
            }
            return submission.getSubmissionToken();
        } catch (DuplicateKeyException e) {
            throw new BusinessException("文章正在审核中");
        }
    }

    @Override
    @Transactional
    public void process(String submissionToken) {
        if (submissionToken == null || submissionToken.isBlank() || submissionMapper.claimForProcessing(submissionToken) != 1) {
            return;
        }
        ArticleModerationSubmission submission = submissionMapper.selectBySubmissionToken(submissionToken);
        if (submission == null) {
            return;
        }
        try {
            Result<com.blog.dto.ModerationResult> result = contentModerationService.moderateArticle(submission.getTitle(), submission.getContent());
            if (result == null || !result.isSuccess() || result.getData() == null) {
                retryOrManual(submission, "AI审核返回无效结果");
                return;
            }
            if (result.getData().isPassed()) {
                pass(submission, null, null, false);
            } else {
                rejectInternal(submission, null, String.join(",", result.getData().getReasons()), false);
            }
        } catch (RuntimeException e) {
            retryOrManual(submission, safeMessage(e));
        }
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private void retryOrManual(ArticleModerationSubmission submission, String error) {
        int retryCount = submission.getRetryCount() == null ? 0 : submission.getRetryCount();
        if (retryCount >= RETRY_MINUTES.length) {
            submissionMapper.moveToManualReview(submission.getSubmissionToken(), error);
            return;
        }
        int nextRetry = retryCount + 1;
        submissionMapper.scheduleRetry(submission.getSubmissionToken(), nextRetry,
                LocalDateTime.now().plusMinutes(RETRY_MINUTES[nextRetry - 1]), error);
    }

    @Transactional
    protected void pass(ArticleModerationSubmission submission, Long adminId, String reason, boolean manual) {
        Article article = articleMapper.selectById(submission.getArticleId());
        if (article == null) {
            if (manual) submissionMapper.completeManually(submission.getSubmissionToken(), ArticleModerationSubmission.Status.REJECTED, adminId, "文章不存在");
            else submissionMapper.completeAi(submission.getSubmissionToken(), ArticleModerationSubmission.Status.REJECTED, "文章不存在");
            return;
        }
        applySnapshot(article, submission);
        article.setStatus(Article.STATUS_PUBLISHED);
        article.setPublishTime(LocalDateTime.now());
        if (articleMapper.updateById(article) != 1) throw new BusinessException("应用审核快照失败");
        int changed = manual
                ? submissionMapper.completeManually(submission.getSubmissionToken(), ArticleModerationSubmission.Status.PASSED, adminId, reason)
                : submissionMapper.completeAi(submission.getSubmissionToken(), ArticleModerationSubmission.Status.PASSED, reason);
        if (changed != 1) throw new BusinessException("审核任务已被处理");
        articleRankService.initializeArticle(article.getId());
    }

    private void applySnapshot(Article article, ArticleModerationSubmission submission) {
        article.setTitle(submission.getTitle());
        article.setSummary(submission.getSummary());
        article.setContent(submission.getContent());
        article.setCoverImage(submission.getCoverImage());
        article.setCategoryId(submission.getCategoryId());
        article.setTopicId(submission.getTopicId());
        article.setAllowComment(submission.getAllowComment());
    }

    @Transactional
    protected void rejectInternal(ArticleModerationSubmission submission, Long adminId, String reason, boolean manual) {
        if (submission.getSubmissionType() == ArticleModerationSubmission.SubmissionType.NEW) {
            Article article = articleMapper.selectById(submission.getArticleId());
            if (article != null) {
                article.setStatus(Article.STATUS_DRAFT);
                articleMapper.updateById(article);
            }
        }
        int changed = manual
                ? submissionMapper.completeManually(submission.getSubmissionToken(), ArticleModerationSubmission.Status.REJECTED, adminId, reason)
                : submissionMapper.completeAi(submission.getSubmissionToken(), ArticleModerationSubmission.Status.REJECTED, reason);
        if (changed != 1) throw new BusinessException("审核任务已被处理");
    }

    @Override
    public void processDueSubmissions() {
        for (ArticleModerationSubmission submission : submissionMapper.selectDueSubmissions()) process(submission.getSubmissionToken());
    }

    @Override
    @Transactional
    public void approve(String submissionToken, Long adminId, String reason) {
        ArticleModerationSubmission submission = requireOpen(submissionToken);
        pass(submission, adminId, reason, true);
    }

    @Override
    @Transactional
    public void reject(String submissionToken, Long adminId, String reason) {
        rejectInternal(requireOpen(submissionToken), adminId, reason, true);
    }

    private ArticleModerationSubmission requireOpen(String token) {
        ArticleModerationSubmission submission = submissionMapper.selectBySubmissionToken(token);
        if (submission == null || submission.getStatus() == ArticleModerationSubmission.Status.PASSED || submission.getStatus() == ArticleModerationSubmission.Status.REJECTED) {
            throw new BusinessException("审核任务不存在或已完成");
        }
        return submission;
    }

    @Override
    public List<ArticleModerationSubmission> listDueSubmissions() {
        return submissionMapper.selectDueSubmissions();
    }

    @Override
    public List<ArticleModerationSubmission> list(ArticleModerationSubmission.Status status) {
        LambdaQueryWrapper<ArticleModerationSubmission> query = new LambdaQueryWrapper<ArticleModerationSubmission>()
                .orderByDesc(ArticleModerationSubmission::getSubmittedAt);
        if (status != null) query.eq(ArticleModerationSubmission::getStatus, status);
        return submissionMapper.selectList(query);
    }

    @Override
    public int recoverStaleProcessing() {
        return submissionMapper.recoverStaleProcessing(LocalDateTime.now().minusMinutes(15));
    }
}
