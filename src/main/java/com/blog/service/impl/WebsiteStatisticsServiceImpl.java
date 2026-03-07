package com.blog.service.impl;

import com.blog.common.Result;
import com.blog.dto.PageDTO;
import com.blog.dto.VisitTrendDTO;
import com.blog.dto.WebsiteStatisticsDTO;
import com.blog.entity.VisitStatistics;
import com.blog.mapper.VisitStatisticsMapper;
import com.blog.mapper.WebsiteAccessLogMapper;
import com.blog.service.WebsiteStatisticsService;
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
 * 读取聚合数据来自 visit_statistics，实时数据来自 website_access_log
 */
@Service
@Slf4j
public class WebsiteStatisticsServiceImpl implements WebsiteStatisticsService {

    @Autowired
    private VisitStatisticsMapper visitStatisticsMapper;

    @Autowired
    private WebsiteAccessLogMapper websiteAccessLogMapper;

    @Override
    public Result<Void> recordPageView(String pageUrl, String userAgent, String ipAddress) {
        // 实际记录由 AccessLogInterceptor 异步完成，此方法保留供手动触发场景调用
        log.debug("页面访问已由拦截器记录，URL：{}", pageUrl);
        return Result.success(null);
    }

    @Override
    public Result<WebsiteStatisticsDTO> getWebsiteStatistics() {
        log.info("获取网站整体统计信息");
        WebsiteStatisticsDTO dto = new WebsiteStatisticsDTO();

        // 历史累计 PV / UV（来自 visit_statistics 聚合表）
        dto.setTotalPageViews(visitStatisticsMapper.sumTotalPageViews());
        dto.setTotalUniqueVisitors(visitStatisticsMapper.sumTotalUniqueVisitors());

        // 今日实时 PV / UV（来自 website_access_log 原始表）
        Integer todayPv = websiteAccessLogMapper.countTodayPv();
        Integer todayUv = websiteAccessLogMapper.countTodayUv();
        dto.setTodayPageViews(todayPv != null ? todayPv.longValue() : 0L);
        dto.setTodayUniqueVisitors(todayUv != null ? todayUv.longValue() : 0L);

        // 昨日 PV / UV（来自 website_access_log 原始表）
        Integer yPv = websiteAccessLogMapper.countYesterdayPv();
        Integer yUv = websiteAccessLogMapper.countYesterdayUv();
        dto.setYesterdayPageViews(yPv != null ? yPv.longValue() : 0L);
        dto.setYesterdayUniqueVisitors(yUv != null ? yUv.longValue() : 0L);

        dto.setStatisticsDate(LocalDateTime.now());
        return Result.success(dto);
    }

    @Override
    public Result<List<VisitTrendDTO>> getVisitTrend(LocalDate startDate, LocalDate endDate) {
        log.info("获取访问趋势，{} ~ {}", startDate, endDate);
        List<VisitStatistics> list = visitStatisticsMapper.selectByDateRange(
                startDate.toString(), endDate.toString());

        List<VisitTrendDTO> result = list.stream().map(vs -> {
            VisitTrendDTO dto = new VisitTrendDTO();
            dto.setDate(vs.getDate());
            dto.setPageViews(vs.getPageViews() != null ? vs.getPageViews().longValue() : 0L);
            dto.setUniqueVisitors(vs.getUniqueVisitors() != null ? vs.getUniqueVisitors().longValue() : 0L);
            dto.setNewVisitors(0L);
            dto.setReturningVisitors(0L);
            dto.setCreateTime(vs.getCreateTime());
            dto.setUpdateTime(vs.getUpdateTime());
            return dto;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    @Override
    public Result<WebsiteStatisticsDTO> getTodayStatistics() {
        log.info("获取今日实时访问统计");
        WebsiteStatisticsDTO dto = new WebsiteStatisticsDTO();

        Integer pv = websiteAccessLogMapper.countTodayPv();
        Integer uv = websiteAccessLogMapper.countTodayUv();
        dto.setTodayPageViews(pv != null ? pv.longValue() : 0L);
        dto.setTodayUniqueVisitors(uv != null ? uv.longValue() : 0L);
        dto.setStatisticsDate(LocalDateTime.now());
        return Result.success(dto);
    }

    @Override
    public Result<WebsiteStatisticsDTO> getWeekStatistics() {
        log.info("获取本周访问统计");
        WebsiteStatisticsDTO dto = new WebsiteStatisticsDTO();
        dto.setTotalPageViews(visitStatisticsMapper.sumLast7DaysPageViews());
        dto.setTotalUniqueVisitors(visitStatisticsMapper.sumLast7DaysUniqueVisitors());
        dto.setStatisticsDate(LocalDateTime.now());
        return Result.success(dto);
    }

    @Override
    public Result<WebsiteStatisticsDTO> getMonthStatistics() {
        log.info("获取本月访问统计");
        WebsiteStatisticsDTO dto = new WebsiteStatisticsDTO();
        dto.setTotalPageViews(visitStatisticsMapper.sumLast30DaysPageViews());
        dto.setTotalUniqueVisitors(visitStatisticsMapper.sumLast30DaysUniqueVisitors());
        dto.setStatisticsDate(LocalDateTime.now());
        return Result.success(dto);
    }

    @Override
    public Result<PageDTO<Map<String, Object>>> getTopPages(Integer page, Integer size) {
        log.info("获取热门页面排行，page={}，size={}", page, size);
        int offset = (page - 1) * size;
        String startDate = LocalDate.now().minusDays(30).toString();
        String endDate = LocalDate.now().toString();

        List<Map<String, Object>> records = websiteAccessLogMapper.selectTopPagesByDateRange(
                startDate, endDate, offset, size);
        Integer total = websiteAccessLogMapper.countDistinctPageUrls(startDate, endDate);

        long totalCount = total != null ? total.longValue() : 0L;
        PageDTO<Map<String, Object>> pageResult = new PageDTO<>(page, size, totalCount, records);
        return Result.success(pageResult);
    }

    @Override
    public Result<List<Map<String, Object>>> getTrafficSources() {
        log.info("获取访问来源统计");
        String startDate = LocalDate.now().minusDays(30).toString();
        String endDate = LocalDate.now().toString();
        List<Map<String, Object>> sources = websiteAccessLogMapper.selectTrafficSourcesByDateRange(
                startDate, endDate, 10);
        return Result.success(sources);
    }

    @Override
    public Result<Void> cleanExpiredStatistics(Integer daysToKeep) {
        log.info("清理过期访问日志，保留最近 {} 天", daysToKeep);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysToKeep);
        int deleted = websiteAccessLogMapper.deleteBeforeDate(cutoff);
        log.info("已删除 {} 条过期访问日志", deleted);
        return Result.success(null);
    }
}
