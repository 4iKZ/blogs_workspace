import axios from '../utils/axios'
import type { BackupInfo, ExportInfo } from '../types/backup'
import type { AxiosResponse } from 'axios'
import { parseDownloadFilename } from '../utils/download'

export interface DownloadResult {
    blob: Blob
    filename: string
}

const toDownloadResult = (
    response: AxiosResponse<Blob>,
    fallbackFilename: string
): DownloadResult => ({
    blob: response.data,
    filename: parseDownloadFilename(
        response.headers['content-disposition'],
        fallbackFilename
    )
})

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

    /** 下载备份文件 */
    downloadBackup: async (backupId: number) =>
        toDownloadResult(
            await axios.get<AxiosResponse<Blob>>(
                `/system/backup/download/${backupId}`,
                { responseType: 'blob' }
            ),
            `backup-${backupId}.sql`
        ),

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
    downloadExportFile: async (exportId: number) =>
        toDownloadResult(
            await axios.get<AxiosResponse<Blob>>(
                `/system/backup/export/download/${exportId}`,
                { responseType: 'blob' }
            ),
            `export-${exportId}.json`
        )
}
