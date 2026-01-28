package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 文件上传记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("upload_files")
@Schema(description = "文件上传记录实体")
public class UploadFile extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文件ID
     */
    @Schema(description = "文件ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 原始文件名
     */
    @Schema(description = "原始文件名")
    @TableField("original_name")
    private String originalName;

    /**
     * 存储文件名
     */
    @Schema(description = "存储文件名")
    @TableField("file_name")
    private String fileName;

    /**
     * 文件路径
     */
    @Schema(description = "文件路径")
    @TableField("file_path")
    private String filePath;

    /**
     * 文件大小（字节）
     */
    @Schema(description = "文件大小（字节）")
    @TableField("file_size")
    private Long fileSize;

    /**
     * 文件类型
     */
    @Schema(description = "文件类型")
    @TableField("file_type")
    private String fileType;

    /**
     * 文件扩展名
     */
    @Schema(description = "文件扩展名")
    @TableField("file_extension")
    private String fileExtension;

    /**
     * 文件MD5值
     */
    @Schema(description = "文件MD5值")
    @TableField("md5_hash")
    private String md5Hash;

    /**
     * 文件URL
     */
    @Schema(description = "文件URL")
    @TableField("file_url")
    private String fileUrl;

    /**
     * 上传用户ID
     */
    @Schema(description = "上传用户ID")
    @TableField("upload_user_id")
    private Long uploadUserId;

    /**
     * 状态：1-正常，2-已删除
     */
    @Schema(description = "状态：1-正常，2-已删除")
    @TableField("status")
    private Integer status;

    /**
     * 逻辑删除字段
     */
    @TableLogic
    @TableField(value = "deleted", fill = FieldFill.INSERT)
    private Integer deleted;
}