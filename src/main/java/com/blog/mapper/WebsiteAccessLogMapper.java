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
     * PV 过滤谓词（Task 3）：仅统计页面浏览型 GET 请求，
     * 排除各类非页面浏览的管理/接口类路径。
     * request_url 存的是 request.getRequestURI()，即不带头部的路径，如 "/api/article/list"。
     * 该谓词对"今日/昨日/指定日期"三种 PV 查询复用，保证口径一致。
     */
    String PAGE_VIEW_FILTER = "request_method = 'GET' "
            + "AND request_url NOT LIKE '/api/statistics/%' "
            + "AND request_url NOT LIKE '/api/user/%' "
            + "AND request_url NOT LIKE '/api/admin/%' "
            + "AND request_url NOT LIKE '/api/comment/%' "
            + "AND request_url NOT LIKE '/api/file/%' "
            + "AND request_url NOT LIKE '/api/category/%' "
            + "AND request_url NOT LIKE '/api/tag/%'";

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
     * 统计指定日期的页面浏览型 PV（GET 页面/列表读取类请求）
     */
    @Select("SELECT COUNT(*) FROM website_access_log WHERE access_date = #{date} AND " +
            PAGE_VIEW_FILTER)
    Integer countPvByDate(@Param("date") String date);

    /**
     * 统计指定日期的独立访客数（UV，user_id 与 ip_address 混合去重）
     */
    @Select("SELECT COUNT(DISTINCT CASE WHEN user_id IS NOT NULL THEN user_id ELSE ip_address END) " +
            "FROM website_access_log WHERE access_date = #{date}")
    Integer countUvByDate(@Param("date") String date);

    // -------------------------------------------------------------------------
    // 实时统计（今日）
    // -------------------------------------------------------------------------

    /**
     * 统计今日页面浏览型 PV
     */
    @Select("SELECT COUNT(*) FROM website_access_log " +
            "WHERE access_date = CAST(NOW() AS DATE) AND " + PAGE_VIEW_FILTER)
    Integer countTodayPv();

    /**
     * 统计今日 UV（user_id 与 ip_address 混合去重）
     */
    @Select("SELECT COUNT(DISTINCT CASE WHEN user_id IS NOT NULL THEN user_id ELSE ip_address END) " +
            "FROM website_access_log WHERE access_date = CAST(NOW() AS DATE)")
    Integer countTodayUv();

    /**
     * 统计昨日页面浏览型 PV
     */
    @Select("SELECT COUNT(*) FROM website_access_log " +
            "WHERE access_date = DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '%Y-%m-%d') AND " +
            PAGE_VIEW_FILTER)
    Integer countYesterdayPv();

    /**
     * 统计昨日 UV（user_id 与 ip_address 混合去重）
     */
    @Select("SELECT COUNT(DISTINCT CASE WHEN user_id IS NOT NULL THEN user_id ELSE ip_address END) " +
            "FROM website_access_log WHERE access_date = DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '%Y-%m-%d')")
    Integer countYesterdayUv();

    /**
     * 统计指定日期区间内的独立访客数（user_id 与 ip_address 混合去重），
     * 区间内天然去重，避免跨天重复计数。供总/周/月 UV 后续切换使用。
     * startDate/endDate 为 yyyy-MM-dd 格式（含边界）。
     */
    @Select("SELECT COUNT(DISTINCT CASE WHEN user_id IS NOT NULL THEN user_id ELSE ip_address END) " +
            "FROM website_access_log " +
            "WHERE access_date >= #{startDate} AND access_date <= #{endDate}")
    Integer countUniqueVisitorsByDateRange(@Param("startDate") String startDate,
                                           @Param("endDate") String endDate);

    // -------------------------------------------------------------------------
    // 设备/浏览器/操作系统统计
    // -------------------------------------------------------------------------

    /**
     * 按设备类型统计访问量（今日）
     * 返回 [{device_type: 'mobile', visit_count: 100}, ...]
     */
    @Select("SELECT device_type, COUNT(*) AS visit_count " +
            "FROM website_access_log " +
            "WHERE access_date = CAST(NOW() AS DATE) AND device_type IS NOT NULL " +
            "GROUP BY device_type")
    List<Map<String, Object>> countByDeviceType();

    /**
     * 按浏览器统计访问量（今日）
     */
    @Select("SELECT browser, COUNT(*) AS visit_count " +
            "FROM website_access_log " +
            "WHERE access_date = CAST(NOW() AS DATE) AND browser IS NOT NULL " +
            "GROUP BY browser")
    List<Map<String, Object>> countByBrowser();

    /**
     * 按操作系统统计访问量（今日）
     */
    @Select("SELECT operating_system, COUNT(*) AS visit_count " +
            "FROM website_access_log " +
            "WHERE access_date = CAST(NOW() AS DATE) AND operating_system IS NOT NULL " +
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
