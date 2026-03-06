package com.blog.service.impl;

import com.blog.service.ChunkedUploadService;
import com.blog.service.ChunkedUploadService.ChunkedUploadStatus;
import com.blog.service.TOSService;
import com.blog.utils.RedisDistributedLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ChunkedUploadServiceImpl 单元测试类
 * 测试 P0-2 问题修复：分片上传状态从内存迁移到 Redis
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("分片上传服务测试")
public class ChunkedUploadServiceImplTest {

    @Mock
    private TOSService tosService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private Executor uploadCleanupExecutor;

    @Mock
    private RedisDistributedLock redisDistributedLock;

    @InjectMocks
    private ChunkedUploadServiceImpl chunkedUploadService;

    @TempDir
    Path tempDir;

    private static final String SESSION_KEY_PREFIX = "upload:session:";
    private static final String CHUNKS_KEY_PREFIX = "upload:chunks:";
    private static final String HASH_KEY_PREFIX = "upload:hash:";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(chunkedUploadService, "tempDir", tempDir.toString());
        ReflectionTestUtils.setField(chunkedUploadService, "expireHours", 24);

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ==================== P0-2: Redis 存储测试 ====================

    @Nested
    @DisplayName("P0-2: Redis 存储测试")
    class RedisStorageTests {

        @Test
        @DisplayName("测试初始化上传会话存储到 Redis")
        void testInitUpload_StoresToRedis() {
            String uploadId = "test-upload-id";
            String fileName = "test.jpg";
            long fileSize = 1024 * 1024;
            int totalChunks = 4;
            String fileHash = "abc123";

            ArgumentCaptor<Map<String, Object>> sessionDataCaptor = ArgumentCaptor.forClass(Map.class);

            String result = chunkedUploadService.initUpload(uploadId, fileName, fileSize, totalChunks, fileHash);

            assertEquals(uploadId, result);

            verify(hashOperations).putAll(eq(SESSION_KEY_PREFIX + uploadId), sessionDataCaptor.capture());
            verify(redisTemplate).expire(eq(SESSION_KEY_PREFIX + uploadId), eq(24L), any());
            verify(valueOperations).set(eq(HASH_KEY_PREFIX + fileHash), eq(uploadId), eq(24L), any());

            Map<String, Object> sessionData = sessionDataCaptor.getValue();
            assertEquals(uploadId, sessionData.get("uploadId"));
            assertEquals(fileName, sessionData.get("fileName"));
            assertEquals(String.valueOf(fileSize), sessionData.get("fileSize"));
            assertEquals(String.valueOf(totalChunks), sessionData.get("totalChunks"));
            assertEquals("0", sessionData.get("uploadedChunks"));
            assertEquals("false", sessionData.get("completed"));
        }

        @Test
        @DisplayName("测试初始化上传会话无文件哈希")
        void testInitUpload_NoFileHash() {
            String uploadId = "test-upload-id-2";
            String fileName = "test2.jpg";
            long fileSize = 2048;
            int totalChunks = 2;
            String fileHash = null;

            String result = chunkedUploadService.initUpload(uploadId, fileName, fileSize, totalChunks, fileHash);

            assertEquals(uploadId, result);
            verify(hashOperations).putAll(eq(SESSION_KEY_PREFIX + uploadId), any());
            verify(valueOperations, never()).set(eq(HASH_KEY_PREFIX + fileHash), any(), anyLong(), any());
        }

        @Test
        @DisplayName("测试获取上传状态从 Redis 读取")
        void testGetUploadStatus_ReadsFromRedis() {
            String uploadId = "test-upload-id-3";

            Map<Object, Object> redisData = new HashMap<>();
            redisData.put("uploadId", uploadId);
            redisData.put("fileName", "test.jpg");
            redisData.put("fileSize", "1048576");
            redisData.put("totalChunks", "4");
            redisData.put("uploadedChunks", "2");
            redisData.put("uploadedBytes", "524288");
            redisData.put("completed", "false");

            when(hashOperations.entries(SESSION_KEY_PREFIX + uploadId)).thenReturn(redisData);

            ChunkedUploadStatus status = chunkedUploadService.getUploadStatus(uploadId);

            assertNotNull(status);
            assertEquals(uploadId, status.getUploadId());
            assertEquals("test.jpg", status.getFileName());
            assertEquals(1048576, status.getFileSize());
            assertEquals(4, status.getTotalChunks());
            assertEquals(2, status.getUploadedChunks());
            assertEquals(524288, status.getUploadedBytes());
            assertFalse(status.isCompleted());
        }

