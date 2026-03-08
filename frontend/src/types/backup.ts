/** 备份信息 */
export interface BackupInfo {
  backupId: number
  fileName: string
  filePath: string
  fileSize: number
  backupType: string
  description: string
  createTime: string
  status: string
}

/** 导出信息 */
export interface ExportInfo {
  exportId: number
  fileName: string
  filePath: string
  fileSize: number
  exportType: string
  recordCount: number
  createTime: string
  status: string
}
