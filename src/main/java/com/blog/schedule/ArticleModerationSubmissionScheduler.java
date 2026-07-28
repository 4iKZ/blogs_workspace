package com.blog.schedule;

import com.blog.service.ArticleModerationSubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Recovers tasks lost because of rejected async work, process restart, or event loss. */
@Component
@Slf4j
@RequiredArgsConstructor
public class ArticleModerationSubmissionScheduler {
    private final ArticleModerationSubmissionService moderationSubmissionService;

    @Scheduled(fixedDelayString = "${moderation.recovery-delay-ms:60000}")
    public void recoverDueSubmissions() {
        try {
            moderationSubmissionService.recoverStaleProcessing();
            moderationSubmissionService.listDueSubmissions()
                    .forEach(submission -> moderationSubmissionService.process(submission.getSubmissionToken()));
        } catch (RuntimeException e) {
            log.error("恢复文章审核任务失败", e);
        }
    }
}
