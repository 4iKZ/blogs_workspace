package com.blog.schedule;

import com.blog.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class FollowCountCorrectionScheduleTest {

    @Test
    void correctFollowCounts_success_shouldLogAndComplete() {
        UserMapper userMapper = mock(UserMapper.class);
        when(userMapper.correctFollowerCounts()).thenReturn(10);
        when(userMapper.correctFollowingCounts()).thenReturn(8);

        FollowCountCorrectionSchedule schedule = new FollowCountCorrectionSchedule();
        // Use reflection to set the private final field
        try {
            java.lang.reflect.Field field = FollowCountCorrectionSchedule.class.getDeclaredField("userMapper");
            field.setAccessible(true);
            field.set(schedule, userMapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        schedule.correctFollowCounts();

        verify(userMapper, times(1)).correctFollowerCounts();
        verify(userMapper, times(1)).correctFollowingCounts();
    }

    @Test
    void correctFollowCounts_exception_shouldNotPropagate() {
        UserMapper userMapper = mock(UserMapper.class);
        when(userMapper.correctFollowerCounts()).thenThrow(new RuntimeException("db error"));

        FollowCountCorrectionSchedule schedule = new FollowCountCorrectionSchedule();
        try {
            java.lang.reflect.Field field = FollowCountCorrectionSchedule.class.getDeclaredField("userMapper");
            field.setAccessible(true);
            field.set(schedule, userMapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        schedule.correctFollowCounts();

        verify(userMapper, times(1)).correctFollowerCounts();
    }
}
