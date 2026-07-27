package com.blog.service;

import com.blog.entity.Article;
import com.blog.entity.ArticleModerationSubmission;

import java.util.List;

public interface ArticleModerationSubmissionService {
    String submitNew(Article article);
    String submitEdit(Article article, Article candidate);
    void process(String submissionToken);
    void processDueSubmissions();
    void approve(String submissionToken, Long adminId, String reason);
    void reject(String submissionToken, Long adminId, String reason);
    List<ArticleModerationSubmission> listDueSubmissions();
    List<ArticleModerationSubmission> list(ArticleModerationSubmission.Status status);
    int recoverStaleProcessing();
}
