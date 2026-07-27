package com.blog.event;

import com.blog.service.ArticleModerationSubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Dispatches durable moderation work. The service re-loads and claims the token. */
@Component
@RequiredArgsConstructor
public class ModerationEventListener {
    private final ArticleModerationSubmissionService moderationSubmissionService;

    @Async("moderationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleModerationEvent(ModerationEvent event) {
        moderationSubmissionService.process(event.getSubmissionToken());
    }
}
