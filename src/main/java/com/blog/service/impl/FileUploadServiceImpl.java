package com.blog.service.impl;

import com.blog.common.Result;
import com.blog.common.ResultCode;
import com.blog.dto.FileInfoDTO;
import com.blog.dto.FileUploadDTO;
import com.blog.entity.FileInfo;
import com.blog.entity.FileCleanupTask;
import com.blog.exception.BusinessException;
import com.blog.mapper.FileInfoMapper;
import com.blog.mapper.FileCleanupTaskMapper;
import com.blog.service.FileUploadService;
import com.blog.service.TOSService;
import com.blog.utils.AuthUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.dao.DuplicateKeyException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文件上传服务实现类
 * 使用火山云TOS对象存储
 */
@Service
public class FileUploadServiceImpl implements FileUploadService {

    private static final Logger log = LoggerFactory.getLogger(FileUploadServiceImpl.class);

    @Value("${upload.max-size:5242880}")
    private long maxFileSize; // 字节，默认5MB，与前端一致

    @Autowired
    private FileInfoMapper fileInfoMapper;
    
    @Autowired
    private TOSService tosService;

    @Autowired
    private FileCleanupTaskMapper fileCleanupTaskMapper;

    @Override
    public Result<String> uploadImage(MultipartFile file) {
        try {
            // 验证文件类型
            String contentType = file.getContentType();
            if (!isImageFile(contentType)) {
                return Result.error("只允许上传图片文件");
            }

            // 验证文件大小
            if (file.getSize() > maxFileSize) {
                return Result.error("文件大小不能超过" + (maxFileSize / 1024 / 1024) + "MB");
            }

            // 上传到火山云TOS - covers文件夹（封面图）
            log.info("开始上传封面图片到TOS: {}", file.getOriginalFilename());
            // 使用带样式的上传方法，应用 lumina 样式（水印+压缩）
            String fileUrl = tosService.uploadFileWithStyle(file, "covers", true);

            log.info("封面图片上传成功（带lumina样式）: {}", fileUrl);
            return Result.success(fileUrl);

        } catch (com.volcengine.tos.TosServerException e) {
            log.error("上传图片失败，TOS服务器错误: statusCode={}, code={}, message={}", e.getStatusCode(), e.getCode(), e.getMessage());
            return Result.error("上传图片失败: [" + e.getStatusCode() + ":" + e.getCode() + "] " + e.getMessage());
        } catch (com.volcengine.tos.TosClientException e) {
            log.error("上传图片失败，TOS客户端错误: {}", e.getMessage());
            return Result.error("上传图片失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("上传图片失败", e);
            return Result.error("上传图片失败: " + e.getMessage());
        }
    }

    @Override
    public Result<FileInfoDTO> uploadFile(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.error("文件不能为空");
            }

            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String fileName = UUID.randomUUID().toString() + fileExtension;
            Long currentUserId = getCurrentUserId();
            String contentHash = calculateSha256(file);

            FileInfo existingFile = findByUserAndHash(currentUserId, contentHash);
            if (existingFile != null) {
                log.info("当前用户已上传相同内容，返回已有记录: hash={}", contentHash);
                return Result.success(convertToDTO(existingFile));
            }

            // 上传到火山云TOS - attachments文件夹
            log.info("开始上传文件到TOS: {}", originalFilename);
            String fileUrl = tosService.uploadFile(file, "attachments");

            // 从 URL 中提取 objectKey
            String objectKey = extractObjectKeyFromUrl(fileUrl);

            // 保存文件信息到数据库
            FileInfo fileInfo = new FileInfo();
            fileInfo.setOriginalName(originalFilename);
            fileInfo.setFileName(fileName);
            fileInfo.setMimeType(file.getContentType());
            fileInfo.setFileSize(file.getSize());
            fileInfo.setFilePath(objectKey);  // 存储TOS ObjectKey
            fileInfo.setFileUrl(fileUrl);     // 存储公开访问URL
            fileInfo.setUploadUserId(currentUserId);
            fileInfo.setCreateTime(LocalDateTime.now());
            fileInfo.setStatus("active");
            fileInfo.setFileCategory("attachment");
            fileInfo.setFileExtension(fileExtension);
            fileInfo.setContentHash(contentHash);

            try {
                int result = fileInfoMapper.insert(fileInfo);
                if (result > 0) {
                    log.info("文件上传成功：{}", originalFilename);
                    return Result.success(convertToDTO(fileInfo));
                }
                compensateUploadedObject(objectKey, "数据库未保存文件记录");
                return Result.error("保存文件信息失败");
            } catch (DuplicateKeyException duplicate) {
                compensateUploadedObject(objectKey, duplicate.getMessage());
                FileInfo winner = findByUserAndHash(currentUserId, contentHash);
                if (winner != null) {
                    return Result.success(convertToDTO(winner));
                }
                return Result.error("文件已存在，请重试");
            } catch (Exception databaseError) {
                compensateUploadedObject(objectKey, databaseError.getMessage());
                throw databaseError;
            }
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public Result<List<FileInfoDTO>> batchUploadFiles(List<MultipartFile> files) {
        try {
            List<FileInfoDTO> fileInfoDTOList = files.stream()
                    .map(this::uploadFile)
                    .filter(result -> result.getCode() == ResultCode.SUCCESS.getCode())
                    .map(result -> result.getData())
                    .collect(Collectors.toList());
            
            log.info("批量上传文件成功，共上传{}个文件", fileInfoDTOList.size());
            return Result.success(fileInfoDTOList);
        } catch (Exception e) {
            log.error("批量上传文件失败", e);
            return Result.error("批量上传文件失败");
        }
    }

    @Override
    public Result<List<FileInfoDTO>> getFileList(Integer page, Integer size, String fileType) {
        try {
            Long currentUserId = getCurrentUserId();
            // 使用MyBatis Plus分页查询文件列表
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<FileInfo> mpPage = 
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FileInfo> queryWrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            if (fileType != null && !fileType.isEmpty()) {
                queryWrapper.eq("file_type", fileType);
            }
            if (!AuthUtils.isAdmin()) {
                queryWrapper.eq("upload_user_id", currentUserId);
            }
            queryWrapper.orderByDesc("create_time");
            
            com.baomidou.mybatisplus.core.metadata.IPage<FileInfo> resultPage = 
                fileInfoMapper.selectPage(mpPage, queryWrapper);
            List<FileInfo> fileInfoList = resultPage.getRecords();
            List<FileInfoDTO> fileInfoDTOList = fileInfoList.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            return Result.success(fileInfoDTOList);
        } catch (Exception e) {
            log.error("获取文件列表失败", e);
            return Result.error("获取文件列表失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteFile(Long fileId) {
        try {
            FileInfo fileInfo = fileInfoMapper.selectById(fileId);
            if (fileInfo == null) {
                return Result.error("文件不存在");
            }
            assertCanAccess(fileInfo);

            String objectKey = fileInfo.getFilePath();
            int result = fileInfoMapper.deleteById(fileId);
            if (result > 0) {
                deleteObjectAfterCommit(objectKey);
                log.info("删除文件成功：{}", fileInfo.getFileName());
                return Result.success();
            } else {
                return Result.error("删除文件失败");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除文件失败", e);
            return Result.error("删除文件失败");
        }
    }

    @Override
    public Result<FileInfoDTO> getFileById(Long fileId) {
        try {
            FileInfo fileInfo = fileInfoMapper.selectById(fileId);
            if (fileInfo == null) {
                return Result.error("文件不存在");
            }
            assertCanAccess(fileInfo);
            FileInfoDTO fileInfoDTO = convertToDTO(fileInfo);
            return Result.success(fileInfoDTO);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取文件详情失败", e);
            return Result.error("获取文件详情失败");
        }
    }

    @Override
    public Result<FileInfoDTO> checkFileExists(String fileMd5) {
        try {
            Long currentUserId = getCurrentUserId();
            FileInfo existingFile = findByUserAndHash(currentUserId, fileMd5);
            if (existingFile != null) {
                assertCanAccess(existingFile);
                return Result.success(convertToDTO(existingFile));
            } else {
                return Result.error("文件不存在");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("检查文件是否存在失败", e);
            return Result.error("检查文件是否存在失败");
        }
    }

    private boolean isImageFile(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }

    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf("."));
        }
        return "";
    }

    private String calculateSha256(MultipartFile file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[8192];
        try (InputStream input = file.getInputStream()) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return java.util.HexFormat.of().formatHex(digest.digest());
    }

    private FileInfo findByUserAndHash(Long userId, String contentHash) {
        return fileInfoMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FileInfo>()
                        .eq("upload_user_id", userId)
                        .eq("content_hash", contentHash)
                        .eq("status", "active")
        );
    }

    private void compensateUploadedObject(String objectKey, String error) {
        try {
            if (tosService.deleteFile(objectKey)) {
                return;
            }
        } catch (Exception deleteError) {
            error = deleteError.getMessage();
        }

        FileCleanupTask task = new FileCleanupTask();
        task.setObjectKey(objectKey);
        task.setRetryCount(0);
        task.setNextRetryTime(LocalDateTime.now().plusMinutes(1));
        task.setStatus("pending");
        task.setLastError(error);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        try {
            fileCleanupTaskMapper.insert(task);
        } catch (Exception cleanupError) {
            log.error("记录TOS清理任务失败，objectKey={}", objectKey, cleanupError);
        }
    }
    
    /**
     * 从 URL 中提取 objectKey
     * URL格式: https://syhaox.tos-cn-beijing.volces.com/old_book_system/covers/2025/12/08/uuid.jpg
     * 返回: old_book_system/covers/2025/12/08/uuid.jpg
     */
    private String extractObjectKeyFromUrl(String url) {
        if (url == null || !url.contains("/")) {
            return url;
        }
        // 找到第三个/之后的内容
        int thirdSlash = url.indexOf("/", url.indexOf("/", url.indexOf("/") + 1) + 1);
        if (thirdSlash > 0 && thirdSlash < url.length() - 1) {
            return url.substring(thirdSlash + 1);
        }
        return url;
    }

    private Long getCurrentUserId() {
        return AuthUtils.getCurrentUserId();
    }

    private void assertCanAccess(FileInfo fileInfo) {
        Long currentUserId = getCurrentUserId();
        if (!AuthUtils.isAdmin() && !java.util.Objects.equals(fileInfo.getUploadUserId(), currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权限访问该文件");
        }
    }

    private void deleteObjectAfterCommit(String objectKey) {
        Runnable deleteAction = () -> {
            if (!tosService.deleteFile(objectKey)) {
                log.error("数据库记录已删除，但TOS对象删除失败，objectKey={}", objectKey);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteAction.run();
                }
            });
        } else {
            deleteAction.run();
        }
    }

    private FileInfoDTO convertToDTO(FileInfo fileInfo) {
        FileInfoDTO fileInfoDTO = new FileInfoDTO();
        BeanUtils.copyProperties(fileInfo, fileInfoDTO);
        
        // fileUrl已经是TOS的公开URL，直接返回
        fileInfoDTO.setFileUrl(fileInfo.getFileUrl());
        
        return fileInfoDTO;
    }
}
