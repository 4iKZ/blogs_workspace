package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 敏感词实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sensitive_words")
@Schema(description = "敏感词实体")
public class SensitiveWord extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 敏感词ID
     */
    @Schema(description = "敏感词ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 敏感词内容
     */
    @Schema(description = "敏感词内容")
    @TableField("word")
    private String word;

    /**
     * 分类
     */
    @Schema(description = "分类")
    @TableField("category")
    private String category;

    /**
     * 级别：1-警告，2-禁止
     */
    @Schema(description = "级别：1-警告，2-禁止")
    @TableField("level")
    private Integer level;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public SensitiveWord() {
    }

    public SensitiveWord(String word, String category, Integer level) {
        this.word = word;
        this.category = category;
        this.level = level;
    }
}
