package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.WebsiteAccessLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Mapper
public interface WebsiteAccessLogMapper extends BaseMapper<WebsiteAccessLog> {

    /**
     * 批量插入访问日志
     */
    int insertBatch(@Param("list") Collection<WebsiteAccessLog> logs);

    // -------------------------------------------------------------------------
    // 原有查询方法（XML 中定义）
    // -------------------------------------------------------------------------

    List<WebsiteAccessLog> selectByDateRange(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    List<Map<String, Object>> selectTopPages(@Param("limit") Integer limit);

    Integer countDistinctPages(@Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);

    List<Map<String, Object>> selectTrafficSources(@Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    int deleteBeforeDate(@Param("cutoffDate") LocalDateTime cutoffDate);

    // -------------------------------------------------------------------------
    // 定时聚合任务使用（access_date 字符串，格式 yyyy-MM-dd）
    // -------------------------------------------------------------------------

    /**
     * 统计指定日期的总访问量（PV）
     */
    @Select("SELECT COUNT(*) FROM website_access_log WHERE access_date = #{date}")
    Integer countPvByDate(@Param("date") String date);

    /**
     * 统计指定日期的独立访客数（IP 去重 UV）
     */
    @Select("SELECT COUNT(DISTINCT ip_address) FROM website_access_log WHERE access_date = #{date}")
    Integer countUvByDate(@Param("date") String date);

    // -------------------------------------------------------------------------
    // 实时统计（今日）
    // -------------------------------------------------------------------------

    /**
     * 统计今日 PV
     */
    @Select("SELECT COUNT(*) FROM website_access_log WHERE access_date = DATE_FORMAT(NOW(), '%Y-%m-%d')")
    Integer countTodayPv();

    /**
     * 统计今日 UV（IP 去重）
     */
    @Select("SELECT COUNT(DISTINCT ip_address) FROM website_access_log WHERE access_date = DATE_FORMAT(NOW(), '%Y-%m-%d')")
    Integer countTodayUv();

    /**
     * 统计昨日 PV
     */
    @Select("SELECT COUNT(*) FROM website_access_log WHERE access_date = DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 1 DAY), '%Y-%m-%d')")
    Integer countYesterdayPv();

    /**
     * 统计昨日 UV
     */
    @Select("SELECT COUNT(DISTINCT ip_address) FROM website_access_log WHERE access_date = DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 1 DAY), '%Y-%m-%d')")
    Integer countYesterdayUv();

    // -------------------------------------------------------------------------
    // 设备/浏览器/操作系统统计
    // -------------------------------------------------------------------------

    /**
     * 按设备类型统计访问量（今日）
     * 返回 [{device_type: 'mobile', visit_count: 100}, ...]
     */
    @Select("SELECT device_type, COUNT(*) AS visit_count " +
            "FROM website_access_log " +
            "WHERE access_date = DATE_FORMAT(NOW(), '%Y-%m-%d') AND device_type IS NOT NULL " +
            "GROUP BY device_type")
    List<Map<String, Object>> countByDeviceType();

    /**
     * 按浏览器统计访问量（今日）
     */
    @Select("SELECT browser, COUNT(*) AS visit_count " +
            "FROM website_access_log " +
            "WHERE access_date = DATE_FORMAT(NOW(), '%Y-%m-%d') AND browser IS NOT NULL " +
            "GROUP BY browser")
    List<Map<String, Object>> countByBrowser();

    /**
     * 按操作系统统计访问量（今日）
     */
    @Select("SELECT operating_system, COUNT(*) AS visit_count " +
            "FROM website_access_log " +
            "WHERE access_date = DATE_FORMAT(NOW(), '%Y-%m-%d') AND operating_system IS NOT NULL " +
            "GROUP BY operating_system")
    List<Map<String, Object>> countByOperatingSystem();

    /**
     * 按来源（referer）统计访问量，支持时间范围
     */
    List<Map<String, Object>> selectTrafficSourcesByDateRange(@Param("startDate") String startDate,
                                                              @Param("endDate") String endDate,
                                                              @Param("limit") Integer limit);

    /**
     * 按页面统计访问量，支持时间范围和分页
     */
    List<Map<String, Object>> selectTopPagesByDateRange(@Param("startDate") String startDate,
                                                        @Param("endDate") String endDate,
                                                        @Param("offset") Integer offset,
                                                        @Param("size") Integer size);

    /**
     * 统计指定日期范围内不同页面 URL 的数量（用于分页 total）
     */
    @Select("SELECT COUNT(DISTINCT page_url) FROM website_access_log " +
            "WHERE page_url IS NOT NULL " +
            "AND access_date >= #{startDate} AND access_date <= #{endDate}")
    Integer countDistinctPageUrls(@Param("startDate") String startDate,
                                  @Param("endDate") String endDate);
}
