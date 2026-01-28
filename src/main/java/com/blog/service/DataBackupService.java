package com.blog.service;

import com.blog.common.Result;
import com.blog.dto.BackupInfoDTO;
import com.blog.dto.ExportInfoDTO;

import java.util.List;

/**
 * 数据备份与恢复服务接口
 */
public interface DataBackupService {

    /**
     * 创建数据库备份
     * @param backupName 备份名称
     * @param description 备份描述
     * @return 备份文件信息
     */
    Result<BackupInfoDTO> createDatabaseBackup(String backupName, String description);

    /**
     * 获取备份列表
     * @return 备份列表
     */
    Result<List<BackupInfoDTO>> getBackupList();

    /**
     * 删除备份
     * @param backupId 备份ID
     * @return 操作结果
     */
    Result<Void> deleteBackup(Long backupId);

    /**
     * 恢复数据库
     * @param backupId 备份ID
     * @return 操作结果
     */
    Result<Void> restoreDatabase(Long backupId);

    /**
     * 导出用户数据
     * @param userId 用户ID（可选，为空则导出所有用户）
     * @return 导出文件信息
     */
    Result<ExportInfoDTO> exportUserData(Long userId);

    /**
     * 导出文章数据
     * @param categoryId 分类ID（可选，为空则导出所有文章）
     * @return 导出文件信息
     */
    Result<ExportInfoDTO> exportArticleData(Long categoryId);

    /**
     * 导出评论数据
     * @param articleId 文章ID（可选，为空则导出所有评论）
     * @return 导出文件信息
     */
    Result<ExportInfoDTO> exportCommentData(Long articleId);

    /**
     * 获取导出文件列表
     * @return 导出文件列表
     */
    Result<List<ExportInfoDTO>> getExportFileList();

    /**
     * 删除导出文件
     * @param exportId 导出ID
     * @return 操作结果
     */
    Result<Void> deleteExportFile(Long exportId);

    /**
     * 下载备份文件
     * @param backupId 备份ID
     * @return 备份文件信息
     */
    Result<BackupInfoDTO> downloadBackup(Long backupId);

    /**
     * 下载导出文件
     * @param exportId 导出ID
     * @return 导出文件信息
     */
    Result<ExportInfoDTO> downloadExportFile(Long exportId);
}