package com.blog.service.impl;

import com.blog.common.ResultCode;
import com.blog.config.ImageValidationProperties;
import com.blog.exception.BusinessException;
import com.blog.service.ChunkedUploadService;
import com.blog.service.TOSService;
import com.blog.utils.RedisDistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Service
public class ChunkedUploadServiceImpl implements ChunkedUploadService {
    static final String SESSION_KEY_PREFIX = "upload:session:";
    static final String CHUNKS_KEY_PREFIX = "upload:chunks:";
    static final String HASH_KEY_PREFIX = "upload:hash:";
    static final String EXPIRY_KEY = "upload:expiry";
    private static final long LOCK_LEASE_SECONDS = 30;
    private static final long LOCK_WAIT_SECONDS = 3;
    private static final long COMPLETING_STALE_MILLIS = TimeUnit.MINUTES.toMillis(10);
    private static final DefaultRedisScript<Long> COMPARE_DELETE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private TOSService tosService;
    private RedisTemplate<String, Object> redisTemplate;
    private RedisDistributedLock redisDistributedLock;
    private SafeUploadPathResolver paths;
    private int expireHours;
    @Autowired(required = false)
    private ImageValidationProperties imageValidationProperties;

    public ChunkedUploadServiceImpl() {
    }

    @Autowired
    public ChunkedUploadServiceImpl(
            TOSService tosService,
            RedisTemplate<String, Object> redisTemplate,
            RedisDistributedLock redisDistributedLock,
            @Value("${upload.chunk.temp-dir:${java.io.tmpdir}/blog-uploads}") String tempDir,
            @Value("${upload.chunk.expire-hours:24}") int expireHours) {
        this(tosService, redisTemplate, redisDistributedLock, Path.of(tempDir), expireHours);
    }

    ChunkedUploadServiceImpl(
            TOSService tosService,
            RedisTemplate<String, Object> redisTemplate,
            RedisDistributedLock redisDistributedLock,
            Path tempDir,
            int expireHours) {
        this.tosService = tosService;
        this.redisTemplate = redisTemplate;
        this.redisDistributedLock = redisDistributedLock;
        this.paths = new SafeUploadPathResolver(tempDir);
        this.expireHours = expireHours;
    }

