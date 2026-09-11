package com.blog.schedule;

import com.blog.service.FollowCountService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class FollowCountCorrectionScheduleTest {

    @Test
    void correctFollowCounts_shouldDelegateToService() {
        FollowCountService followCountService = mock(FollowCountService.class);

        FollowCountCorrectionSchedule schedule = new FollowCountCorrectionSchedule();
        // Use reflection to set the private field
        try {
            java.lang.reflect.Field field = FollowCountCorrectionSchedule.class.getDeclaredField("followCountService");
            field.setAccessible(true);
            field.set(schedule, followCountService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        schedule.correctFollowCounts();

        verify(followCountService, times(1)).correctAll();
    }
}
