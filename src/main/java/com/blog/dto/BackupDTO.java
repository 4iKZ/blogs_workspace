package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据备份DTO
 */
@Data
@Schema(description = "数据备份信息")
public class BackupDTO {
    
    @Schema(description = "备份ID")
    private Long backupId;
    
    @Schema(description = "备份名称")
    private String backupName;
    
    @Schema(description = "备份类型：database-数据库备份，file-文件备份，full-全量备份")
    private String backupType;
    
    @Schema(description = "备份文件路径")
    private String filePath;
    
    @Schema(description = "备份文件大小（字节）")
    private Long fileSize;
    
    @Schema(description = "备份文件大小（格式化显示）")
    private String fileSizeFormatted;
    
    @Schema(description = "备份状态：pending-待处理，running-进行中，completed-已完成，failed-失败")
    private String status;
    
    @Schema(description = "备份开始时间")
    private LocalDateTime startTime;
    
    @Schema(description = "备份结束时间")
    private LocalDateTime endTime;
    
    @Schema(description = "备份耗时（秒）")
    private Long duration;
    
    @Schema(description = "备份描述")
    private String description;
    
    @Schema(description = "备份文件MD5值")
    private String fileMd5;
    
    @Schema(description = "创建人")
    private String createdBy;
    
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}