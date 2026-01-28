package com.blog.controller;

import com.blog.common.Result;
import com.blog.dto.PageDTO;
import com.blog.dto.VisitTrendDTO;
import com.blog.dto.WebsiteStatisticsDTO;
import com.blog.service.WebsiteStatisticsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 网站访问统计控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/statistics/website")
@Tag(name = "网站访问统计管理")
public class WebsiteStatisticsController {

    @Autowired
    private WebsiteStatisticsService websiteStatisticsService;

    @PostMapping("/record")
    @Operation(summary = "记录页面访问")
    public Result<Void> recordPageView(
            @RequestParam @Parameter(description = "页面URL") String pageUrl,
            @RequestHeader(value = "User-Agent", required = false) @Parameter(description = "用户代理") String userAgent,
            @RequestHeader(value = "X-Forwarded-For", required = false) @Parameter(description = "IP地址") String ipAddress) {
        return websiteStatisticsService.recordPageView(pageUrl, userAgent, ipAddress);
    }

    @GetMapping("/overview")
    @Operation(summary = "获取网站总体统计信息")
    public Result<WebsiteStatisticsDTO> getWebsiteStatistics() {
        return websiteStatisticsService.getWebsiteStatistics();
    }

    @GetMapping("/trend")
    @Operation(summary = "获取访问趋势数据")
    public Result<List<VisitTrendDTO>> getVisitTrend(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") @Parameter(description = "开始日期") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") @Parameter(description = "结束日期") LocalDate endDate) {
        return websiteStatisticsService.getVisitTrend(startDate, endDate);
    }

    @GetMapping("/today")
    @Operation(summary = "获取今日访问统计")
    public Result<WebsiteStatisticsDTO> getTodayStatistics() {
        return websiteStatisticsService.getTodayStatistics();
    }

    @GetMapping("/week")
    @Operation(summary = "获取本周访问统计")
    public Result<WebsiteStatisticsDTO> getWeekStatistics() {
        return websiteStatisticsService.getWeekStatistics();
    }

    @GetMapping("/month")
    @Operation(summary = "获取本月访问统计")
    public Result<WebsiteStatisticsDTO> getMonthStatistics() {
        return websiteStatisticsService.getMonthStatistics();
    }

    @GetMapping("/top-pages")
    @Operation(summary = "获取热门页面排行")
    public Result<PageDTO<Map<String, Object>>> getTopPages(
            @RequestParam(defaultValue = "1") @Parameter(description = "页码") Integer page,
            @RequestParam(defaultValue = "10") @Parameter(description = "每页数量") Integer size) {
        return websiteStatisticsService.getTopPages(page, size);
    }

    @GetMapping("/traffic-sources")
    @Operation(summary = "获取访问来源统计")
    public Result<List<Map<String, Object>>> getTrafficSources() {
        return websiteStatisticsService.getTrafficSources();
    }

    @DeleteMapping("/clean")
    @Operation(summary = "清理过期统计数据")
    public Result<Void> cleanExpiredStatistics(
            @RequestParam(defaultValue = "90") @Parameter(description = "保留天数") Integer daysToKeep) {
        return websiteStatisticsService.cleanExpiredStatistics(daysToKeep);
    }
}
