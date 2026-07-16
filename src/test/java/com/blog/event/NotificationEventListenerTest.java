package com.blog.event;

import com.blog.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationEventListenerTest {

    @Test
    void handleNotificationEvent_success_shouldCreateNotification() {
        NotificationService notificationService = mock(NotificationService.class);
        NotificationEventListener listener = new NotificationEventListener();

        java.lang.reflect.Field field;
        try {
            field = NotificationEventListener.class.getDeclaredField("notificationService");
            field.setAccessible(true);
            field.set(listener, notificationService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        NotificationEvent event = new NotificationEvent(
                this, 100L, 1L, 1, 10L, 1, "liked your article"
        );

        listener.handleNotificationEvent(event);

        verify(notificationService, times(1)).createNotification(
                eq(100L), eq(1L), eq(1), eq(10L), eq(1), eq("liked your article")
        );
    }

    @Test
    void handleNotificationEvent_exception_shouldNotPropagate() {
        NotificationService notificationService = mock(NotificationService.class);
        doThrow(new RuntimeException("service error")).when(notificationService)
                .createNotification(anyLong(), anyLong(), anyInt(), anyLong(), anyInt(), anyString());

        NotificationEventListener listener = new NotificationEventListener();
        try {
            java.lang.reflect.Field field = NotificationEventListener.class.getDeclaredField("notificationService");
            field.setAccessible(true);
            field.set(listener, notificationService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        NotificationEvent event = new NotificationEvent(
                this, 100L, 1L, 1, 10L, 1, "liked your article"
        );

        listener.handleNotificationEvent(event);

        verify(notificationService, times(1)).createNotification(
                anyLong(), anyLong(), anyInt(), anyLong(), anyInt(), anyString()
        );
    }
}
