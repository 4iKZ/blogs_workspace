package com.blog.service;

import com.blog.common.Result;
import com.blog.dto.WebsiteVisitDTO;
import com.blog.dto.PageVisitDTO;
import com.blog.dto.VisitorSourceDTO;
import com.blog.dto.DeviceStatisticsDTO;

import java.util.List;

/**
 * 网站访问统计服务接口
 */
public interface WebsiteVisitService {

    /**
     * 记录页面访问
     * @param pageUrl 页面URL
     * @param userId 用户ID（可选）
     * @param ipAddress IP地址
     * @param userAgent 用户代理
     * @return 操作结果
     */
    Result<Void> recordPageVisit(String pageUrl, Long userId, String ipAddress, String userAgent);

    /**
     * 获取网站访问统计
     * @param type 统计类型：day-日统计，week-周统计，month-月统计
     * @param startDate 开始日期（格式：yyyy-MM-dd）
     * @param endDate 结束日期（格式：yyyy-MM-dd）
     * @return 访问统计列表
     */
    Result<List<WebsiteVisitDTO>> getWebsiteVisitStatistics(String type, String startDate, String endDate);

    /**
     * 获取实时访问统计
     * @return 实时访问统计信息
     */
    Result<WebsiteVisitDTO> getRealTimeStatistics();

    /**
     * 获取热门页面统计
     * @param limit 数量限制
     * @return 热门页面统计列表
     */
    Result<List<PageVisitDTO>> getHotPageStatistics(Integer limit);

    /**
     * 获取访客来源统计
     * @param limit 数量限制
     * @return 访客来源统计列表
     */
    Result<List<VisitorSourceDTO>> getVisitorSourceStatistics(Integer limit);

    /**
     * 获取访问设备统计
     * @return 访问设备统计信息
     */
    Result<DeviceStatisticsDTO> getDeviceStatistics();
}