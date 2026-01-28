package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.WebsiteStatistics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WebsiteStatisticsMapper extends BaseMapper<WebsiteStatistics> {
    
    /**
     * 获取当前网站统计信息
     * @return 网站统计信息
     */
    @Select("SELECT * FROM visit_statistics ORDER BY update_time DESC LIMIT 1")
    WebsiteStatistics selectCurrentStatistics();
}