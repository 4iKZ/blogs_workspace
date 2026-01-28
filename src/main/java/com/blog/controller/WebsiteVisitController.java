package com.blog.controller;

import com.blog.common.Result;
import com.blog.dto.*;
import com.blog.service.WebsiteVisitService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 网站访问统计控制器
 */
@RestController
@RequestMapping("/api/statistics/website")
@Tag(name = "网站访问统计管理")
public class WebsiteVisitController {

    @Autowired
    private WebsiteVisitService websiteVisitService;

    @PostMapping("/visit")
    @Operation(summary = "记录页面访问")
    public Result<Void> recordPageVisit(HttpServletRequest request) {
        String pageUrl = request.getRequestURI();
        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        Long userId = null; // 从请求中获取用户ID，如果有的话

        return websiteVisitService.recordPageVisit(pageUrl, userId, ipAddress, userAgent);
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取网站访问统计")
    public Result<List<WebsiteVisitDTO>> getWebsiteVisitStatistics(
            @Parameter(description = "统计类型：day-日统计，week-周统计，month-月统计") @RequestParam String type,
            @Parameter(description = "开始日期（格式：yyyy-MM-dd）") @RequestParam String startDate,
            @Parameter(description = "结束日期（格式：yyyy-MM-dd）") @RequestParam String endDate) {
        return websiteVisitService.getWebsiteVisitStatistics(type, startDate, endDate);
    }

    @GetMapping("/realtime")
    @Operation(summary = "获取实时访问统计")
    public Result<WebsiteVisitDTO> getRealTimeStatistics() {
        return websiteVisitService.getRealTimeStatistics();
    }

    @GetMapping("/hot-pages")
    @Operation(summary = "获取热门页面统计")
    public Result<List<PageVisitDTO>> getHotPageStatistics(
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "10") Integer limit) {
        return websiteVisitService.getHotPageStatistics(limit);
    }

    @GetMapping("/visitor-sources")
    @Operation(summary = "获取访客来源统计")
    public Result<List<VisitorSourceDTO>> getVisitorSourceStatistics(
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "10") Integer limit) {
        return websiteVisitService.getVisitorSourceStatistics(limit);
    }

    @GetMapping("/devices")
    @Operation(summary = "获取访问设备统计")
    public Result<DeviceStatisticsDTO> getDeviceStatistics() {
        return websiteVisitService.getDeviceStatistics();
    }
}
