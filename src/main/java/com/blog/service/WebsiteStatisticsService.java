package com.blog.service;

import com.blog.common.Result;
import com.blog.dto.PageDTO;
import com.blog.dto.VisitTrendDTO;
import com.blog.dto.WebsiteStatisticsDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 网站访问统计服务接口
 */
public interface WebsiteStatisticsService {

    /**
     * 记录页面访问
     */
    Result<Void> recordPageView(String pageUrl, String userAgent, String ipAddress);

    /**
     * 获取网站总体统计信息
     */
    Result<WebsiteStatisticsDTO> getWebsiteStatistics();

    /**
     * 获取访问趋势数据
     */
    Result<List<VisitTrendDTO>> getVisitTrend(LocalDate startDate, LocalDate endDate);

    /**
     * 获取今日访问统计
     */
    Result<WebsiteStatisticsDTO> getTodayStatistics();

    /**
     * 获取本周访问统计
     */
    Result<WebsiteStatisticsDTO> getWeekStatistics();

    /**
     * 获取本月访问统计
     */
    Result<WebsiteStatisticsDTO> getMonthStatistics();

    /**
     * 获取热门页面排行
     */
    Result<PageDTO<Map<String, Object>>> getTopPages(Integer page, Integer size);

    /**
     * 获取访问来源统计
     */
    Result<List<Map<String, Object>>> getTrafficSources();

    /**
     * 清理过期统计数据
     */
    Result<Void> cleanExpiredStatistics(Integer daysToKeep);
}