        @Test
        @DisplayName("测试获取不存在的上传状态返回 null")
        void testGetUploadStatus_NotExists() {
            String uploadId = "non-existent-id";

            when(hashOperations.entries(SESSION_KEY_PREFIX + uploadId)).thenReturn(Collections.emptyMap());

            ChunkedUploadStatus status = chunkedUploadService.getUploadStatus(uploadId);

            assertNull(status);
        }

        @Test
        @DisplayName("测试检查可恢复上传从 Redis 查找")
        void testCheckResumeUpload_FoundInRedis() {
            String fileHash = "abc123";
            String uploadId = "resumable-upload-id";

            when(valueOperations.get(HASH_KEY_PREFIX + fileHash)).thenReturn(uploadId);

            Map<Object, Object> redisData = new HashMap<>();
            redisData.put("uploadId", uploadId);
            redisData.put("fileName", "resumable.jpg");
            redisData.put("fileSize", "1048576");
            redisData.put("totalChunks", "4");
            redisData.put("uploadedChunks", "2");
            redisData.put("completed", "false");

            when(hashOperations.entries(SESSION_KEY_PREFIX + uploadId)).thenReturn(redisData);

            String result = chunkedUploadService.checkResumeUpload(fileHash);

            assertEquals(uploadId, result);
        }

        @Test
        @DisplayName("测试检查可恢复上传已完成返回 null")
        void testCheckResumeUpload_AlreadyCompleted() {
            String fileHash = "abc123";
            String uploadId = "completed-upload-id";

            when(valueOperations.get(HASH_KEY_PREFIX + fileHash)).thenReturn(uploadId);

            Map<Object, Object> redisData = new HashMap<>();
            redisData.put("uploadId", uploadId);
            redisData.put("fileName", "completed.jpg");
            redisData.put("fileSize", "1048576");
            redisData.put("totalChunks", "4");
            redisData.put("completed", "true");

            when(hashOperations.entries(SESSION_KEY_PREFIX + uploadId)).thenReturn(redisData);

            String result = chunkedUploadService.checkResumeUpload(fileHash);

            assertNull(result);
        }
    }

    // ==================== 分片上传测试 ====================

    @Nested
    @DisplayName("分片上传测试")
    class UploadChunkTests {

        @Test
        @DisplayName("测试上传分片更新 Redis 进度")
        void testUploadChunk_UpdatesProgress() throws IOException {
            String uploadId = "chunk-test-id";
            int chunkIndex = 0;

            Map<Object, Object> sessionData = new HashMap<>();
            sessionData.put("uploadId", uploadId);
            sessionData.put("fileName", "test.jpg");
            sessionData.put("fileSize", "1048576");
            sessionData.put("totalChunks", "2");
            sessionData.put("completed", "false");

            when(hashOperations.entries(SESSION_KEY_PREFIX + uploadId)).thenReturn(sessionData);

            java.nio.file.Path sessionDir = tempDir.resolve(uploadId);
            Files.createDirectories(sessionDir);

            MultipartFile mockChunk = mock(MultipartFile.class);
            when(mockChunk.getSize()).thenReturn(512L);
            doAnswer(invocation -> {
                java.io.File file = invocation.getArgument(0);
                file.createNewFile();
                return null;
            }).when(mockChunk).transferTo(any(java.io.File.class));

            Map<Object, Object> chunksData = new HashMap<>();
            chunksData.put("0", sessionDir.resolve("chunk_0").toString());
            when(hashOperations.entries(CHUNKS_KEY_PREFIX + uploadId)).thenReturn(chunksData);

            when(hashOperations.get(CHUNKS_KEY_PREFIX + uploadId, "0")).thenReturn(null);

            when(redisDistributedLock.tryLock(anyString(), anyLong(), any()))
                    .thenReturn("lock-value-123");

            boolean result = chunkedUploadService.uploadChunk(uploadId, chunkIndex, mockChunk);

            assertTrue(result);
            verify(hashOperations).putIfAbsent(eq(CHUNKS_KEY_PREFIX + uploadId), eq("0"), any());
            verify(redisDistributedLock).unlock(anyString(), eq("lock-value-123"));
        }

