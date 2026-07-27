package com.blog.schedule;

import com.blog.entity.ArticleModerationSubmission;
import com.blog.service.ArticleModerationSubmissionService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class ArticleModerationSubmissionSchedulerTest {
    @Test
    void restartRecoveryRepairsStaleWorkThenDispatchesDueTokens() {
        ArticleModerationSubmission submission = new ArticleModerationSubmission();
        submission.setSubmissionToken("recovered-token");
        ArticleModerationSubmissionService service = mock(ArticleModerationSubmissionService.class);
        when(service.listDueSubmissions()).thenReturn(List.of(submission));
        ArticleModerationSubmissionScheduler scheduler = new ArticleModerationSubmissionScheduler(service);

        scheduler.recoverDueSubmissions();

        var order = inOrder(service);
        order.verify(service).recoverStaleProcessing();
        order.verify(service).listDueSubmissions();
        order.verify(service).process("recovered-token");
    }
}
