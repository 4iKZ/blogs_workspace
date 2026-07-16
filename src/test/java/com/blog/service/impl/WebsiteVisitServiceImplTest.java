package com.blog.service.impl;

import com.blog.dto.DeviceStatisticsDTO;
import com.blog.dto.PageVisitDTO;
import com.blog.dto.VisitorSourceDTO;
import com.blog.dto.WebsiteVisitDTO;
import com.blog.entity.VisitStatistics;
import com.blog.entity.WebsiteAccessLog;
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
import org.mockito.ArgumentCaptor;

class WebsiteVisitServiceImplTest {

    private final WebsiteVisitServiceImpl service = new WebsiteVisitServiceImpl();

    @Test
    void recordPageVisit_nullIp_shouldSetUnknown() {
        WebsiteAccessLogMapper mapper = mock(WebsiteAccessLogMapper.class);
        setField(service, "websiteAccessLogMapper", mapper);

        service.recordPageVisit("/home", 1L, null, "Mozilla");

        verify(mapper, times(1)).insert(any(WebsiteAccessLog.class));
        WebsiteAccessLog log = capturedAccessLog(mapper);
        assertThat(log.getIpAddress()).isEqualTo("unknown");
        assertThat(log.getPageUrl()).isEqualTo("/home");
        assertThat(log.getUserId()).isEqualTo(1L);
    }

    @Test
    void getWebsiteVisitStatistics_emptyList_shouldReturnEmpty() {
        VisitStatisticsMapper mapper = mock(VisitStatisticsMapper.class);
        when(mapper.selectByDateRange(anyString(), anyString())).thenReturn(List.of());
        setField(service, "visitStatisticsMapper", mapper);

        var result = service.getWebsiteVisitStatistics("day", "2025-01-01", "2025-01-31");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEmpty();
    }

    @Test
    void getRealTimeStatistics_nullValues_shouldDefaultToZero() {
        WebsiteAccessLogMapper mapper = mock(WebsiteAccessLogMapper.class);
        when(mapper.countTodayPv()).thenReturn(null);
        when(mapper.countTodayUv()).thenReturn(null);
        setField(service, "websiteAccessLogMapper", mapper);

        var result = service.getRealTimeStatistics();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getPageView()).isEqualTo(0L);
        assertThat(result.getData().getUniqueVisitor()).isEqualTo(0L);
        assertThat(result.getData().getVisitCount()).isEqualTo(0L);
        assertThat(result.getData().getDate()).isEqualTo(LocalDate.now().toString());
    }

    @Test
    void getHotPageStatistics_shouldParseVisitCounts() {
        WebsiteAccessLogMapper mapper = mock(WebsiteAccessLogMapper.class);
        Map<String, Object> row = Map.of(
                "page_url", "/home",
                "visit_count", 500,
                "unique_visitor", 200
        );
        when(mapper.selectTopPages(anyInt())).thenReturn(List.of(row));
        setField(service, "websiteAccessLogMapper", mapper);

        var result = service.getHotPageStatistics(10);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getPageUrl()).isEqualTo("/home");
        assertThat(result.getData().get(0).getVisitCount()).isEqualTo(500L);
        assertThat(result.getData().get(0).getUniqueVisitor()).isEqualTo(200L);
    }

    @Test
    void getVisitorSourceStatistics_shouldCalculatePercentage() {
        WebsiteAccessLogMapper mapper = mock(WebsiteAccessLogMapper.class);
        Map<String, Object> row = Map.of(
                "source_type", "search",
                "source_name", "Google",
                "visit_count", 300
        );
        when(mapper.selectTrafficSourcesByDateRange(anyString(), anyString(), anyInt()))
                .thenReturn(List.of(row));
        setField(service, "websiteAccessLogMapper", mapper);

        var result = service.getVisitorSourceStatistics(10);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getSourceType()).isEqualTo("search");
        assertThat(result.getData().get(0).getVisitCount()).isEqualTo(300L);
        assertThat(result.getData().get(0).getPercentage()).isEqualTo(100.0);
    }

    @Test
    void getVisitorSourceStatistics_multipleSources_shouldCalculateCorrectly() {
        WebsiteAccessLogMapper mapper = mock(WebsiteAccessLogMapper.class);
        Map<String, Object> row1 = Map.of("source_type", "search", "source_name", "Google", "visit_count", 300);
        Map<String, Object> row2 = Map.of("source_type", "direct", "source_name", "Direct", "visit_count", 200);
        Map<String, Object> row3 = Map.of("source_type", "social", "source_name", "Twitter", "visit_count", 100);
        when(mapper.selectTrafficSourcesByDateRange(anyString(), anyString(), anyInt()))
                .thenReturn(List.of(row1, row2, row3));
        setField(service, "websiteAccessLogMapper", mapper);

        var result = service.getVisitorSourceStatistics(10);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(3);
        assertThat(result.getData().get(0).getPercentage()).isEqualTo(50.0);
        assertThat(result.getData().get(1).getPercentage()).isEqualTo(33.33);
        assertThat(result.getData().get(2).getPercentage()).isEqualTo(16.67);
    }

    @Test
    void getDeviceStatistics_shouldAggregateDeviceTypes() {
        WebsiteAccessLogMapper mapper = mock(WebsiteAccessLogMapper.class);
        when(mapper.countByDeviceType()).thenReturn(List.of(
                Map.of("device_type", "desktop", "visit_count", 600),
                Map.of("device_type", "mobile", "visit_count", 300),
                Map.of("device_type", "tablet", "visit_count", 100)
        ));
        when(mapper.countByBrowser()).thenReturn(List.of());
        when(mapper.countByOperatingSystem()).thenReturn(List.of());
        setField(service, "websiteAccessLogMapper", mapper);

        var result = service.getDeviceStatistics();

        assertThat(result.isSuccess()).isTrue();
        DeviceStatisticsDTO dto = result.getData();
        assertThat(dto.getDeviceType().getDesktop()).isEqualTo(60.0);
        assertThat(dto.getDeviceType().getMobile()).isEqualTo(30.0);
        assertThat(dto.getDeviceType().getTablet()).isEqualTo(10.0);
    }

    @Test
    void getDeviceStatistics_emptyData_shouldReturnZeros() {
        WebsiteAccessLogMapper mapper = mock(WebsiteAccessLogMapper.class);
        when(mapper.countByDeviceType()).thenReturn(List.of());
        when(mapper.countByBrowser()).thenReturn(List.of());
        when(mapper.countByOperatingSystem()).thenReturn(List.of());
        setField(service, "websiteAccessLogMapper", mapper);

        var result = service.getDeviceStatistics();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getDeviceType().getDesktop()).isEqualTo(0.0);
        assertThat(result.getData().getBrowser().getChrome()).isEqualTo(0.0);
    }

    private static WebsiteAccessLog capturedAccessLog(WebsiteAccessLogMapper mapper) {
        ArgumentCaptor<WebsiteAccessLog> captor = ArgumentCaptor.forClass(WebsiteAccessLog.class);
        verify(mapper).insert(captor.capture());
        return captor.getValue();
    }

    private static void setField(WebsiteVisitServiceImpl target, String fieldName, Object value) {
        try {
            var field = WebsiteVisitServiceImpl.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
