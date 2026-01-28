package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.WebsiteAccessLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface WebsiteAccessLogMapper extends BaseMapper<WebsiteAccessLog> {
    
    /**
     * 根据日期范围查询访问日志
     */
    List<WebsiteAccessLog> selectByDateRange(@Param("startDate") LocalDateTime startDate, 
                                            @Param("endDate") LocalDateTime endDate);
    
    /**
     * 查询热门页面
     */
    List<Map<String, Object>> selectTopPages(@Param("limit") Integer limit);
    
    /**
     * 统计不同页面的访问次数
     */
    Integer countDistinctPages(@Param("startDate") LocalDateTime startDate, 
                              @Param("endDate") LocalDateTime endDate);
    
    /**
     * 查询访问来源统计
     */
    List<Map<String, Object>> selectTrafficSources(@Param("startDate") LocalDateTime startDate, 
                                                  @Param("endDate") LocalDateTime endDate);
    
    /**
     * 删除指定日期之前的访问日志
     */
    int deleteBeforeDate(@Param("cutoffDate") LocalDateTime cutoffDate);
}