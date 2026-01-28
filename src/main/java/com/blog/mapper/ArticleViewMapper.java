package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.ArticleView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 文章浏览记录Mapper接口
 */
@Mapper
public interface ArticleViewMapper extends BaseMapper<ArticleView> {

    /**
     * 统计文章浏览次数
     * @param articleId 文章ID
     * @return 浏览次数
     */
    @Select("SELECT COUNT(1) FROM article_views WHERE article_id = #{articleId} AND deleted = 0")
    int countArticleViews(@Param("articleId") Long articleId);

    /**
     * 统计今日文章浏览次数
     * @param articleId 文章ID
     * @return 今日浏览次数
     */
    @Select("SELECT COUNT(1) FROM article_views WHERE article_id = #{articleId} AND DATE(create_time) = CURDATE() AND deleted = 0")
    int countTodayArticleViews(@Param("articleId") Long articleId);

    /**
     * 统计用户浏览次数（今日）
     * @param userId 用户ID
     * @param articleId 文章ID
     * @return 用户今日浏览次数
     */
    @Select("SELECT COUNT(1) FROM article_views WHERE user_id = #{userId} AND article_id = #{articleId} AND DATE(create_time) = CURDATE() AND deleted = 0")
    int countUserTodayViews(@Param("userId") Long userId, @Param("articleId") Long articleId);

    /**
     * 统计IP浏览次数（今日）
     * @param ipAddress IP地址
     * @param articleId 文章ID
     * @return IP今日浏览次数
     */
    @Select("SELECT COUNT(1) FROM article_views WHERE ip_address = #{ipAddress} AND article_id = #{articleId} AND DATE(create_time) = CURDATE() AND deleted = 0")
    int countIpTodayViews(@Param("ipAddress") String ipAddress, @Param("articleId") Long articleId);

    /**
     * 统计今日总浏览次数
     * @return 今日总浏览次数
     */
    @Select("SELECT COUNT(1) FROM article_views WHERE DATE(create_time) = CURDATE() AND deleted = 0")
    int countTodayTotalViews();

    /**
     * 统计今日独立访客数
     * @return 今日独立访客数
     */
    @Select("SELECT COUNT(DISTINCT ip_address) FROM article_views WHERE DATE(create_time) = CURDATE() AND deleted = 0")
    int countTodayUniqueVisitors();
}