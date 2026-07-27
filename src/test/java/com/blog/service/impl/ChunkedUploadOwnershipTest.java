package com.blog.service.impl;

import com.blog.exception.BusinessException;
import com.blog.service.TOSService;
import com.blog.utils.RedisDistributedLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ChunkedUploadOwnershipTest {

    @TempDir
    Path tempDir;
    @Mock RedisTemplate<String, Object> redis;
    @Mock HashOperations<String, Object, Object> hashes;
    @Mock ValueOperations<String, Object> values;
    @Mock TOSService tos;
    @Mock RedisDistributedLock locks;
    @Mock MultipartFile chunk;

    private ChunkedUploadServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForHash()).thenReturn(hashes);
        lenient().when(redis.opsForValue()).thenReturn(values);
        service = new ChunkedUploadServiceImpl(tos, redis, locks, tempDir.toString(), 24);
    }

    @Test
    void anotherUserCannotReadUploadStatus() {
        String uploadId = UUID.randomUUID().toString();
        Map<Object, Object> session = new HashMap<>();
        session.put("uploadId", uploadId);
        session.put("ownerUserId", "101");
        session.put("fileName", "cover.png");
        session.put("fileSize", "1");
        session.put("totalChunks", "1");
        session.put("uploadedChunks", "0");
        session.put("uploadedBytes", "0");
        session.put("status", "UPLOADING");
        when(hashes.entries("upload:session:" + uploadId)).thenReturn(session);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.getUploadStatus(202L, uploadId));

        assertEquals(404, error.getCode());
    }

    @Test
    void anotherUserCannotUploadCancelOrComplete() {
        String uploadId = UUID.randomUUID().toString();
        Map<Object, Object> session = Map.of(
                "uploadId", uploadId,
                "ownerUserId", "101",
                "fileName", "cover.png",
                "fileSize", "1",
                "totalChunks", "1",
                "status", "UPLOADING");
        when(hashes.entries("upload:session:" + uploadId)).thenReturn(session);

        assertEquals(404, assertThrows(BusinessException.class,
                () -> service.uploadChunk(202L, uploadId, 0, chunk)).getCode());
        assertEquals(404, assertThrows(BusinessException.class,
                () -> service.cancelUpload(202L, uploadId)).getCode());
        assertEquals(404, assertThrows(BusinessException.class,
                () -> service.completeUpload(202L, uploadId)).getCode());
    }

    @Test
    void hashResumeIsIsolatedPerUser() {
        String id = UUID.randomUUID().toString();
        Map<Object, Object> session = new HashMap<>();
        session.put("uploadId", id);
        session.put("ownerUserId", "101");
        session.put("fileName", "cover.png");
        session.put("fileSize", "1");
        session.put("totalChunks", "1");
        session.put("status", "UPLOADING");
        when(values.get("upload:hash:101:same-hash")).thenReturn(id);
        when(hashes.entries("upload:session:" + id)).thenReturn(session);
        when(values.get("upload:hash:202:same-hash")).thenReturn(null);
        assertEquals(id, service.checkResumeUpload(101L, "same-hash"));
        assertEquals(null, service.checkResumeUpload(202L, "same-hash"));
    }
}
