package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文件信息DTO
 */
@Data
@Schema(description = "文件信息DTO")
public class FileInfoDTO {

    @Schema(description = "文件ID")
    private Long id;

    @Schema(description = "文件名称")
    private String fileName;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "文件MD5值")
    private String fileMd5;

    @Schema(description = "文件URL")
    private String fileUrl;

    @Schema(description = "文件存储路径")
    private String filePath;

    @Schema(description = "上传用户ID")
    private Long uploadUserId;

    @Schema(description = "上传用户昵称")
    private String uploadUserName;

    @Schema(description = "上传时间")
    private String uploadTime;

    @Schema(description = "文件状态：0-正常，1-已删除")
    private Integer status;
}