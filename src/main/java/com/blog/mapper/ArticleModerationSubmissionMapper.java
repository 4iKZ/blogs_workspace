package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.ArticleModerationSubmission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ArticleModerationSubmissionMapper extends BaseMapper<ArticleModerationSubmission> {
    @Select("SELECT * FROM article_moderation_submissions WHERE submission_token = #{token}")
    ArticleModerationSubmission selectBySubmissionToken(@Param("token") String token);

    @Update("UPDATE article_moderation_submissions SET status = 'PROCESSING', processing_started_at = CURRENT_TIMESTAMP, update_time = CURRENT_TIMESTAMP " +
            "WHERE submission_token = #{token} AND status IN ('PENDING', 'RETRY') " +
            "AND (next_retry_at IS NULL OR next_retry_at <= CURRENT_TIMESTAMP)")
    int claimForProcessing(@Param("token") String token);

    @Update("UPDATE article_moderation_submissions SET status = 'PROCESSING', processing_started_at = CURRENT_TIMESTAMP, update_time = CURRENT_TIMESTAMP " +
            "WHERE submission_token = #{token} AND status IN ('PENDING', 'RETRY', 'MANUAL_REVIEW')")
    int claimForManualDecision(@Param("token") String token);

    @Update("UPDATE article_moderation_submissions SET status = 'RETRY', retry_count = #{retryCount}, " +
            "next_retry_at = #{nextRetryAt}, last_error = #{lastError}, update_time = CURRENT_TIMESTAMP " +
            "WHERE submission_token = #{token} AND status = 'PROCESSING'")
    int scheduleRetry(@Param("token") String token, @Param("retryCount") int retryCount,
                      @Param("nextRetryAt") LocalDateTime nextRetryAt, @Param("lastError") String lastError);

    @Update("UPDATE article_moderation_submissions SET status = 'MANUAL_REVIEW', " +
            "last_error = #{lastError}, reviewed_at = CURRENT_TIMESTAMP, update_time = CURRENT_TIMESTAMP " +
            "WHERE submission_token = #{token} AND status = 'PROCESSING'")
    int moveToManualReview(@Param("token") String token, @Param("lastError") String lastError);

    @Update("UPDATE article_moderation_submissions SET status = #{status}, active_article_id = NULL, reviewed_at = CURRENT_TIMESTAMP, " +
            "last_error = #{reason}, update_time = CURRENT_TIMESTAMP WHERE submission_token = #{token} AND status = 'PROCESSING'")
    int completeAi(@Param("token") String token, @Param("status") ArticleModerationSubmission.Status status,
                   @Param("reason") String reason);

    @Update("UPDATE article_moderation_submissions SET status = #{status}, active_article_id = NULL, reviewed_at = CURRENT_TIMESTAMP, " +
            "reviewed_by = #{adminId}, review_reason = #{reason}, manual_action_at = CURRENT_TIMESTAMP, update_time = CURRENT_TIMESTAMP " +
            "WHERE submission_token = #{token} AND status = 'PROCESSING'")
    int completeManually(@Param("token") String token, @Param("status") ArticleModerationSubmission.Status status,
                         @Param("adminId") Long adminId, @Param("reason") String reason);

    @Select("SELECT * FROM article_moderation_submissions WHERE status = 'PENDING' OR (status = 'RETRY' AND next_retry_at <= CURRENT_TIMESTAMP)")
    List<ArticleModerationSubmission> selectDueSubmissions();

    @Select("SELECT * FROM article_moderation_submissions WHERE status = 'PROCESSING' AND processing_started_at < #{before}")
    List<ArticleModerationSubmission> selectStaleProcessing(@Param("before") LocalDateTime before);
}
