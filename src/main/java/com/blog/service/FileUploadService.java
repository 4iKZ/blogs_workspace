package com.blog.service;

import com.blog.common.Result;
import com.blog.dto.FileInfoDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
     * 批量上传文件
     */
    Result<List<FileInfoDTO>> batchUploadFiles(List<MultipartFile> files);

    /**
     * 获取文件列表
     */
    Result<List<FileInfoDTO>> getFileList(Integer page, Integer size, String fileType);

    /**
     * 删除文件
     */
    Result<Void> deleteFile(Long fileId);

    /**
     * 获取文件详情
     */
    Result<FileInfoDTO> getFileById(Long fileId);

    /**
     * 检查文件是否存在（通过MD5）
     */
    Result<FileInfoDTO> checkFileExists(String fileMd5);
}