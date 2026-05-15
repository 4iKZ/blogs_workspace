package com.blog.service;

import com.blog.common.Result;
import com.blog.dto.ModerationResult;
import com.blog.entity.ArticleModerationLog;

/**
 * 文章审核记录服务接口
 */
public interface ArticleModerationLogService {

    /**
     * 保存审核记录
     *
     * @param articleId 文章ID
     * @param title 文章标题
     * @param content 文章内容摘要
     * @param moderationResult 审核结果
     * @return 保存的记录ID
     */
    Result<Long> saveModerationLog(Long articleId, String title, String content, ModerationResult moderationResult);

    /**
     * 获取文章的最新审核记录
     *
     * @param articleId 文章ID
     * @return 审核记录
     */
    Result<ArticleModerationLog> getLatestLog(Long articleId);
}
