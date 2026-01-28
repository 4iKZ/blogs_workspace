package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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
    @Select("SELECT * FROM system_config WHERE config_key = #{configKey} AND deleted = 0")
    SystemConfig selectByConfigKey(@Param("configKey") String configKey);

    /**
     * 查询启用的配置
     * @param configType 配置类型（可选）
     * @return 配置列表
     */
    @Select("<script>" +
            "SELECT * FROM system_config " +
            "WHERE status = 1 AND deleted = 0 " +
            "<if test='configType != null'>AND config_type = #{configType}</if>" +
            "ORDER BY config_key ASC" +
            "</script>")
    List<SystemConfig> selectActiveConfigs(@Param("configType") Integer configType);

    /**
     * 检查配置键是否存在（排除当前ID）
     * @param configKey 配置键
     * @param id 当前配置ID（更新时排除自身）
     * @return 存在返回1，不存在返回0
     */
    @Select("SELECT COUNT(1) FROM system_config WHERE config_key = #{configKey} AND deleted = 0 AND id != #{id}")
    int countByConfigKeyExcludeId(@Param("configKey") String configKey, @Param("id") Long id);

    /**
     * 批量更新配置状态
     * @param configKeys 配置键列表
     * @param status 新状态
     * @return 影响行数
     */
    @Select("<script>" +
            "UPDATE system_config SET status = #{status}, update_time = NOW() " +
            "WHERE config_key IN " +
            "<foreach collection='configKeys' item='configKey' open='(' separator=',' close=')'>#{configKey}</foreach>" +
            "AND deleted = 0" +
            "</script>")
    int batchUpdateConfigStatus(@Param("configKeys") List<String> configKeys, @Param("status") Integer status);
}