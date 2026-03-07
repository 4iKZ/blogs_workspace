package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.VisitStatistics;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 访问统计 Mapper（对应 visit_statistics 表，每日聚合数据）
 */
@Mapper
public interface VisitStatisticsMapper extends BaseMapper<VisitStatistics> {

    /**
     * 根据日期查询统计记录
     */
    @Select("SELECT * FROM visit_statistics WHERE `date` = #{date}")
    VisitStatistics selectByDate(@Param("date") String date);

    /**
     * 查询最近 N 天的统计记录（按日期倒序）
     */
    @Select("SELECT * FROM visit_statistics " +
            "WHERE `date` >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
            "ORDER BY `date` DESC")
    List<VisitStatistics> selectRecentDays(@Param("days") Integer days);

    /**
     * 查询指定日期范围的统计记录
     */
    @Select("SELECT * FROM visit_statistics " +
            "WHERE `date` BETWEEN #{startDate} AND #{endDate} " +
            "ORDER BY `date` ASC")
    List<VisitStatistics> selectByDateRange(@Param("startDate") String startDate,
                                            @Param("endDate") String endDate);

    /**
     * 汇总所有历史 PV（页面浏览量）
     */
    @Select("SELECT COALESCE(SUM(page_views), 0) FROM visit_statistics")
    long sumTotalPageViews();

    /**
     * 汇总所有历史 UV（独立访客数）
     */
    @Select("SELECT COALESCE(SUM(unique_visitors), 0) FROM visit_statistics")
    long sumTotalUniqueVisitors();

    /**
     * 汇总最近 7 天 PV
     */
    @Select("SELECT COALESCE(SUM(page_views), 0) FROM visit_statistics " +
            "WHERE `date` >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)")
    long sumLast7DaysPageViews();

    /**
     * 汇总最近 30 天 PV
     */
    @Select("SELECT COALESCE(SUM(page_views), 0) FROM visit_statistics " +
            "WHERE `date` >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)")
    long sumLast30DaysPageViews();

    /**
     * 汇总最近 7 天 UV
     */
    @Select("SELECT COALESCE(SUM(unique_visitors), 0) FROM visit_statistics " +
            "WHERE `date` >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)")
    long sumLast7DaysUniqueVisitors();

    /**
     * 汇总最近 30 天 UV
     */
    @Select("SELECT COALESCE(SUM(unique_visitors), 0) FROM visit_statistics " +
            "WHERE `date` >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)")
    long sumLast30DaysUniqueVisitors();

    /**
     * upsert 一条每日聚合记录（ON DUPLICATE KEY UPDATE）
     * visit_statistics 表在 date 列上有 uk_date 唯一索引
     */
    @Insert("INSERT INTO visit_statistics " +
            "(`date`, total_visits, unique_visitors, page_views, new_users, new_articles, new_comments, create_time, update_time) " +
            "VALUES (#{date}, #{totalVisits}, #{uniqueVisitors}, #{pageViews}, #{newUsers}, #{newArticles}, #{newComments}, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE " +
            "total_visits = #{totalVisits}, " +
            "unique_visitors = #{uniqueVisitors}, " +
            "page_views = #{pageViews}, " +
            "new_users = #{newUsers}, " +
            "new_articles = #{newArticles}, " +
            "new_comments = #{newComments}, " +
            "update_time = NOW()")
    int upsertStatistics(@Param("date") String date,
                         @Param("totalVisits") Integer totalVisits,
                         @Param("uniqueVisitors") Integer uniqueVisitors,
                         @Param("pageViews") Integer pageViews,
                         @Param("newUsers") Integer newUsers,
                         @Param("newArticles") Integer newArticles,
                         @Param("newComments") Integer newComments);
}
