package com.blog.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 分片上传服务接口
 */
public interface ChunkedUploadService {

    /**
     * 初始化分片上传会话
     */
    String initUpload(String uploadId, String fileName, long fileSize, int totalChunks, String fileHash);

    /**
     * 上传单个分片
     */
    boolean uploadChunk(String uploadId, int chunkIndex, MultipartFile chunk);

    /**
     * 完成分片上传，合并所有分片
     */
    String completeUpload(String uploadId);

    /**
     * 取消分片上传，清理临时文件
     */
    boolean cancelUpload(String uploadId);

    /**
     * 检查是否有可恢复的上传
     */
    String checkResumeUpload(String fileHash);

    /**
     * 获取上传状态
     */
    ChunkedUploadStatus getUploadStatus(String uploadId);

    /**
     * 分片上传状态
     */
    class ChunkedUploadStatus {
        private String uploadId;
        private String fileName;
        private long fileSize;
        private int totalChunks;
        private int uploadedChunks;
        private long uploadedBytes;
        private boolean completed;

        public ChunkedUploadStatus(String uploadId, String fileName, long fileSize, int totalChunks) {
            this.uploadId = uploadId;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.totalChunks = totalChunks;
            this.uploadedChunks = 0;
            this.uploadedBytes = 0;
            this.completed = false;
        }

        // Getters and Setters
        public String getUploadId() { return uploadId; }
        public String getFileName() { return fileName; }
        public long getFileSize() { return fileSize; }
        public int getTotalChunks() { return totalChunks; }
        public int getUploadedChunks() { return uploadedChunks; }
        public long getUploadedBytes() { return uploadedBytes; }
        public boolean isCompleted() { return completed; }

        public void setUploadedChunks(int uploadedChunks) { this.uploadedChunks = uploadedChunks; }
        public void setUploadedBytes(long uploadedBytes) { this.uploadedBytes = uploadedBytes; }
        public void setCompleted(boolean completed) { this.completed = completed; }
    }
}
