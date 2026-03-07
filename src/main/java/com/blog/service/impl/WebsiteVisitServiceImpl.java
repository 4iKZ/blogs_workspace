package com.blog.service.impl;

import com.blog.common.Result;
import com.blog.dto.DeviceStatisticsDTO;
import com.blog.dto.PageVisitDTO;
import com.blog.dto.VisitorSourceDTO;
import com.blog.dto.WebsiteVisitDTO;
import com.blog.entity.VisitStatistics;
import com.blog.entity.WebsiteAccessLog;
import com.blog.mapper.VisitStatisticsMapper;
import com.blog.mapper.WebsiteAccessLogMapper;
import com.blog.service.WebsiteVisitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 网站访问统计服务实现类
 * 写入由 AccessLogInterceptor 异步负责，此 Service 专注查询功能
 */
@Slf4j
@Service
public class WebsiteVisitServiceImpl implements WebsiteVisitService {

    @Autowired
    private WebsiteAccessLogMapper websiteAccessLogMapper;

    @Autowired
    private VisitStatisticsMapper visitStatisticsMapper;

    @Override
    public Result<Void> recordPageVisit(String pageUrl, Long userId, String ipAddress, String userAgent) {
        // 手动触发记录（供前端 SPA 路由切换时调用）
        WebsiteAccessLog accessLog = new WebsiteAccessLog();
        accessLog.setAccessDate(LocalDate.now().toString());
        accessLog.setAccessTime(LocalDateTime.now());
        accessLog.setPageUrl(pageUrl);
        accessLog.setIpAddress(ipAddress != null ? ipAddress : "unknown");
        accessLog.setUserAgent(userAgent);
        accessLog.setUserId(userId);
        accessLog.setRequestMethod("GET");
        websiteAccessLogMapper.insert(accessLog);
        log.debug("手动记录页面访问，URL：{}", pageUrl);
        return Result.success();
    }

