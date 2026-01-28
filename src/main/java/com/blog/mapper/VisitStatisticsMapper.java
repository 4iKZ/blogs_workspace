package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.VisitStatistics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 访问统计Mapper接口
 */
@Mapper
public interface VisitStatisticsMapper extends BaseMapper<VisitStatistics> {

    /**
     * 根据日期查询统计记录
     * @param visitDate 日期（格式：yyyy-MM-dd）
     * @return 统计记录
     */
    @Select("SELECT * FROM visit_statistics WHERE visit_date = #{visitDate} AND deleted = 0")
    VisitStatistics selectByVisitDate(@Param("visitDate") String visitDate);

    /**
     * 查询最近几天的统计记录
     * @param days 天数
     * @return 统计记录列表
     */
    @Select("SELECT * FROM visit_statistics WHERE visit_date >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) AND deleted = 0 " +
            "ORDER BY visit_date DESC")
    List<VisitStatistics> selectRecentStatistics(@Param("days") Integer days);

    /**
     * 查询指定日期范围的统计记录
     * @param startDate 开始日期（格式：yyyy-MM-dd）
     * @param endDate 结束日期（格式：yyyy-MM-dd）
     * @return 统计记录列表
     */
    @Select("SELECT * FROM visit_statistics WHERE visit_date BETWEEN #{startDate} AND #{endDate} AND deleted = 0 " +
            "ORDER BY visit_date DESC")
    List<VisitStatistics> selectStatisticsByDateRange(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 统计总访问量
     * @return 总访问量
     */
    @Select("SELECT COALESCE(SUM(page_views), 0) FROM visit_statistics WHERE deleted = 0")
    long sumTotalPageViews();

    /**
     * 统计总独立访客数
     * @return 总独立访客数
     */
    @Select("SELECT COALESCE(SUM(unique_visitors), 0) FROM visit_statistics WHERE deleted = 0")
    long sumTotalUniqueVisitors();

    /**
     * 统计最近7天的访问量
     * @return 最近7天访问量
     */
    @Select("SELECT COALESCE(SUM(page_views), 0) FROM visit_statistics WHERE visit_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) AND deleted = 0")
    long sumLast7DaysPageViews();

    /**
     * 统计最近30天的访问量
     * @return 最近30天访问量
     */
    @Select("SELECT COALESCE(SUM(page_views), 0) FROM visit_statistics WHERE visit_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) AND deleted = 0")
    long sumLast30DaysPageViews();

    /**
     * 更新今日统计数据
     * @param visitDate 日期
     * @param pageViews 页面访问量
     * @param uniqueVisitors 独立访客数
     * @param newVisitors 新访客数
     * @param bounceRate 跳出率
     * @param avgDuration 平均访问时长
     * @return 影响行数
     */
    @Select("<script>" +
            "INSERT INTO visit_statistics (visit_date, page_views, unique_visitors, new_visitors, bounce_rate, avg_duration, create_time, update_time) " +
            "VALUES (#{visitDate}, #{pageViews}, #{uniqueVisitors}, #{newVisitors}, #{bounceRate}, #{avgDuration}, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE " +
            "page_views = #{pageViews}, unique_visitors = #{uniqueVisitors}, new_visitors = #{newVisitors}, " +
            "bounce_rate = #{bounceRate}, avg_duration = #{avgDuration}, update_time = NOW()" +
            "</script>")
    int upsertStatistics(@Param("visitDate") String visitDate,
                        @Param("pageViews") Long pageViews,
                        @Param("uniqueVisitors") Long uniqueVisitors,
                        @Param("newVisitors") Long newVisitors,
                        @Param("bounceRate") Double bounceRate,
                        @Param("avgDuration") Long avgDuration);
}