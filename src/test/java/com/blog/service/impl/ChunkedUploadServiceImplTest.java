package com.blog.service.impl;

import com.blog.exception.BusinessException;
import com.blog.service.ChunkedUploadService;
import com.blog.service.TOSService;
import com.blog.utils.RedisDistributedLock;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.data.redis.core.script.RedisScript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ChunkedUploadServiceImplTest {
    private FileSystem fileSystem;
    private Path tempDir;
    @Mock TOSService tos;
    @Mock RedisTemplate<String, Object> redis;
    @Mock HashOperations<String, Object, Object> hashes;
    @Mock ValueOperations<String, Object> values;
    @Mock ZSetOperations<String, Object> zsets;
    @Mock RedisDistributedLock locks;
    private ChunkedUploadServiceImpl service;
    private SafeUploadPathResolver paths;

    @BeforeEach
    void setUp() throws IOException {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        tempDir = fileSystem.getPath("/uploads");
        Files.createDirectories(tempDir);
        paths = new SafeUploadPathResolver(tempDir);
        lenient().when(redis.opsForHash()).thenReturn(hashes);
        lenient().when(redis.expire(anyString(), eq(24L), eq(TimeUnit.HOURS))).thenReturn(true);
        lenient().when(zsets.add(eq("upload:expiry"), any(), any(Double.class))).thenReturn(true);
        lenient().when(locks.tryLockWithWatchdog(
                org.mockito.ArgumentMatchers.startsWith("upload:session-lock:"),
                eq(30L), eq(TimeUnit.SECONDS), eq(3L), eq(TimeUnit.SECONDS))).thenReturn("session-lock");
        lenient().when(locks.tryLockWithWatchdog(
                org.mockito.ArgumentMatchers.startsWith("upload:init:"),
                eq(30L), eq(TimeUnit.SECONDS), eq(3L), eq(TimeUnit.SECONDS))).thenReturn("init-lock");
        service = new ChunkedUploadServiceImpl(tos, redis, locks, tempDir, 24);
    }

    @AfterEach
    void closeFileSystem() throws IOException {
        fileSystem.close();
    }

    @Test
    void initializationGeneratesUuidAndStoresOwnerScopedHash() {
        when(redis.opsForValue()).thenReturn(values);
        lenient().when(redis.opsForZSet()).thenReturn(zsets);
        when(values.setIfAbsent(anyString(), anyString(), eq(24L), eq(TimeUnit.HOURS))).thenReturn(true);

        ChunkedUploadService.UploadInitialization result =
                service.initUpload(7L, "cover.png", 1, 1, "sha256");

        UUID.fromString(result.uploadId());
        assertEquals(ChunkedUploadService.CHUNK_SIZE, result.chunkSize());
        assertEquals(ChunkedUploadService.MAX_FILE_SIZE, result.maxFileSize());
        verify(values).setIfAbsent(eq("upload:hash:7:sha256"), eq(result.uploadId()),
                eq(24L), eq(TimeUnit.HOURS));
        verify(hashes).putAll(eq("upload:session:" + result.uploadId()),
                org.mockito.ArgumentMatchers.argThat(map -> "7".equals(map.get("ownerUserId"))));
    }

    @Test
    void initializationRedisFailureCompensatesMarkerAndAllIndexes() throws Exception {
        when(redis.opsForZSet()).thenReturn(zsets);
        doThrow(new IllegalStateException("redis unavailable"))
                .when(hashes).putAll(anyString(), anyMap());

        assertThrows(BusinessException.class,
                () -> service.initUpload(7L, "cover.png", 1, 1, null));

        try (var files = Files.list(tempDir)) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().endsWith(".session")));
        }
        verify(redis).delete(argThat((String key) -> key.startsWith("upload:session:")));
        verify(redis).delete(argThat((String key) -> key.startsWith("upload:chunks:")));
        verify(zsets).remove(eq("upload:expiry"), any());
    }

    @Test
    void initializationExpiryIndexFailureCompensatesHashSessionAndMarker() throws Exception {
        when(redis.opsForValue()).thenReturn(values);
        when(redis.opsForZSet()).thenReturn(zsets);
        when(values.setIfAbsent(anyString(), anyString(), eq(24L), eq(TimeUnit.HOURS))).thenReturn(true);
        when(zsets.add(eq("upload:expiry"), any(), any(Double.class)))
                .thenThrow(new IllegalStateException("zset unavailable"));

        assertThrows(BusinessException.class,
                () -> service.initUpload(7L, "cover.png", 1, 1, "sha256"));

        try (var files = Files.list(tempDir)) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().endsWith(".session")));
        }
        ArgumentCaptor<String> claimedId = ArgumentCaptor.forClass(String.class);
        verify(values).setIfAbsent(eq("upload:hash:7:sha256"), claimedId.capture(),
                eq(24L), eq(TimeUnit.HOURS));
        verify(redis).execute(any(RedisScript.class), eq(java.util.List.of("upload:hash:7:sha256")),
                eq(claimedId.getValue()));
        verify(redis, never()).delete("upload:hash:7:sha256");
        verify(redis).delete(argThat((String key) -> key.startsWith("upload:session:")));
        verify(redis).delete(argThat((String key) -> key.startsWith("upload:chunks:")));
    }

    @Test
    void initializationReusesExistingOwnerHashMappingWithoutCreatingAnotherSession() throws Exception {
        String existingId = UUID.randomUUID().toString();
        Map<Object, Object> existing = session(existingId, 1, 1);
        existing.put("ownerUserId", "7");
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("upload:hash:7:sha256")).thenReturn(existingId);
        when(hashes.entries("upload:session:" + existingId)).thenReturn(existing);

        ChunkedUploadService.UploadInitialization result =
                service.initUpload(7L, "cover.png", 1, 1, "sha256");

        assertEquals(existingId, result.uploadId());
        verify(values, never()).setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        try (var files = Files.list(tempDir)) {
            assertFalse(files.findAny().isPresent());
        }
    }

    @Test
    void initializationCasLoserReturnsWinningOwnerSessionWithoutCreatingMarker() throws Exception {
        String winnerId = UUID.randomUUID().toString();
        Map<Object, Object> winner = session(winnerId, 1, 1);
        winner.put("ownerUserId", "7");
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("upload:hash:7:sha256")).thenReturn(null, winnerId);
        when(values.setIfAbsent(eq("upload:hash:7:sha256"), anyString(),
                eq(24L), eq(TimeUnit.HOURS))).thenReturn(false);
        when(hashes.entries("upload:session:" + winnerId)).thenReturn(winner);

        ChunkedUploadService.UploadInitialization result =
                service.initUpload(7L, "cover.png", 1, 1, "sha256");

        assertEquals(winnerId, result.uploadId());
        try (var files = Files.list(tempDir)) {
            assertFalse(files.findAny().isPresent());
        }
    }

    @Test
    void initializationRejectsSizeChunkCountAndUnsafeNameBoundaries() {
        assertThrows(BusinessException.class, () -> service.initUpload(1L, "a.png", 0, 1, null));
        assertThrows(BusinessException.class, () -> service.initUpload(
                1L, "a.png", ChunkedUploadService.MAX_FILE_SIZE + 1, 3, null));
        assertThrows(BusinessException.class, () -> service.initUpload(
                1L, "a.png", ChunkedUploadService.CHUNK_SIZE + 1, 1, null));
        assertThrows(IllegalArgumentException.class, () -> service.initUpload(1L, "../a.png", 1, 1, null));
    }

    @ParameterizedTest
    @CsvSource({"1,1", "5242880,1", "5242881,2", "10485760,2"})
    void acceptsExactFileSizeBoundaries(long size, int chunks) {
        lenient().when(redis.opsForZSet()).thenReturn(zsets);
        ChunkedUploadService.UploadInitialization result =
                service.initUpload(1L, "cover.png", size, chunks, null);
        assertEquals(chunks, (size + ChunkedUploadService.CHUNK_SIZE - 1) / ChunkedUploadService.CHUNK_SIZE);
        UUID.fromString(result.uploadId());
    }

    @Test
    void duplicateChunkIsIdempotentAndDoesNotAccumulate() throws Exception {
        String id = UUID.randomUUID().toString();
        paths.createSessionDirectory(id);
        Path existing = Files.write(paths.resolveChunkFile(id, 0), new byte[]{1});
        when(hashes.entries("upload:session:" + id)).thenReturn(session(id, 1L, 1));
        when(hashes.get("upload:chunks:" + id, "0")).thenReturn(existing.toString());
        when(locks.tryLock(anyString(), eq(1L), eq(TimeUnit.MINUTES))).thenReturn("lock");

        assertTrue(service.uploadChunk(1L, id, 0,
                new MockMultipartFile("file", "a.png", "image/png", new byte[]{1})));

        verify(hashes, never()).putIfAbsent(eq("upload:chunks:" + id), eq("0"), anyString());
    }

    @Test
    void statusReturnsUploadedIndexSetAndCumulativeOverflowIsRejected() throws Exception {
        String id = UUID.randomUUID().toString();
        paths.createSessionDirectory(id);
        Path chunk = paths.resolveChunkFile(id, 0);
        Path extra = Files.write(paths.resolveChunkFile(id, 9), new byte[]{9});
        when(hashes.entries("upload:session:" + id)).thenReturn(session(id, 1L, 1));
        when(hashes.entries("upload:chunks:" + id)).thenReturn(Map.of("0", chunk.toString(), "9", extra.toString()));
        when(hashes.get("upload:chunks:" + id, "0")).thenReturn(null);
        when(hashes.putIfAbsent("upload:chunks:" + id, "0", chunk.toString())).thenReturn(true);
        when(locks.tryLock(anyString(), eq(1L), eq(TimeUnit.MINUTES))).thenReturn("lock");

        assertThrows(BusinessException.class, () -> service.uploadChunk(1L, id, 0,
                new MockMultipartFile("file", new byte[]{1})));

        when(hashes.entries("upload:chunks:" + id)).thenReturn(Map.of("0", chunk.toString()));
        assertEquals(Set.of(0), service.getUploadStatus(1L, id).getUploadedIndices());
    }

    @Test
    void rejectsWrongChunkSizeAndPseudoImage() throws Exception {
        String id = UUID.randomUUID().toString();
        paths.createSessionDirectory(id);
        Map<Object, Object> session = session(id, 3L, 1);
        Path chunkPath = paths.resolveChunkFile(id, 0);
        Map<Object, Object> chunks = Map.of("0", chunkPath.toString());
        when(hashes.entries("upload:session:" + id)).thenReturn(session);

        assertThrows(BusinessException.class, () -> service.uploadChunk(1L, id, 0,
                new MockMultipartFile("file", new byte[]{1, 2})));

        when(locks.tryLock(anyString(), eq(1L), eq(TimeUnit.MINUTES))).thenReturn("lock");
        when(hashes.get("upload:chunks:" + id, "0")).thenReturn(null);
        when(hashes.putIfAbsent("upload:chunks:" + id, "0", chunkPath.toString())).thenReturn(true);
        when(hashes.entries("upload:chunks:" + id)).thenReturn(chunks);
        service.uploadChunk(1L, id, 0, new MockMultipartFile("file", new byte[]{1, 2, 3}));

        assertThrows(BusinessException.class, () -> service.completeUpload(1L, id));
        verify(tos, never()).uploadFileWithStyle(
                org.mockito.ArgumentMatchers.any(), eq("covers"), eq(true));
    }

    @Test
    void expiredSessionIsHiddenAndLeftIndexedForLockedSchedulerCleanup() throws Exception {
        String id = UUID.randomUUID().toString();
        Path sessionDir = paths.createSessionDirectory(id);
        Files.writeString(paths.resolveChunkFile(id, 0), "x");
        Map<Object, Object> session = session(id, 1L, 1);
        session.put("expiresAt", String.valueOf(System.currentTimeMillis() - 1));
        when(hashes.entries("upload:session:" + id)).thenReturn(session);

        assertEquals(404, assertThrows(BusinessException.class,
                () -> service.getUploadStatus(1L, id)).getCode());
        assertTrue(Files.exists(sessionDir));
    }

    @Test
    void completeRejectsMissingChunkAndMergedByteMismatch() throws Exception {
        String missingId = UUID.randomUUID().toString();
        paths.createSessionDirectory(missingId);
        when(hashes.entries("upload:session:" + missingId)).thenReturn(session(missingId, 2L, 2));
        when(hashes.entries("upload:chunks:" + missingId)).thenReturn(Map.of());
        assertThrows(BusinessException.class, () -> service.completeUpload(1L, missingId));

        String mismatchId = UUID.randomUUID().toString();
        paths.createSessionDirectory(mismatchId);
        Path oneByte = Files.write(paths.resolveChunkFile(mismatchId, 0), new byte[]{1});
        when(hashes.entries("upload:session:" + mismatchId)).thenReturn(session(mismatchId, 2L, 1));
        when(hashes.entries("upload:chunks:" + mismatchId)).thenReturn(Map.of("0", oneByte.toString()));
        assertThrows(BusinessException.class, () -> service.completeUpload(1L, mismatchId));
    }

    @Test
    void schedulerCleansExpiredIndexEveryFifteenMinutes() throws Exception {
        String id = UUID.randomUUID().toString();
        Path dir = paths.createSessionDirectory(id);
        Files.writeString(paths.resolveChunkFile(id, 0), "x");
        when(redis.opsForZSet()).thenReturn(zsets);
        when(zsets.rangeByScore(eq("upload:expiry"), eq(0D), org.mockito.ArgumentMatchers.anyDouble()))
                .thenReturn(Set.of(id));
        when(hashes.entries("upload:session:" + id)).thenReturn(session(id, 1, 1));
        service.cleanupExpiredUploads();
        assertTrue(Files.notExists(dir));
        assertEquals("0 */15 * * * ?", ChunkedUploadServiceImpl.class
                .getMethod("cleanupExpiredUploads")
                .getAnnotation(org.springframework.scheduling.annotation.Scheduled.class).cron());
    }

    @Test
    void tosFailureRollsBackMergedAndAllowsSuccessfulRetry() throws Exception {
        String id = UUID.randomUUID().toString();
        byte[] png = pngBytes();
        paths.createSessionDirectory(id);
        Path chunk = Files.write(paths.resolveChunkFile(id, 0), png);
        Map<Object, Object> session = session(id, png.length, 1);
        when(hashes.entries("upload:session:" + id)).thenReturn(session);
        when(hashes.entries("upload:chunks:" + id)).thenReturn(Map.of("0", chunk.toString()));
        when(redis.opsForZSet()).thenReturn(zsets);
        when(locks.tryLockWithWatchdog(eq("upload:session-lock:" + id), eq(30L), eq(TimeUnit.SECONDS),
                eq(3L), eq(TimeUnit.SECONDS)))
                .thenReturn("lock");
        when(tos.uploadFileWithStyleAtObjectKey(any(),
                eq("covers/chunked/" + id + ".png"), eq(true)))
                .thenThrow(new IllegalStateException("secret TOS failure"))
                .thenReturn("https://example.com/retried.png");

        BusinessException first = assertThrows(BusinessException.class,
                () -> service.completeUpload(1L, id));
        assertFalse(first.getMessage().contains("secret TOS"));
        assertTrue(Files.notExists(paths.resolveMergedFile(id)));
        assertTrue(Files.exists(chunk));

        assertEquals("https://example.com/retried.png", service.completeUpload(1L, id));
    }

    @Test
    void freshCompletingSessionReturnsConflictButStaleSessionCanRecover() throws Exception {
        String freshId = UUID.randomUUID().toString();
        paths.createSessionDirectory(freshId);
        Map<Object, Object> fresh = session(freshId, 1, 1);
        fresh.put("status", "COMPLETING");
        fresh.put("stateUpdatedAt", String.valueOf(System.currentTimeMillis()));
        when(hashes.entries("upload:session:" + freshId)).thenReturn(fresh);

        assertEquals(409, assertThrows(BusinessException.class,
                () -> service.completeUpload(1L, freshId)).getCode());

        String staleId = UUID.randomUUID().toString();
        byte[] png = pngBytes();
        paths.createSessionDirectory(staleId);
        Path chunk = Files.write(paths.resolveChunkFile(staleId, 0), png);
        Map<Object, Object> stale = session(staleId, png.length, 1);
        stale.put("status", "COMPLETING");
        stale.put("stateUpdatedAt", String.valueOf(
                System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(11)));
        when(hashes.entries("upload:session:" + staleId)).thenReturn(stale);
        when(hashes.entries("upload:chunks:" + staleId)).thenReturn(Map.of("0", chunk.toString()));
        when(redis.opsForZSet()).thenReturn(zsets);
        when(tos.uploadFileWithStyleAtObjectKey(any(),
                eq("covers/chunked/" + staleId + ".png"), eq(true)))
                .thenReturn("https://example.com/stale.png");

        assertEquals("https://example.com/stale.png", service.completeUpload(1L, staleId));
    }

    @Test
    void tosSuccessAndCleanupFailureReturnsSameUrlAndRetryDoesNotCreateAnotherObject() throws Exception {
        String id = UUID.randomUUID().toString();
        byte[] png = pngBytes();
        Path marker = paths.createSessionDirectory(id);
        Path chunk = Files.write(paths.resolveChunkFile(id, 0), png);
        Map<Object, Object> mutableSession = session(id, png.length, 1);
        when(hashes.entries("upload:session:" + id)).thenReturn(mutableSession);
        when(hashes.entries("upload:chunks:" + id)).thenReturn(Map.of("0", chunk.toString()));
        lenient().when(redis.opsForZSet()).thenReturn(zsets);
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Map<Object, Object> updates = invocation.getArgument(1);
            mutableSession.putAll(updates);
            return null;
        }).when(hashes).putAll(eq("upload:session:" + id), anyMap());
        when(tos.uploadFileWithStyleAtObjectKey(any(),
                eq("covers/chunked/" + id + ".png"), eq(true))).thenAnswer(invocation -> {
            Files.delete(marker);
            Files.createDirectory(marker);
            Files.writeString(marker.resolve("child"), "prevent cleanup");
            return "https://example.com/stable.png";
        });

        assertEquals("https://example.com/stable.png", service.completeUpload(1L, id));
        assertEquals("UPLOADED", mutableSession.get("status"));
        assertEquals("https://example.com/stable.png", service.completeUpload(1L, id));

        verify(tos, times(1)).uploadFileWithStyleAtObjectKey(
                any(), eq("covers/chunked/" + id + ".png"), eq(true));
        verify(redis, never()).delete("upload:session:" + id);
        verify(zsets, never()).remove("upload:expiry", id);
    }

    @ParameterizedTest
    @ValueSource(strings = {"compare-delete", "session-delete", "expiry-remove"})
    void uploadedRetryReturnsPersistedUrlWhenRedisCleanupStepFails(String failingStep) throws Exception {
        String id = UUID.randomUUID().toString();
        Map<Object, Object> uploaded = session(id, 1, 1);
        uploaded.put("status", "UPLOADED");
        uploaded.put("completedUrl", "https://example.com/persisted.png");
        uploaded.put("hashKey", "upload:hash:1:sha256");
        when(hashes.entries("upload:session:" + id)).thenReturn(uploaded);
        lenient().when(redis.opsForZSet()).thenReturn(zsets);
        switch (failingStep) {
            case "compare-delete" -> when(redis.execute(
                    any(RedisScript.class), any(java.util.List.class), any()))
                    .thenThrow(new IllegalStateException("compare-delete unavailable"));
            case "session-delete" -> when(redis.delete("upload:session:" + id))
                    .thenThrow(new IllegalStateException("delete unavailable"));
            case "expiry-remove" -> when(zsets.remove("upload:expiry", id))
                    .thenThrow(new IllegalStateException("zset unavailable"));
            default -> throw new IllegalArgumentException(failingStep);
        }

        assertEquals("https://example.com/persisted.png", service.completeUpload(1L, id));
        verify(tos, never()).uploadFileWithStyleAtObjectKey(any(), anyString(), eq(true));
    }

    @Test
    void cleanupIoFailureRetainsRedisIndexesForRetry() throws Exception {
        String id = UUID.randomUUID().toString();
        Path marker = paths.createSessionDirectory(id);
        Files.delete(marker);
        Files.createDirectory(marker);
        Files.writeString(marker.resolve("child"), "block delete");
        when(redis.opsForZSet()).thenReturn(zsets);
        when(zsets.rangeByScore(eq("upload:expiry"), eq(0D), any(Double.class))).thenReturn(Set.of(id));
        when(hashes.entries("upload:session:" + id)).thenReturn(session(id, 1, 1));
        when(locks.tryLockWithWatchdog(eq("upload:session-lock:" + id), eq(30L), eq(TimeUnit.SECONDS),
                eq(3L), eq(TimeUnit.SECONDS)))
                .thenReturn("lock");

        service.cleanupExpiredUploads();

        verify(redis, never()).delete("upload:session:" + id);
        verify(redis, never()).delete("upload:chunks:" + id);
        verify(zsets, never()).remove("upload:expiry", id);
    }

    @Test
    void cancelFailsWithConflictWhileLifecycleLockIsHeld() throws Exception {
        String id = UUID.randomUUID().toString();
        paths.createSessionDirectory(id);
        when(hashes.entries("upload:session:" + id)).thenReturn(session(id, 1, 1));
        when(locks.tryLockWithWatchdog(eq("upload:session-lock:" + id), eq(30L), eq(TimeUnit.SECONDS),
                eq(3L), eq(TimeUnit.SECONDS)))
                .thenReturn(null);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.cancelUpload(1L, id));
        assertEquals(409, error.getCode());
        verify(tos, never()).uploadFileWithStyle(any(), anyString(), eq(true));
    }

    @Test
    void watchdogLifecycleLockPreventsCancelDuringLongRunningComplete() throws Exception {
        String id = UUID.randomUUID().toString();
        byte[] png = pngBytes();
        paths.createSessionDirectory(id);
        Path chunk = Files.write(paths.resolveChunkFile(id, 0), png);
        Map<Object, Object> session = session(id, png.length, 1);
        when(hashes.entries("upload:session:" + id)).thenReturn(session);
        when(hashes.entries("upload:chunks:" + id)).thenReturn(Map.of("0", chunk.toString()));
        when(redis.opsForZSet()).thenReturn(zsets);
        AtomicBoolean held = new AtomicBoolean();
        when(locks.tryLockWithWatchdog(eq("upload:session-lock:" + id), eq(30L), eq(TimeUnit.SECONDS),
                eq(3L), eq(TimeUnit.SECONDS)))
                .thenAnswer(invocation -> held.compareAndSet(false, true) ? "lock" : null);
        doAnswer(invocation -> {
            held.set(false);
            return null;
        }).when(locks).unlock(eq("upload:session-lock:" + id), eq("lock"));
        CountDownLatch tosEntered = new CountDownLatch(1);
        CountDownLatch releaseTos = new CountDownLatch(1);
        when(tos.uploadFileWithStyleAtObjectKey(any(),
                eq("covers/chunked/" + id + ".png"), eq(true))).thenAnswer(invocation -> {
            tosEntered.countDown();
            assertTrue(releaseTos.await(5, TimeUnit.SECONDS));
            return "https://example.com/complete.png";
        });

        try (var executor = Executors.newSingleThreadExecutor()) {
            var completing = executor.submit(() -> service.completeUpload(1L, id));
            assertTrue(tosEntered.await(5, TimeUnit.SECONDS));
            assertEquals(409, assertThrows(BusinessException.class,
                    () -> service.cancelUpload(1L, id)).getCode());
            releaseTos.countDown();
            assertEquals("https://example.com/complete.png", completing.get(5, TimeUnit.SECONDS));
        }
        verify(locks, times(2)).tryLockWithWatchdog(
                eq("upload:session-lock:" + id), eq(30L), eq(TimeUnit.SECONDS),
                eq(3L), eq(TimeUnit.SECONDS));
    }

    private static byte[] pngBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }

    private static Map<Object, Object> session(String id, long size, int chunks) {
        Map<Object, Object> session = new HashMap<>();
        session.put("uploadId", id);
        session.put("ownerUserId", "1");
        session.put("fileName", "cover.png");
        session.put("fileSize", String.valueOf(size));
        session.put("totalChunks", String.valueOf(chunks));
        session.put("uploadedChunks", "0");
        session.put("uploadedBytes", "0");
        session.put("status", "UPLOADING");
        session.put("expiresAt", String.valueOf(System.currentTimeMillis() + 60_000));
        return session;
    }
}
