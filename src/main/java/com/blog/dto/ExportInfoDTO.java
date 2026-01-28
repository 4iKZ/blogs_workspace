package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "导出信息DTO")
public class ExportInfoDTO {
    
    @Schema(description = "导出ID")
    private Long exportId;
    
    @Schema(description = "导出文件名")
    private String fileName;
    
    @Schema(description = "导出文件路径")
    private String filePath;
    
    @Schema(description = "导出文件大小（字节）")
    private Long fileSize;
    
    @Schema(description = "导出类型：user/article/comment")
    private String exportType;
    
    @Schema(description = "记录数量")
    private Long recordCount;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "导出状态：success/failed/pending")
    private String status;
}