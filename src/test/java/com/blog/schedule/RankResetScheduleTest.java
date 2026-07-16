package com.blog.schedule;

import com.blog.service.ArticleRankService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RankResetScheduleTest {

    @Test
    void resetDayRank_success_shouldCallService() {
        ArticleRankService rankService = mock(ArticleRankService.class);

        RankResetSchedule schedule = new RankResetSchedule();
        try {
            java.lang.reflect.Field field = RankResetSchedule.class.getDeclaredField("articleRankService");
            field.setAccessible(true);
            field.set(schedule, rankService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        schedule.resetDayRank();

        verify(rankService, times(1)).resetRank("day");
    }

    @Test
    void resetWeekRank_success_shouldCallService() {
        ArticleRankService rankService = mock(ArticleRankService.class);

        RankResetSchedule schedule = new RankResetSchedule();
        try {
            java.lang.reflect.Field field = RankResetSchedule.class.getDeclaredField("articleRankService");
            field.setAccessible(true);
            field.set(schedule, rankService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        schedule.resetWeekRank();

        verify(rankService, times(1)).resetRank("week");
    }

    @Test
    void resetDayRank_exception_shouldNotPropagate() {
        ArticleRankService rankService = mock(ArticleRankService.class);
        doThrow(new RuntimeException("redis down")).when(rankService).resetRank("day");

        RankResetSchedule schedule = new RankResetSchedule();
        try {
            java.lang.reflect.Field field = RankResetSchedule.class.getDeclaredField("articleRankService");
            field.setAccessible(true);
            field.set(schedule, rankService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        schedule.resetDayRank();

        verify(rankService, times(1)).resetRank("day");
    }
}
