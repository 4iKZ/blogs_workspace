package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统配置Mapper接口
 */
@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {

    /**
     * 根据配置键查询配置
     * @param configKey 配置键
     * @return 配置实体
     */
    @Select("SELECT * FROM system_config WHERE config_key = #{configKey}")
    SystemConfig selectByConfigKey(@Param("configKey") String configKey);

    /**
     * 检查配置键是否存在（排除当前ID）
     * @param configKey 配置键
     * @param id 当前配置ID（更新时排除自身）
     * @return 存在返回1，不存在返回0
     */
    @Select("SELECT COUNT(1) FROM system_config WHERE config_key = #{configKey} AND id != #{id}")
    int countByConfigKeyExcludeId(@Param("configKey") String configKey, @Param("id") Long id);
}
