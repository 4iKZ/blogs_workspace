package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 系统配置实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_config")
@Schema(description = "系统配置实体")
public class SystemConfig extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 配置ID
     */
    @Schema(description = "配置ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 配置键
     */
    @Schema(description = "配置键")
    @TableField("config_key")
    private String configKey;

    /**
     * 配置值
     */
    @Schema(description = "配置值")
    @TableField("config_value")
    private String configValue;

    /**
     * 配置描述
     */
    @Schema(description = "配置描述")
    @TableField("description")
    private String description;

    /**
     * 配置值类型：string/number/boolean/json
     */
    @Schema(description = "配置值类型：string/number/boolean/json")
    @TableField("config_type")
    private String configType;

    /**
     * 是否公开：0-否，1-是
     */
    @Schema(description = "是否公开：0-否，1-是")
    @TableField("is_public")
    private Integer isPublic;
}