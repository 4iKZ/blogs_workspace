package com.blog.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/** Event deliberately contains only an opaque durable submission token. */
@Getter
public class ModerationEvent extends ApplicationEvent {
    private final String submissionToken;

    public ModerationEvent(Object source, String submissionToken) {
        super(source);
        this.submissionToken = submissionToken;
    }

    @Override
    public String toString() {
        return "ModerationEvent{submissionToken='" + submissionToken + "'}";
    }
}
