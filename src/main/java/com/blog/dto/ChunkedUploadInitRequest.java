package com.blog.dto;

public record ChunkedUploadInitRequest(
        String uploadId,
        String fileName,
        Long fileSize,
        Integer totalChunks,
        String fileHash) {
}
