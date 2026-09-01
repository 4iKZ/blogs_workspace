import axios from '../utils/axios'

export interface FileInfo {
    id: number
    userId: number
    fileName: string
    originalName: string
    fileSize: number
    fileType: string
    fileUrl: string
    localPath?: string
    contentHash?: string
    status: number
    downloadCount: number
    createTime: string
    updateTime: string
}

export interface FileListResult {
    records?: FileInfo[]
    items?: FileInfo[]
    total: number
}

export const fileService = {
    /**
     * 获取文件列表（后端返回分页对象，含 records|items 与 total）
     * @param page 页码，默认1
     * @param size 每页数量，默认10
     * @param fileType 文件类型(可选)
     */
    async getFileList(page: number = 1, size: number = 10, fileType?: string): Promise<FileListResult> {
        const params: any = { page, size }
        if (fileType) {
            params.fileType = fileType
        }
        return axios.get<FileListResult>('/file/list', { params })
    },

    /**
     * 删除文件
     * @param fileId 文件ID
     */
    async deleteFile(fileId: number): Promise<void> {
        await axios.delete(`/file/${fileId}`)
    }
}
