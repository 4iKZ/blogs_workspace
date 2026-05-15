package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.Result;
import com.blog.dto.ModerationResult;
import com.blog.entity.ArticleModerationLog;
import com.blog.mapper.ArticleModerationLogMapper;
import com.blog.service.ArticleModerationLogService;
import com.blog.utils.BusinessUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 文章审核记录服务实现
 */
@Service
@Slf4j
public class ArticleModerationLogServiceImpl implements ArticleModerationLogService {

    @Autowired
    private ArticleModerationLogMapper moderationLogMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Result<Long> saveModerationLog(Long articleId, String title, String content, ModerationResult moderationResult) {
        try {
            ArticleModerationLog moderationLog = new ArticleModerationLog();
            moderationLog.setArticleId(articleId);
            moderationLog.setTitle(title);
            // 保存内容摘要，最多500字符
            moderationLog.setContent(content != null && content.length() > 500 ? content.substring(0, 500) : content);
            moderationLog.setPassed(moderationResult.isPassed() ? ArticleModerationLog.PASSED : ArticleModerationLog.NOT_PASSED);
            moderationLog.setViolationType(moderationResult.getViolationType());
            moderationLog.setConfidence(moderationResult.getConfidence());
            moderationLog.setCheckTime(LocalDateTime.now());

            // 将违规原因列表转为JSON字符串存储
            if (moderationResult.getReasons() != null && !moderationResult.getReasons().isEmpty()) {
                try {
                    moderationLog.setReasons(objectMapper.writeValueAsString(moderationResult.getReasons()));
                } catch (JsonProcessingException e) {
                    log.warn("序列化违规原因失败", e);
                    moderationLog.setReasons(moderationResult.getReasons().toString());
                }
            }

            int result = moderationLogMapper.insert(moderationLog);
            if (result > 0) {
                log.info("保存审核记录成功，文章ID：{}，是否通过：{}", articleId, moderationResult.isPassed());
                return BusinessUtils.success(moderationLog.getId());
            } else {
                return BusinessUtils.error("保存审核记录失败");
            }
        } catch (Exception e) {
            log.error("保存审核记录异常，文章ID：{}", articleId, e);
            return BusinessUtils.error("保存审核记录异常: " + e.getMessage());
        }
    }

    @Override
    public Result<ArticleModerationLog> getLatestLog(Long articleId) {
        try {
            LambdaQueryWrapper<ArticleModerationLog> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ArticleModerationLog::getArticleId, articleId)
                    .orderByDesc(ArticleModerationLog::getCheckTime)
                    .last("LIMIT 1");

            ArticleModerationLog moderationLog = moderationLogMapper.selectOne(wrapper);
            if (moderationLog != null) {
                return BusinessUtils.success(moderationLog);
            } else {
                return BusinessUtils.error("未找到审核记录");
            }
        } catch (Exception e) {
            log.error("查询审核记录异常，文章ID：{}", articleId, e);
            return BusinessUtils.error("查询审核记录异常: " + e.getMessage());
        }
    }
}