        @Test
        @DisplayName("测试上传分片会话不存在返回 false")
        void testUploadChunk_SessionNotExists() throws IOException {
            String uploadId = "non-existent-session";
            int chunkIndex = 0;

            when(hashOperations.entries(SESSION_KEY_PREFIX + uploadId)).thenReturn(Collections.emptyMap());

            MultipartFile mockChunk = mock(MultipartFile.class);

            boolean result = chunkedUploadService.uploadChunk(uploadId, chunkIndex, mockChunk);

            assertFalse(result);
            verify(redisDistributedLock, never()).tryLock(anyString(), anyLong(), any());
        }

        @Test
        @DisplayName("测试上传分片已完成忽略")
        void testUploadChunk_AlreadyCompleted() throws IOException {
            String uploadId = "completed-upload";
            int chunkIndex = 0;

            Map<Object, Object> sessionData = new HashMap<>();
            sessionData.put("uploadId", uploadId);
            sessionData.put("completed", "true");

            when(hashOperations.entries(SESSION_KEY_PREFIX + uploadId)).thenReturn(sessionData);

            MultipartFile mockChunk = mock(MultipartFile.class);

            boolean result = chunkedUploadService.uploadChunk(uploadId, chunkIndex, mockChunk);

            assertTrue(result);
            verify(hashOperations, never()).put(any(), any(), any());
        }

        @Test
        @DisplayName("测试重复上传同一分片（幂等性）")
        void testUploadChunk_Idempotency() throws IOException {
            String uploadId = "idempotency-test";
            int chunkIndex = 0;

            Map<Object, Object> sessionData = new HashMap<>();
            sessionData.put("uploadId", uploadId);
            sessionData.put("fileName", "test.jpg");
            sessionData.put("fileSize", "1048576");
            sessionData.put("totalChunks", "2");
            sessionData.put("completed", "false");

            when(hashOperations.entries(SESSION_KEY_PREFIX + uploadId)).thenReturn(sessionData);

            java.nio.file.Path sessionDir = tempDir.resolve(uploadId);
            Files.createDirectories(sessionDir);

            MultipartFile mockChunk = mock(MultipartFile.class);
            when(mockChunk.getSize()).thenReturn(512L);
            doAnswer(invocation -> {
                java.io.File file = invocation.getArgument(0);
                file.createNewFile();
                return null;
            }).when(mockChunk).transferTo(any(java.io.File.class));

            Map<Object, Object> chunksData = new HashMap<>();
            chunksData.put("0", sessionDir.resolve("chunk_0").toString());
            when(hashOperations.entries(CHUNKS_KEY_PREFIX + uploadId)).thenReturn(chunksData);

            when(hashOperations.get(CHUNKS_KEY_PREFIX + uploadId, "0"))
                    .thenReturn(null)
                    .thenReturn(sessionDir.resolve("chunk_0").toString());

            when(redisDistributedLock.tryLock(anyString(), anyLong(), any()))
                    .thenReturn("lock-value-123");

            boolean result1 = chunkedUploadService.uploadChunk(uploadId, chunkIndex, mockChunk);
            boolean result2 = chunkedUploadService.uploadChunk(uploadId, chunkIndex, mockChunk);

            assertTrue(result1);
            assertTrue(result2);

            verify(hashOperations, times(1)).putIfAbsent(eq(CHUNKS_KEY_PREFIX + uploadId), eq("0"), any());
            verify(redisDistributedLock, times(2)).unlock(anyString(), eq("lock-value-123"));
        }

        @Test
        @DisplayName("测试并发上传同一分片（分布式锁）")
        void testUploadChunk_ConcurrentUpload() throws IOException {
            String uploadId = "concurrent-test";
            int chunkIndex = 0;

            Map<Object, Object> sessionData = new HashMap<>();
            sessionData.put("uploadId", uploadId);
            sessionData.put("fileName", "test.jpg");
            sessionData.put("fileSize", "1048576");
            sessionData.put("totalChunks", "2");
            sessionData.put("completed", "false");

            when(hashOperations.entries(SESSION_KEY_PREFIX + uploadId)).thenReturn(sessionData);

            java.nio.file.Path sessionDir = tempDir.resolve(uploadId);
            Files.createDirectories(sessionDir);

            MultipartFile mockChunk = mock(MultipartFile.class);
            when(mockChunk.getSize()).thenReturn(512L);

            Map<Object, Object> chunksData = new HashMap<>();
            when(hashOperations.entries(CHUNKS_KEY_PREFIX + uploadId)).thenReturn(chunksData);

            when(hashOperations.get(CHUNKS_KEY_PREFIX + uploadId, "0"))
                    .thenReturn(null)
                    .thenReturn(sessionDir.resolve("chunk_0").toString());

            when(redisDistributedLock.tryLock(anyString(), anyLong(), any()))
                    .thenReturn("lock-value-123");

            boolean result1 = chunkedUploadService.uploadChunk(uploadId, chunkIndex, mockChunk);
            boolean result2 = chunkedUploadService.uploadChunk(uploadId, chunkIndex, mockChunk);

            assertTrue(result1);
            assertTrue(result2);

            verify(hashOperations, times(1)).putIfAbsent(eq(CHUNKS_KEY_PREFIX + uploadId), eq("0"), any());
            verify(redisDistributedLock, times(2)).unlock(anyString(), eq("lock-value-123"));
        }