    @Override
    public UploadInitialization initUpload(Long userId, String fileName, long fileSize,
                                           int totalChunks, String fileHash) {
        requireUser(userId);
        String safeName = paths.validateFileName(fileName);
        if (fileSize <= 0 || fileSize > MAX_FILE_SIZE) {
            throw new BusinessException(ResultCode.FILE_SIZE_ERROR, "文件大小必须在1字节到10MiB之间");
        }
        int expectedChunks = (int) ((fileSize + CHUNK_SIZE - 1) / CHUNK_SIZE);
        if (totalChunks != expectedChunks) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "分片总数与文件大小不匹配");
        }
        String resumeHashKey = fileHash == null || fileHash.isBlank() ? null : hashKey(userId, fileHash);
        if (resumeHashKey != null) {
            return withWatchdogLock("upload:init:" + userId + ":" + fileHash,
                    () -> initUploadLocked(userId, safeName, fileSize, totalChunks, resumeHashKey));
        }
        return initUploadLocked(userId, safeName, fileSize, totalChunks, null);
    }

    private UploadInitialization initUploadLocked(Long userId, String safeName, long fileSize,
                                                   int totalChunks, String resumeHashKey) {
        if (resumeHashKey != null) {
            Object existingId = redisTemplate.opsForValue().get(resumeHashKey);
            if (existingId != null) {
                Map<Object, Object> existing = redisTemplate.opsForHash().entries(sessionKey(existingId.toString()));
                long existingExpiry = optionalLong(existing, "expiresAt");
                if (String.valueOf(userId).equals(String.valueOf(existing.get("ownerUserId")))
                        && existingExpiry > System.currentTimeMillis()) {
                    return new UploadInitialization(existingId.toString(), CHUNK_SIZE, MAX_FILE_SIZE, existingExpiry);
                }
                compareAndDelete(resumeHashKey, existingId.toString());
            }
        }
        String uploadId = UUID.randomUUID().toString();
        long expiresAt = Instant.now().plusSeconds(expireHours * 3600L).toEpochMilli();
        String sessionKey = sessionKey(uploadId);
        Map<String, Object> session = new HashMap<>();
        session.put("uploadId", uploadId);
        session.put("ownerUserId", String.valueOf(userId));
        session.put("fileName", safeName);
        session.put("fileSize", String.valueOf(fileSize));
        session.put("totalChunks", String.valueOf(totalChunks));
        session.put("uploadedChunks", "0");
        session.put("uploadedBytes", "0");
        session.put("detectedMime", "");
        session.put("status", "UPLOADING");
        session.put("stateUpdatedAt", String.valueOf(System.currentTimeMillis()));
        session.put("expiresAt", String.valueOf(expiresAt));
        if (resumeHashKey != null) {
            session.put("hashKey", resumeHashKey);
        }
        try {
            if (resumeHashKey != null) {
                Boolean claimed = redisTemplate.opsForValue()
                        .setIfAbsent(resumeHashKey, uploadId, expireHours, TimeUnit.HOURS);
                if (!Boolean.TRUE.equals(claimed)) {
                    Object winner = redisTemplate.opsForValue().get(resumeHashKey);
                    if (winner != null) {
                        Map<Object, Object> existing =
                                redisTemplate.opsForHash().entries(sessionKey(winner.toString()));
                        long existingExpiry = optionalLong(existing, "expiresAt");
                        if (String.valueOf(userId).equals(String.valueOf(existing.get("ownerUserId")))
                                && existingExpiry > System.currentTimeMillis()) {
                            return new UploadInitialization(
                                    winner.toString(), CHUNK_SIZE, MAX_FILE_SIZE, existingExpiry);
                        }
                    }
                    throw new BusinessException(ResultCode.CONFLICT, "相同文件正在初始化");
                }
            }
            paths.createSessionDirectory(uploadId);
            redisTemplate.opsForHash().putAll(sessionKey, session);
            if (!Boolean.TRUE.equals(redisTemplate.expire(
                    sessionKey, expireHours, TimeUnit.HOURS))) {
                throw new IllegalStateException("上传会话TTL写入失败");
            }
            if (!Boolean.TRUE.equals(redisTemplate.opsForZSet().add(
                    EXPIRY_KEY, uploadId, expiresAt))) {
                throw new IllegalStateException("上传过期索引写入失败");
            }
            return new UploadInitialization(uploadId, CHUNK_SIZE, MAX_FILE_SIZE, expiresAt);
        } catch (BusinessException e) {
            rollbackInitialization(uploadId, resumeHashKey);
            throw e;
        } catch (IOException | RuntimeException e) {
            rollbackInitialization(uploadId, resumeHashKey);
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR, "初始化上传失败");
        }
    }

    @Override
    public boolean uploadChunk(Long userId, String uploadId, int chunkIndex, MultipartFile chunk) {
        String id = paths.validateUploadId(uploadId);
        ownedSession(userId, id);
        return withSessionLock(id, () -> uploadChunkLocked(userId, id, chunkIndex, chunk));
    }

    private boolean uploadChunkLocked(Long userId, String uploadId, int chunkIndex, MultipartFile chunk) {
        Map<Object, Object> session = ownedSession(userId, uploadId);
        int totalChunks = integer(session, "totalChunks");
        long fileSize = number(session, "fileSize");
        if (!"UPLOADING".equals(session.get("status"))) {
            throw new BusinessException(ResultCode.CONFLICT, "上传会话状态无效");
        }
        if (chunkIndex < 0 || chunkIndex >= totalChunks) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "分片索引越界");
        }
        long expectedSize = chunkIndex == totalChunks - 1
                ? fileSize - (long) chunkIndex * CHUNK_SIZE : CHUNK_SIZE;
        if (chunk == null || chunk.getSize() != expectedSize) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "分片大小不正确");
        }
        String chunksKey = chunksKey(uploadId);
        String field = String.valueOf(chunkIndex);
        String lockKey = "upload:chunk:" + uploadId + ":" + chunkIndex;
        String lockValue = redisDistributedLock.tryLock(lockKey, 1, TimeUnit.MINUTES);
        if (lockValue == null) {
            throw new BusinessException(ResultCode.CONFLICT, "分片正在处理");
        }
        try {
            Object existing = redisTemplate.opsForHash().get(chunksKey, field);
            if (existing != null) {
                Path existingPath = storedPath(existing);
                if (!existingPath.equals(paths.resolveChunkFile(uploadId, chunkIndex))
                        || paths.chunkSize(uploadId, chunkIndex) != expectedSize) {
                    throw new SecurityException("已有分片状态无效");
                }
                return true;
            }
            Path destination = paths.resolveChunkFile(uploadId, chunkIndex);
            try (InputStream in = chunk.getInputStream()) {
                paths.writeChunk(uploadId, chunkIndex, in, expectedSize);
            }
            Boolean inserted = redisTemplate.opsForHash().putIfAbsent(chunksKey, field, destination.toString());
            if (Boolean.FALSE.equals(inserted)) {
                paths.deleteChunk(uploadId, chunkIndex);
                return true;
            }
            redisTemplate.expire(chunksKey, expireHours, TimeUnit.HOURS);
            Map<Object, Object> chunks = redisTemplate.opsForHash().entries(chunksKey);
            long uploadedBytes = 0;
            for (Map.Entry<Object, Object> entry : chunks.entrySet()) {
                int storedIndex = Integer.parseInt(String.valueOf(entry.getKey()));
                Path chunkPath = storedPath(entry.getValue());
                if (!chunkPath.equals(paths.resolveChunkFile(uploadId, storedIndex))) {
                    throw new SecurityException("分片路径越界");
                }
                uploadedBytes += paths.chunkSize(uploadId, storedIndex);
            }
            if (uploadedBytes > fileSize) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "累计上传大小越界");
            }
            redisTemplate.opsForHash().put(sessionKey(uploadId), "uploadedChunks", String.valueOf(chunks.size()));
            redisTemplate.opsForHash().put(sessionKey(uploadId), "uploadedBytes", String.valueOf(uploadedBytes));
            return true;
        } catch (IOException e) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR, "保存分片失败");
        } finally {
            redisDistributedLock.unlock(lockKey, lockValue);
        }
    }

    @Override
    public String completeUpload(Long userId, String uploadId) {
        String id = paths.validateUploadId(uploadId);
        ownedSession(userId, id);
        return withSessionLock(id, () -> completeUploadLocked(userId, id));
    }

    private String completeUploadLocked(Long userId, String uploadId) {
        Map<Object, Object> session = ownedSession(userId, uploadId);
        String status = String.valueOf(session.get("status"));
        if ("UPLOADED".equals(status)) {
            String completedUrl = String.valueOf(session.get("completedUrl"));
            try {
                cleanup(uploadId, session);
            } catch (RuntimeException cleanupError) {
                // 完成结果已持久化，清理只是可重试后置步骤，不能改变客户端结果。
                log.warn("已完成上传的后置清理失败，将由过期索引重试: {}", uploadId, cleanupError);
            }
            return completedUrl;
        }
        if ("COMPLETING".equals(status)) {
            long updatedAt = optionalLong(session, "stateUpdatedAt");
            if (updatedAt == 0 || System.currentTimeMillis() - updatedAt <= COMPLETING_STALE_MILLIS) {
                throw new BusinessException(ResultCode.CONFLICT, "上传会话正在处理");
            }
            try {
                paths.deleteMerged(uploadId);
            } catch (IOException | RuntimeException e) {
                throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR, "恢复上传会话失败");
            }
            updateState(uploadId, "UPLOADING");
            status = "UPLOADING";
        }
        if (!"UPLOADING".equals(status)) {
            throw new BusinessException(ResultCode.CONFLICT, "上传会话正在处理");
        }
        int totalChunks = integer(session, "totalChunks");
        long fileSize = number(session, "fileSize");
        Map<Object, Object> chunks = redisTemplate.opsForHash().entries(chunksKey(uploadId));
        if (chunks.size() != totalChunks) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "分片不完整");
        }
        updateState(uploadId, "COMPLETING");
        boolean uploaded = false;
        String completedUrl = null;
        try {
            paths.deleteMerged(uploadId);
            try (OutputStream out = new BufferedOutputStream(paths.openMergedForWrite(uploadId))) {
                for (int i = 0; i < totalChunks; i++) {
                    Object stored = chunks.get(String.valueOf(i));
                    if (stored == null) {
                        throw new BusinessException(ResultCode.BAD_REQUEST, "缺少分片");
                    }
                    Path chunk = storedPath(stored);
                    if (!chunk.equals(paths.resolveChunkFile(uploadId, i))) {
                        throw new SecurityException("分片路径无效");
                    }
                    try (InputStream in = new BufferedInputStream(paths.openChunkForRead(uploadId, i))) {
                        in.transferTo(out);
                    }
                }
            }
            try (InputStream mergedInput = new BufferedInputStream(paths.openMergedForRead(uploadId))) {
                ValidatedImage validated;
                try {
                    validated = ValidatedImage.from(mergedInput, MAX_FILE_SIZE, imageValidationProperties);
                } catch (IllegalArgumentException e) {
                    throw new BusinessException(ResultCode.FILE_TYPE_ERROR, "图片内容无效");
                }
                if (validated.bytes().length != fileSize) {
                    throw new BusinessException(ResultCode.BAD_REQUEST, "合并文件大小不一致");
                }
                redisTemplate.opsForHash().put(sessionKey(uploadId), "detectedMime", validated.mimeType());
                String objectKey = String.valueOf(session.getOrDefault(
                        "objectKey", "covers/chunked/" + uploadId + validated.extension()));
                redisTemplate.opsForHash().put(sessionKey(uploadId), "objectKey", objectKey);
                completedUrl = tosService.uploadFileWithStyleAtObjectKey(
                        validated.asMultipartFile(), objectKey, true);
                uploaded = true;
                Map<String, Object> completed = new HashMap<>();
                completed.put("status", "UPLOADED");
                completed.put("completedUrl", completedUrl);
                completed.put("stateUpdatedAt", String.valueOf(System.currentTimeMillis()));
                try {
                    redisTemplate.opsForHash().putAll(sessionKey(uploadId), completed);
                } catch (RuntimeException persistenceError) {
                    log.error("TOS上传成功但完成状态持久化失败，将按稳定对象Key重试: {}", uploadId, persistenceError);
                    try {
                        redisTemplate.opsForHash().put(sessionKey(uploadId), "completedUrl", completedUrl);
                        redisTemplate.opsForHash().put(sessionKey(uploadId), "stateUpdatedAt",
                                String.valueOf(System.currentTimeMillis()));
                        redisTemplate.opsForHash().put(sessionKey(uploadId), "status", "UPLOADED");
                    } catch (RuntimeException compensationError) {
                        log.error("记录TOS上传补偿状态失败，稳定对象Key仍可安全覆盖: {}", uploadId, compensationError);
                    }
                    return completedUrl;
                }
                cleanup(uploadId, session);
                return completedUrl;
            }
        } catch (IOException | RuntimeException e) {
            if (uploaded) {
                log.error("TOS上传后处理失败，保留稳定对象供重试: {}", uploadId, e);
                return completedUrl;
            }
            try {
                paths.deleteMerged(uploadId);
            } catch (IOException | RuntimeException cleanupError) {
                log.warn("回滚合并文件失败: {}", uploadId, cleanupError);
            }
            updateState(uploadId, "UPLOADING");
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR, "完成上传失败，请重试");
        }
    }

    @Override
    public boolean cancelUpload(Long userId, String uploadId) {
        String id = paths.validateUploadId(uploadId);
        ownedSession(userId, id);
        return withSessionLock(id, () -> {
            Map<Object, Object> session = ownedSession(userId, id);
            if (!cleanup(id, session)) {
                throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR, "取消上传失败，请重试");
            }
            return true;
        });
    }

    @Override
    public String checkResumeUpload(Long userId, String fileHash) {
        requireUser(userId);
        if (fileHash == null || fileHash.isBlank()) {
            return null;
        }
        Object uploadId = redisTemplate.opsForValue().get(hashKey(userId, fileHash));
        if (uploadId == null) {
            return null;
        }
        try {
            ownedSession(userId, uploadId.toString());
            return uploadId.toString();
        } catch (BusinessException e) {
            return null;
        }
    }

    @Override
    public ChunkedUploadStatus getUploadStatus(Long userId, String uploadId) {
        Map<Object, Object> data = ownedSession(userId, uploadId);
        ChunkedUploadStatus status = new ChunkedUploadStatus(uploadId, String.valueOf(data.get("fileName")),
                number(data, "fileSize"), integer(data, "totalChunks"));
        status.setUploadedChunks(optionalInt(data, "uploadedChunks"));
        status.setUploadedBytes(optionalLong(data, "uploadedBytes"));
        status.setCompleted("UPLOADED".equals(data.get("status")) || "COMPLETED".equals(data.get("status")));
        status.setDetectedMime(String.valueOf(data.getOrDefault("detectedMime", "")));
        status.setUploadedIndices(redisTemplate.opsForHash().entries(chunksKey(uploadId)).keySet().stream()
                .map(String::valueOf).map(Integer::valueOf).collect(Collectors.toSet()));
        return status;
    }

    @Scheduled(cron = "0 */15 * * * ?")
    public void cleanupExpiredUploads() {
        long now = System.currentTimeMillis();
        Set<Object> expired = redisTemplate.opsForZSet().rangeByScore(EXPIRY_KEY, 0, now);
        if (expired == null) {
            return;
        }
        for (Object id : expired) {
            String uploadId = String.valueOf(id);
            try {
                withSessionLock(uploadId, () -> {
                    Map<Object, Object> session = redisTemplate.opsForHash().entries(sessionKey(uploadId));
                    cleanup(uploadId, session);
                    return null;
                });
            } catch (RuntimeException e) {
                log.warn("清理过期上传失败: {}", uploadId, e);
            }
        }
    }

    private Map<Object, Object> ownedSession(Long userId, String uploadId) {
        requireUser(userId);
        uploadId = paths.validateUploadId(uploadId);
        Map<Object, Object> session = redisTemplate.opsForHash().entries(sessionKey(uploadId));
        if (session.isEmpty() || !String.valueOf(userId).equals(String.valueOf(session.get("ownerUserId")))) {
            throw new BusinessException(ResultCode.NOT_FOUND, "上传会话不存在");
        }
        long expiresAt = optionalLong(session, "expiresAt");
        if (expiresAt > 0 && expiresAt <= System.currentTimeMillis()) {
            throw new BusinessException(ResultCode.NOT_FOUND, "上传会话不存在");
        }
        return session;
    }

    private boolean cleanup(String uploadId, Map<Object, Object> session) {
        try {
            paths.deleteSessionDirectory(uploadId);
        } catch (IOException | RuntimeException e) {
            log.warn("删除上传目录失败: {}", uploadId, e);
            return false;
        }
        Object hashKey = session.get("hashKey");
        if (hashKey != null) {
            compareAndDelete(hashKey.toString(), uploadId);
        }
        redisTemplate.delete(sessionKey(uploadId));
        redisTemplate.delete(chunksKey(uploadId));
        redisTemplate.opsForZSet().remove(EXPIRY_KEY, uploadId);
        return true;
    }

    private void rollbackInitialization(String uploadId, String resumeHashKey) {
        try {
            paths.deleteSessionDirectory(uploadId);
        } catch (IOException | RuntimeException e) {
            log.warn("回滚上传marker失败: {}", uploadId, e);
        }
        try {
            redisTemplate.delete(sessionKey(uploadId));
        } catch (RuntimeException e) {
            log.warn("回滚上传session失败: {}", uploadId, e);
        }
        try {
            redisTemplate.delete(chunksKey(uploadId));
        } catch (RuntimeException e) {
            log.warn("回滚上传chunks失败: {}", uploadId, e);
        }
        try {
            if (resumeHashKey != null) {
                compareAndDelete(resumeHashKey, uploadId);
            }
        } catch (RuntimeException e) {
            log.warn("回滚上传hash失败: {}", uploadId, e);
        }
        try {
            if (redisTemplate.opsForZSet() != null) {
                redisTemplate.opsForZSet().remove(EXPIRY_KEY, uploadId);
            }
        } catch (RuntimeException e) {
            log.warn("回滚上传过期索引失败: {}", uploadId, e);
        }
    }

    private <T> T withSessionLock(String uploadId, Supplier<T> action) {
        // 分片写入和完成/取消/清理刻意按会话串行：牺牲同会话吞吐以保证本地文件与Redis状态一致。
        String lockKey = "upload:session-lock:" + paths.validateUploadId(uploadId);
        return withWatchdogLock(lockKey, action);
    }

    private <T> T withWatchdogLock(String lockKey, Supplier<T> action) {
        String lockValue = redisDistributedLock.tryLockWithWatchdog(
                lockKey, LOCK_LEASE_SECONDS, TimeUnit.SECONDS, LOCK_WAIT_SECONDS, TimeUnit.SECONDS);
        if (lockValue == null) {
            throw new BusinessException(ResultCode.CONFLICT, "上传会话正在处理");
        }
        try {
            return action.get();
        } finally {
            try {
                redisDistributedLock.unlock(lockKey, lockValue);
            } catch (RuntimeException e) {
                log.error("释放上传锁失败: {}", lockKey, e);
            }
        }
    }

    private void updateState(String uploadId, String status) {
        Map<String, Object> values = new HashMap<>();
        values.put("status", status);
        values.put("stateUpdatedAt", String.valueOf(System.currentTimeMillis()));
        redisTemplate.opsForHash().putAll(sessionKey(uploadId), values);
    }

    private void compareAndDelete(String key, String expectedValue) {
        redisTemplate.execute(COMPARE_DELETE_SCRIPT, java.util.List.of(key), expectedValue);
    }

    private static void requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录");
        }
    }

    private static long number(Map<Object, Object> data, String key) {
        return Long.parseLong(String.valueOf(data.get(key)));
    }

    private static int integer(Map<Object, Object> data, String key) {
        return Integer.parseInt(String.valueOf(data.get(key)));
    }

    private static long optionalLong(Map<Object, Object> data, String key) {
        Object value = data.get(key);
        return value == null || value.toString().isBlank() ? 0 : Long.parseLong(value.toString());
    }

    private static int optionalInt(Map<Object, Object> data, String key) {
        return (int) optionalLong(data, key);
    }

    private static String sessionKey(String uploadId) {
        return SESSION_KEY_PREFIX + uploadId;
    }

    private static String chunksKey(String uploadId) {
        return CHUNKS_KEY_PREFIX + uploadId;
    }

    private static String hashKey(Long userId, String hash) {
        return HASH_KEY_PREFIX + userId + ":" + hash;
    }

    private Path storedPath(Object value) {
        return paths.root().getFileSystem().getPath(String.valueOf(value)).toAbsolutePath().normalize();
    }

}
