package com.blog.service;

import com.blog.common.Result;
import com.blog.dto.ModerationResult;

/**
 * AI内容审核服务接口
 */
public interface ContentModerationService {

    /**
     * 审核文章内容（标题+正文）
     *
     * @param title   文章标题
     * @param content 文章正文
     * @return 审核结果
     */
    Result<ModerationResult> moderateArticle(String title, String content);

    /**
     * 审核评论内容
     *
     * @param content 评论内容
     * @return 审核结果
     */
    Result<ModerationResult> moderateComment(String content);

    /**
     * 审核任意文本内容
     *
     * @param content 待审核内容
     * @return 审核结果
     */
    Result<ModerationResult> moderate(String content);
}
