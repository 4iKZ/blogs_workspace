package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.ArticleModerationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章审核记录Mapper接口
 */
@Mapper
public interface ArticleModerationLogMapper extends BaseMapper<ArticleModerationLog> {
}
