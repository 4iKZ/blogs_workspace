package com.blog.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.Set;

/**
 * 分片上传服务接口
 */
public interface ChunkedUploadService {

    long CHUNK_SIZE = 5L * 1024 * 1024;
    long MAX_FILE_SIZE = 10L * 1024 * 1024;

    UploadInitialization initUpload(Long currentUserId, String fileName, long fileSize,
                                    int totalChunks, String fileHash);

    boolean uploadChunk(Long currentUserId, String uploadId, int chunkIndex, MultipartFile chunk);

    String completeUpload(Long currentUserId, String uploadId);

    boolean cancelUpload(Long currentUserId, String uploadId);

    String checkResumeUpload(Long currentUserId, String fileHash);

    ChunkedUploadStatus getUploadStatus(Long currentUserId, String uploadId);

    record UploadInitialization(String uploadId, long chunkSize, long maxFileSize, long expiresAt) {
    }

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
        private String detectedMime;
        private Set<Integer> uploadedIndices = Set.of();

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
        public String getDetectedMime() { return detectedMime; }
        public Set<Integer> getUploadedIndices() { return uploadedIndices; }

        public void setUploadedChunks(int uploadedChunks) { this.uploadedChunks = uploadedChunks; }
        public void setUploadedBytes(long uploadedBytes) { this.uploadedBytes = uploadedBytes; }
        public void setCompleted(boolean completed) { this.completed = completed; }
        public void setDetectedMime(String detectedMime) { this.detectedMime = detectedMime; }
        public void setUploadedIndices(Set<Integer> uploadedIndices) { this.uploadedIndices = Set.copyOf(uploadedIndices); }
    }
}
