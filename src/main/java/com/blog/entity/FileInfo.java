package com.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@TableName("file_info")
@Schema(description = "文件信息")
public class FileInfo {
    
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "文件ID")
    private Long id;
    
    @Schema(description = "原始文件名")
    private String originalName;
    
    @Schema(description = "存储文件名")
    private String fileName;
    
    @Schema(description = "文件路径")
    private String filePath;
    
    @Schema(description = "文件大小（字节）")
    private Long fileSize;
    
    @Schema(description = "文件类型")
    private String fileType;
    
    @Schema(description = "文件MIME类型")
    private String mimeType;
    
    @Schema(description = "文件扩展名")
    private String fileExtension;
    
    @Schema(description = "文件分类：image/attachment")
    private String fileCategory;
    
    @Schema(description = "上传用户ID")
    private Long uploadUserId;
    
    @Schema(description = "文件状态：active/deleted")
    private String status;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    
    @Schema(description = "文件访问URL")
    private String fileUrl;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getOriginalName() {
        return originalName;
    }
    
    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getFileType() {
        return fileType;
    }
    
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
    
    public String getMimeType() {
        return mimeType;
    }
    
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
    
    public String getFileExtension() {
        return fileExtension;
    }
    
    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }
    
    public String getFileCategory() {
        return fileCategory;
    }
    
    public void setFileCategory(String fileCategory) {
        this.fileCategory = fileCategory;
    }
    
    public Long getUploadUserId() {
        return uploadUserId;
    }
    
    public void setUploadUserId(Long uploadUserId) {
        this.uploadUserId = uploadUserId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
    
    public String getFileUrl() {
        return fileUrl;
    }
    
    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
}