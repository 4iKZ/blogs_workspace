package com.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/** Immutable content snapshot queued for article moderation. */
@Data
@TableName("article_moderation_submissions")
public class ArticleModerationSubmission {
    public enum SubmissionType { NEW, EDIT }
    public enum Status { PENDING, PROCESSING, RETRY, PASSED, REJECTED, MANUAL_REVIEW }

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long articleId;
    /** articleId while active, null once terminal; unique index enforces one active submission. */
    private Long activeArticleId;
    private String submissionToken;
    private String title;
    private String summary;
    private String content;
    private String coverImage;
    private Long categoryId;
    private Long topicId;
    private Integer allowComment;
    private SubmissionType submissionType;
    private Status status;
    private Integer retryCount;
    private LocalDateTime nextRetryAt;
    private LocalDateTime processingStartedAt;
    private String lastError;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private Long reviewedBy;
    private String reviewReason;
    private LocalDateTime manualActionAt;

    public static ArticleModerationSubmission newSubmission(Article article) {
        return snapshot(article.getId(), article, SubmissionType.NEW);
    }

    public static ArticleModerationSubmission edit(Long articleId, Article existing, String title, String content,
                                                    String summary, String coverImage, Long categoryId) {
        Article candidate = new Article();
        candidate.setTitle(title);
        candidate.setContent(content);
        candidate.setSummary(summary);
        candidate.setCoverImage(coverImage);
        candidate.setCategoryId(categoryId);
        candidate.setTopicId(existing.getTopicId());
        candidate.setAllowComment(existing.getAllowComment());
        return snapshot(articleId, candidate, SubmissionType.EDIT);
    }

    public static ArticleModerationSubmission snapshot(Long articleId, Article article, SubmissionType type) {
        ArticleModerationSubmission submission = new ArticleModerationSubmission();
        submission.articleId = articleId;
        submission.activeArticleId = articleId;
        submission.submissionToken = UUID.randomUUID().toString();
        submission.title = article.getTitle();
        submission.summary = article.getSummary();
        submission.content = article.getContent();
        submission.coverImage = article.getCoverImage();
        submission.categoryId = article.getCategoryId();
        submission.topicId = article.getTopicId();
        submission.allowComment = article.getAllowComment();
        submission.submissionType = type;
        submission.status = Status.PENDING;
        submission.retryCount = 0;
        submission.submittedAt = LocalDateTime.now();
        return submission;
    }
}
