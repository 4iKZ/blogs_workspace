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
    md5: string
    status: number
    downloadCount: number
    createTime: string
    updateTime: string
}

export interface FileListResponse {
    total: number
    list: FileInfo[]
    pageNum: number
    pageSize: number
    pages: number
}

export const fileService = {
    /**
     * 获取文件列表
     * @param page 页码，默认1
     * @param size 每页数量，默认10
     * @param fileType 文件类型(可选)
     */
    async getFileList(page: number = 1, size: number = 10, fileType?: string): Promise<FileListResponse> {
        const params: any = { page, size }
        if (fileType) {
            params.fileType = fileType
        }
        const response = await axios.get('/file/list', { params })
        return response as unknown as FileListResponse
    },

    /**
     * 删除文件
     * @param fileId 文件ID
     */
    async deleteFile(fileId: number): Promise<void> {
        await axios.delete(`/file/${fileId}`)
    }
}