        @Test
        @DisplayName("测试分布式锁获取失败")
        void testUploadChunk_LockAcquisitionFailed() throws IOException {
            String uploadId = "lock-failed-test";
            int chunkIndex = 0;

            Map<Object, Object> sessionData = new HashMap<>();
            sessionData.put("uploadId", uploadId);
            sessionData.put("fileName", "test.jpg");
            sessionData.put("fileSize", "1048576");
            sessionData.put("totalChunks", "2");
            sessionData.put("completed", "false");

            when(hashOperations.entries(SESSION_KEY_PREFIX + uploadId)).thenReturn(sessionData);

            when(redisDistributedLock.tryLock(anyString(), anyLong(), any()))
                    .thenReturn(null);

            MultipartFile mockChunk = mock(MultipartFile.class);

            boolean result = chunkedUploadService.uploadChunk(uploadId, chunkIndex, mockChunk);

            assertFalse(result);
            verify(hashOperations, never()).put(any(), any(), any());
            verify(redisDistributedLock, never()).unlock(anyString(), anyString());
        }
    }

    // ==================== 取消上传测试 ====================

    @Nested
    @DisplayName("取消上传测试")
    class CancelUploadTests {

        @Test
        @DisplayName("测试取消上传清理 Redis 数据")
        void testCancelUpload_ClearsRedisData() {
            String uploadId = "cancel-test-id";

            when(redisTemplate.hasKey(SESSION_KEY_PREFIX + uploadId)).thenReturn(true);
            when(redisTemplate.keys(HASH_KEY_PREFIX + "*")).thenReturn(Collections.emptySet());

            boolean result = chunkedUploadService.cancelUpload(uploadId);

            assertTrue(result);
            verify(redisTemplate).delete(SESSION_KEY_PREFIX + uploadId);
            verify(redisTemplate).delete(CHUNKS_KEY_PREFIX + uploadId);
        }

        @Test
        @DisplayName("测试取消不存在的上传返回 false")
        void testCancelUpload_NotExists() {
            String uploadId = "non-existent-cancel";

            when(redisTemplate.hasKey(SESSION_KEY_PREFIX + uploadId)).thenReturn(false);

            boolean result = chunkedUploadService.cancelUpload(uploadId);

            assertFalse(result);
            verify(redisTemplate, never()).delete(anyString());
        }
    }

    // ==================== 服务重启恢复测试 ====================

    @Nested
    @DisplayName("服务重启恢复测试")
    class ServiceRestartTests {

        @Test
        @DisplayName("测试服务重启后可恢复上传状态")
        void testServiceRestart_CanResumeUpload() {
            String uploadId = "restart-resume-id";
            String fileHash = "restart-hash";

            when(valueOperations.get(HASH_KEY_PREFIX + fileHash)).thenReturn(uploadId);

            Map<Object, Object> redisData = new HashMap<>();
            redisData.put("uploadId", uploadId);
            redisData.put("fileName", "restart-test.jpg");
            redisData.put("fileSize", "2097152");
            redisData.put("totalChunks", "4");
            redisData.put("uploadedChunks", "1");
            redisData.put("uploadedBytes", "524288");
            redisData.put("completed", "false");

            when(hashOperations.entries(SESSION_KEY_PREFIX + uploadId)).thenReturn(redisData);

            String resumedUploadId = chunkedUploadService.checkResumeUpload(fileHash);
            ChunkedUploadStatus status = chunkedUploadService.getUploadStatus(resumedUploadId);

            assertNotNull(resumedUploadId);
            assertNotNull(status);
            assertEquals(1, status.getUploadedChunks());
            assertEquals(4, status.getTotalChunks());
        }

