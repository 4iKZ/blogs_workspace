package com.blog.event;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModerationEventTest {
    @Test
    void carriesOnlyOpaqueSubmissionToken() {
        ModerationEvent event = new ModerationEvent(this, "submission-token");
        assertThat(event.getSubmissionToken()).isEqualTo("submission-token");
        assertThat(event.toString()).doesNotContain("content").doesNotContain("articleId");
    }
}
