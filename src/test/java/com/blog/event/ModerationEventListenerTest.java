package com.blog.event;

import com.blog.service.ArticleModerationSubmissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ModerationEventListenerTest {
    @Mock private ArticleModerationSubmissionService moderationSubmissionService;
    @InjectMocks private ModerationEventListener listener;

    @Test
    void reloadsAndProcessesOnlyTheSubmissionToken() {
        listener.handleModerationEvent(new ModerationEvent(this, "token"));
        verify(moderationSubmissionService).process("token");
    }
}
