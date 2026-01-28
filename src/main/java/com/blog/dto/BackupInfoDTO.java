package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "备份信息DTO")
public class BackupInfoDTO {
    
    @Schema(description = "备份ID")
    private Long backupId;
    
    @Schema(description = "备份文件名")
    private String fileName;
    
    @Schema(description = "备份文件路径")
    private String filePath;
    
    @Schema(description = "备份文件大小（字节）")
    private Long fileSize;
    
    @Schema(description = "备份类型：database/user/article/comment")
    private String backupType;
    
    @Schema(description = "备份描述")
    private String description;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "备份状态：success/failed/pending")
    private String status;
}