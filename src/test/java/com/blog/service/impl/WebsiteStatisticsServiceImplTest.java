package com.blog.service.impl;

import com.blog.dto.PageDTO;
import com.blog.dto.VisitTrendDTO;
import com.blog.dto.WebsiteStatisticsDTO;
import com.blog.entity.VisitStatistics;
import com.blog.mapper.VisitStatisticsMapper;
import com.blog.mapper.WebsiteAccessLogMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WebsiteStatisticsServiceImplTest {

    private final WebsiteStatisticsServiceImpl service = new WebsiteStatisticsServiceImpl();

    @Test
    void getWebsiteStatistics_nullValues_shouldDefaultToZero() {
        VisitStatisticsMapper visitMapper = mock(VisitStatisticsMapper.class);
        WebsiteAccessLogMapper logMapper = mock(WebsiteAccessLogMapper.class);
        when(visitMapper.sumTotalPageViews()).thenReturn(0L);
        when(visitMapper.sumTotalUniqueVisitors()).thenReturn(0L);
        when(logMapper.countTodayPv()).thenReturn(null);
        when(logMapper.countTodayUv()).thenReturn(null);
        when(logMapper.countYesterdayPv()).thenReturn(null);
        when(logMapper.countYesterdayUv()).thenReturn(null);
        setField(service, "visitStatisticsMapper", visitMapper);
        setField(service, "websiteAccessLogMapper", logMapper);

        var result = service.getWebsiteStatistics();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getTotalPageViews()).isEqualTo(0L);
        assertThat(result.getData().getTotalUniqueVisitors()).isEqualTo(0L);
        assertThat(result.getData().getTodayPageViews()).isEqualTo(0L);
        assertThat(result.getData().getTodayUniqueVisitors()).isEqualTo(0L);
        assertThat(result.getData().getYesterdayPageViews()).isEqualTo(0L);
        assertThat(result.getData().getYesterdayUniqueVisitors()).isEqualTo(0L);
        assertThat(result.getData().getStatisticsDate()).isNotNull();
    }

    @Test
    void getTodayStatistics_zeroValues_shouldReturnZero() {
        WebsiteAccessLogMapper logMapper = mock(WebsiteAccessLogMapper.class);
        when(logMapper.countTodayPv()).thenReturn(0);
        when(logMapper.countTodayUv()).thenReturn(0);
        setField(service, "websiteAccessLogMapper", logMapper);

        var result = service.getTodayStatistics();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getTodayPageViews()).isEqualTo(0L);
        assertThat(result.getData().getTodayUniqueVisitors()).isEqualTo(0L);
    }

    @Test
    void getWeekStatistics_shouldReturnLast7Days() {
        VisitStatisticsMapper visitMapper = mock(VisitStatisticsMapper.class);
        when(visitMapper.sumLast7DaysPageViews()).thenReturn(1200L);
        when(visitMapper.sumLast7DaysUniqueVisitors()).thenReturn(300L);
        setField(service, "visitStatisticsMapper", visitMapper);

        var result = service.getWeekStatistics();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getTotalPageViews()).isEqualTo(1200L);
        assertThat(result.getData().getTotalUniqueVisitors()).isEqualTo(300L);
    }

    @Test
    void getMonthStatistics_shouldReturnLast30Days() {
        VisitStatisticsMapper visitMapper = mock(VisitStatisticsMapper.class);
        when(visitMapper.sumLast30DaysPageViews()).thenReturn(5000L);
        when(visitMapper.sumLast30DaysUniqueVisitors()).thenReturn(1200L);
        setField(service, "visitStatisticsMapper", visitMapper);

        var result = service.getMonthStatistics();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getTotalPageViews()).isEqualTo(5000L);
        assertThat(result.getData().getTotalUniqueVisitors()).isEqualTo(1200L);
    }

    @Test
    void getVisitTrend_shouldMapToDTOs() {
        VisitStatisticsMapper visitMapper = mock(VisitStatisticsMapper.class);
        VisitStatistics vs = new VisitStatistics();
        vs.setDate(LocalDate.of(2025, 1, 1));
        vs.setPageViews(100);
        vs.setUniqueVisitors(50);
        vs.setCreateTime(LocalDateTime.now());
        vs.setUpdateTime(LocalDateTime.now());
        when(visitMapper.selectByDateRange(anyString(), anyString())).thenReturn(List.of(vs));
        setField(service, "visitStatisticsMapper", visitMapper);

        var result = service.getVisitTrend(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getDate()).isEqualTo("2025-01-01");
        assertThat(result.getData().get(0).getPageViews()).isEqualTo(100L);
        assertThat(result.getData().get(0).getUniqueVisitors()).isEqualTo(50L);
        assertThat(result.getData().get(0).getNewVisitors()).isEqualTo(0L);
    }

    @Test
    void getTopPages_shouldReturnPagedResult() {
        WebsiteAccessLogMapper logMapper = mock(WebsiteAccessLogMapper.class);
        Map<String, Object> row = Map.of("page_url", "/home", "visit_count", 500);
        when(logMapper.selectTopPagesByDateRange(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of(row));
        when(logMapper.countDistinctPageUrls(anyString(), anyString())).thenReturn(1);
        setField(service, "websiteAccessLogMapper", logMapper);

        var result = service.getTopPages(1, 10);

        assertThat(result.isSuccess()).isTrue();
        PageDTO<Map<String, Object>> page = result.getData();
        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getRecords()).hasSize(1);
        assertThat(page.getRecords().get(0).get("page_url")).isEqualTo("/home");
    }

    @Test
    void getTrafficSources_shouldReturnList() {
        WebsiteAccessLogMapper logMapper = mock(WebsiteAccessLogMapper.class);
        Map<String, Object> source = Map.of("source", "google", "count", 200);
        when(logMapper.selectTrafficSourcesByDateRange(anyString(), anyString(), eq(10)))
                .thenReturn(List.of(source));
        setField(service, "websiteAccessLogMapper", logMapper);

        var result = service.getTrafficSources();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).get("source")).isEqualTo("google");
    }

    @Test
    void cleanExpiredStatistics_shouldCallDelete() {
        WebsiteAccessLogMapper logMapper = mock(WebsiteAccessLogMapper.class);
        when(logMapper.deleteBeforeDate(any())).thenReturn(42);
        setField(service, "websiteAccessLogMapper", logMapper);

        var result = service.cleanExpiredStatistics(30);

        assertThat(result.isSuccess()).isTrue();
        verify(logMapper, times(1)).deleteBeforeDate(any());
    }

    @Test
    void recordPageView_shouldReturnSuccess() {
        var result = service.recordPageView("/home", "Mozilla", "127.0.0.1");

        assertThat(result.isSuccess()).isTrue();
    }

    private static void setField(WebsiteStatisticsServiceImpl target, String fieldName, Object value) {
        try {
            var field = WebsiteStatisticsServiceImpl.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
