package com.blog.service;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.dto.FileInfoDTO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务接口
 */
public interface FileUploadService {

    /**
     * 上传图片
     */
    Result<String> uploadImage(MultipartFile file);

    /**
     * 上传文件
     */
    Result<FileInfoDTO> uploadFile(MultipartFile file);

    /**
     * 获取文件列表
     */
    Result<PageResult<FileInfoDTO>> getFileList(Integer page, Integer size, String fileType);

    /**
     * 删除文件
     */
    Result<Void> deleteFile(Long fileId);

    /**
     * 获取文件详情
     */
    Result<FileInfoDTO> getFileById(Long fileId);

    /**
     * 检查文件是否存在（通过内容SHA-256哈希）
     */
    Result<FileInfoDTO> checkFileExists(String contentHash);
}