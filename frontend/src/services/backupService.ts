import axios from '../utils/axios'
import type { BackupInfo, ExportInfo } from '../types/backup'

export const backupService = {
    // ===== 数据库备份 =====

    /** 创建数据库备份 */
    createDatabaseBackup: (backupName: string, description?: string) =>
        axios.post<BackupInfo>('/system/backup/database', null, {
            params: { backupName, description }
        }),

    /** 获取备份列表 */
    getBackupList: () =>
        axios.get<BackupInfo[]>('/system/backup/list'),

    /** 删除备份 */
    deleteBackup: (backupId: number) =>
        axios.delete<void>(`/system/backup/${backupId}`),

    /** 恢复数据库 */
    restoreDatabase: (backupId: number) =>
        axios.post<void>(`/system/backup/restore/${backupId}`),

    /** 下载备份文件 */
    downloadBackup: (backupId: number) =>
        axios.get<BackupInfo>(`/system/backup/download/${backupId}`),

    // ===== 数据导出 =====

    /** 导出用户数据 */
    exportUserData: (userId?: number) =>
        axios.post<ExportInfo>('/system/backup/export/user', null, {
            params: userId ? { userId } : {}
        }),

    /** 导出文章数据 */
    exportArticleData: (categoryId?: number) =>
        axios.post<ExportInfo>('/system/backup/export/article', null, {
            params: categoryId ? { categoryId } : {}
        }),

    /** 导出评论数据 */
    exportCommentData: (articleId?: number) =>
        axios.post<ExportInfo>('/system/backup/export/comment', null, {
            params: articleId ? { articleId } : {}
        }),

    /** 获取导出文件列表 */
    getExportFileList: () =>
        axios.get<ExportInfo[]>('/system/backup/export/list'),

    /** 删除导出文件 */
    deleteExportFile: (exportId: number) =>
        axios.delete<void>(`/system/backup/export/${exportId}`),

    /** 下载导出文件 */
    downloadExportFile: (exportId: number) =>
        axios.get<ExportInfo>(`/system/backup/export/download/${exportId}`)
}
