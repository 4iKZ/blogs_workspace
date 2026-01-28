package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据恢复DTO
 */
@Data
@Schema(description = "数据恢复信息")
public class RestoreDTO {
    
    @Schema(description = "恢复ID")
    private Long restoreId;
    
    @Schema(description = "备份ID")
    private Long backupId;
    
    @Schema(description = "备份信息")
    private BackupDTO backupInfo;
    
    @Schema(description = "恢复类型：full-全量恢复，partial-部分恢复")
    private String restoreType;
    
    @Schema(description = "恢复状态：pending-待处理，running-进行中，completed-已完成，failed-失败")
    private String status;
    
    @Schema(description = "恢复开始时间")
    private LocalDateTime startTime;
    
    @Schema(description = "恢复结束时间")
    private LocalDateTime endTime;
    
    @Schema(description = "恢复耗时（秒）")
    private Long duration;
    
    @Schema(description = "恢复结果描述")
    private String result;
    
    @Schema(description = "错误信息")
    private String errorMessage;
    
    @Schema(description = "创建人")
    private String createdBy;
    
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}