    @Override
    public Result<List<WebsiteVisitDTO>> getWebsiteVisitStatistics(String type, String startDate, String endDate) {
        log.info("获取访问统计，类型={}，{} ~ {}", type, startDate, endDate);
        List<VisitStatistics> list = visitStatisticsMapper.selectByDateRange(startDate, endDate);

        List<WebsiteVisitDTO> result = list.stream().map(vs -> {
            WebsiteVisitDTO dto = new WebsiteVisitDTO();
            dto.setDate(vs.getDate() != null ? vs.getDate().toString() : null);
            dto.setPageView(vs.getPageViews() != null ? vs.getPageViews().longValue() : 0L);
            dto.setUniqueVisitor(vs.getUniqueVisitors() != null ? vs.getUniqueVisitors().longValue() : 0L);
            dto.setVisitCount(vs.getTotalVisits() != null ? vs.getTotalVisits().longValue() : 0L);
            return dto;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    @Override
    public Result<WebsiteVisitDTO> getRealTimeStatistics() {
        log.info("获取实时访问统计（今日）");
        WebsiteVisitDTO dto = new WebsiteVisitDTO();
        dto.setDate(LocalDate.now().toString());

        Integer pv = websiteAccessLogMapper.countTodayPv();
        Integer uv = websiteAccessLogMapper.countTodayUv();
        dto.setPageView(pv != null ? pv.longValue() : 0L);
        dto.setUniqueVisitor(uv != null ? uv.longValue() : 0L);
        dto.setVisitCount(pv != null ? pv.longValue() : 0L);
        return Result.success(dto);
    }

    @Override
    public Result<List<PageVisitDTO>> getHotPageStatistics(Integer limit) {
        log.info("获取热门页面统计，limit={}", limit);
        List<Map<String, Object>> raw = websiteAccessLogMapper.selectTopPages(limit);

        List<PageVisitDTO> result = raw.stream().map(row -> {
            PageVisitDTO dto = new PageVisitDTO();
            dto.setPageUrl(String.valueOf(row.getOrDefault("page_url", "")));
            Object vc = row.get("visit_count");
            dto.setVisitCount(vc != null ? Long.parseLong(vc.toString()) : 0L);
            Object uv = row.get("unique_visitor");
            dto.setUniqueVisitor(uv != null ? Long.parseLong(uv.toString()) : 0L);
            return dto;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    @Override
    public Result<List<VisitorSourceDTO>> getVisitorSourceStatistics(Integer limit) {
        log.info("获取访客来源统计，limit={}", limit);
        String startDate = LocalDate.now().minusDays(30).toString();
        String endDate = LocalDate.now().toString();
        List<Map<String, Object>> raw = websiteAccessLogMapper.selectTrafficSourcesByDateRange(
                startDate, endDate, limit);

        // 计算总访问量用于占比
        long totalVisits = raw.stream()
                .mapToLong(r -> {
                    Object vc = r.get("visit_count");
                    return vc != null ? Long.parseLong(vc.toString()) : 0L;
                }).sum();

        List<VisitorSourceDTO> result = raw.stream().map(row -> {
            VisitorSourceDTO dto = new VisitorSourceDTO();
            dto.setSourceType(String.valueOf(row.getOrDefault("source_type", "other")));
            dto.setSourceName(String.valueOf(row.getOrDefault("source_name", "")));
            long vc = row.get("visit_count") != null ? Long.parseLong(row.get("visit_count").toString()) : 0L;
            dto.setVisitCount(vc);
            Object uv = row.get("unique_visitor");
            dto.setUniqueVisitor(uv != null ? Long.parseLong(uv.toString()) : 0L);
            dto.setPercentage(totalVisits > 0 ? Math.round(vc * 10000.0 / totalVisits) / 100.0 : 0.0);
            return dto;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    @Override
    public Result<DeviceStatisticsDTO> getDeviceStatistics() {
        log.info("获取访问设备统计（今日）");
        DeviceStatisticsDTO dto = new DeviceStatisticsDTO();

        // 设备类型统计
        List<Map<String, Object>> deviceRaw = websiteAccessLogMapper.countByDeviceType();
        dto.setDeviceType(buildDeviceTypeStat(deviceRaw));

        // 浏览器统计
        List<Map<String, Object>> browserRaw = websiteAccessLogMapper.countByBrowser();
        dto.setBrowser(buildBrowserStat(browserRaw));

        // 操作系统统计
        List<Map<String, Object>> osRaw = websiteAccessLogMapper.countByOperatingSystem();
        dto.setOperatingSystem(buildOsStat(osRaw));

        return Result.success(dto);
    }

    // -------------------------------------------------------------------------
    // 私有工具方法
    // -------------------------------------------------------------------------

    private DeviceStatisticsDTO.DeviceTypeStat buildDeviceTypeStat(List<Map<String, Object>> raw) {
        Map<String, Long> countMap = toCountMap(raw, "device_type");
        long total = countMap.values().stream().mapToLong(Long::longValue).sum();
        DeviceStatisticsDTO.DeviceTypeStat stat = new DeviceStatisticsDTO.DeviceTypeStat();
        stat.setDesktop(percent(countMap.getOrDefault("desktop", 0L), total));
        stat.setMobile(percent(countMap.getOrDefault("mobile", 0L), total));
        stat.setTablet(percent(countMap.getOrDefault("tablet", 0L), total));
        return stat;
    }

    private DeviceStatisticsDTO.BrowserStat buildBrowserStat(List<Map<String, Object>> raw) {
        Map<String, Long> countMap = toCountMap(raw, "browser");
        long total = countMap.values().stream().mapToLong(Long::longValue).sum();
        DeviceStatisticsDTO.BrowserStat stat = new DeviceStatisticsDTO.BrowserStat();
        stat.setChrome(percent(sumContains(countMap, "Chrome"), total));
        stat.setFirefox(percent(sumContains(countMap, "Firefox"), total));
        stat.setSafari(percent(sumContains(countMap, "Safari"), total));
        stat.setEdge(percent(sumContains(countMap, "Edge"), total));
        long others = total - sumContains(countMap, "Chrome")
                - sumContains(countMap, "Firefox")
                - sumContains(countMap, "Safari")
                - sumContains(countMap, "Edge");
        stat.setOther(percent(Math.max(others, 0), total));
        return stat;
    }

    private DeviceStatisticsDTO.OperatingSystemStat buildOsStat(List<Map<String, Object>> raw) {
        Map<String, Long> countMap = toCountMap(raw, "operating_system");
        long total = countMap.values().stream().mapToLong(Long::longValue).sum();
        DeviceStatisticsDTO.OperatingSystemStat stat = new DeviceStatisticsDTO.OperatingSystemStat();
        stat.setWindows(percent(sumContains(countMap, "Windows"), total));
        stat.setMacos(percent(sumContains(countMap, "Mac"), total));
        stat.setLinux(percent(sumContains(countMap, "Linux"), total));
        stat.setAndroid(percent(sumContains(countMap, "Android"), total));
        stat.setIos(percent(sumContains(countMap, "iOS"), total));
        long others = total - sumContains(countMap, "Windows")
                - sumContains(countMap, "Mac")
                - sumContains(countMap, "Linux")
                - sumContains(countMap, "Android")
                - sumContains(countMap, "iOS");
        stat.setOther(percent(Math.max(others, 0), total));
        return stat;
    }

    private Map<String, Long> toCountMap(List<Map<String, Object>> raw, String keyField) {
        return raw.stream().collect(Collectors.toMap(
                r -> String.valueOf(r.getOrDefault(keyField, "other")),
                r -> {
                    Object vc = r.get("visit_count");
                    return vc != null ? Long.parseLong(vc.toString()) : 0L;
                },
                Long::sum
        ));
    }

    private long sumContains(Map<String, Long> map, String keyword) {
        return map.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getKey().contains(keyword))
                .mapToLong(Map.Entry::getValue).sum();
    }

    private double percent(long part, long total) {
        if (total == 0) return 0.0;
        return Math.round(part * 10000.0 / total) / 100.0;
    }
}