        @Test
        @DisplayName("测试多实例部署共享上传状态")
        void testMultipleInstances_ShareUploadState() {
            String uploadId = "shared-upload-id";

            Map<Object, Object> redisData = new HashMap<>();
            redisData.put("uploadId", uploadId);
            redisData.put("fileName", "shared.jpg");
            redisData.put("fileSize", "1048576");
            redisData.put("totalChunks", "2");
            redisData.put("uploadedChunks", "1");
            redisData.put("completed", "false");

            when(hashOperations.entries(SESSION_KEY_PREFIX + uploadId)).thenReturn(redisData);

            ChunkedUploadStatus status1 = chunkedUploadService.getUploadStatus(uploadId);
            ChunkedUploadStatus status2 = chunkedUploadService.getUploadStatus(uploadId);

            assertNotNull(status1);
            assertNotNull(status2);
            assertEquals(status1.getUploadedChunks(), status2.getUploadedChunks());
            assertEquals(status1.getTotalChunks(), status2.getTotalChunks());
        }
    }

    // ==================== TTL 过期测试 ====================

    @Nested
    @DisplayName("TTL 过期测试")
    class TTLExpiryTests {

        @Test
        @DisplayName("测试初始化上传设置 TTL")
        void testInitUpload_SetsTTL() {
            String uploadId = "ttl-test-id";

            chunkedUploadService.initUpload(uploadId, "test.jpg", 1024, 1, null);

            verify(redisTemplate).expire(eq(SESSION_KEY_PREFIX + uploadId), eq(24L), any());
        }

        @Test
        @DisplayName("测试上传分片设置 chunks TTL")
        void testUploadChunk_SetsChunksTTL() throws IOException {
            String uploadId = "chunk-ttl-test";
            int chunkIndex = 0;

            Map<Object, Object> sessionData = new HashMap<>();
            sessionData.put("uploadId", uploadId);
            sessionData.put("fileName", "test.jpg");
            sessionData.put("fileSize", "1024");
            sessionData.put("totalChunks", "1");
            sessionData.put("completed", "false");

            when(hashOperations.entries(SESSION_KEY_PREFIX + uploadId)).thenReturn(sessionData);

            java.nio.file.Path sessionDir = tempDir.resolve(uploadId);
            Files.createDirectories(sessionDir);

            MultipartFile mockChunk = mock(MultipartFile.class);
            when(mockChunk.getSize()).thenReturn(1024L);
            doAnswer(invocation -> {
                java.io.File file = invocation.getArgument(0);
                file.createNewFile();
                return null;
            }).when(mockChunk).transferTo(any(java.io.File.class));

            when(hashOperations.get(CHUNKS_KEY_PREFIX + uploadId, "0")).thenReturn(null);

            Map<Object, Object> chunksData = new HashMap<>();
            chunksData.put("0", sessionDir.resolve("chunk_0").toString());
            when(hashOperations.entries(CHUNKS_KEY_PREFIX + uploadId)).thenReturn(chunksData);

            when(redisDistributedLock.tryLock(anyString(), anyLong(), any()))
                    .thenReturn("lock-value-123");

            chunkedUploadService.uploadChunk(uploadId, chunkIndex, mockChunk);

            verify(redisTemplate).expire(eq(CHUNKS_KEY_PREFIX + uploadId), eq(24L), any());
            verify(redisDistributedLock).unlock(anyString(), eq("lock-value-123"));
        }
    }

    // ==================== 边界条件测试 ====================

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("测试空文件哈希不存储映射")
        void testEmptyFileHash_NoMapping() {
            String uploadId = "empty-hash-test";

            chunkedUploadService.initUpload(uploadId, "test.jpg", 1024, 1, "");

            verify(valueOperations, never()).set(startsWith(HASH_KEY_PREFIX), any(), anyLong(), any());
        }

        @Test
        @DisplayName("测试获取上传状态数据不完整返回 null")
        void testGetUploadStatus_IncompleteData() {
            String uploadId = "incomplete-data";

            Map<Object, Object> redisData = new HashMap<>();
            redisData.put("uploadId", uploadId);

            when(hashOperations.entries(SESSION_KEY_PREFIX + uploadId)).thenReturn(redisData);

            ChunkedUploadStatus status = chunkedUploadService.getUploadStatus(uploadId);

            assertNull(status);
        }
    }
}
