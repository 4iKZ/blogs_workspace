package com.blog.service.impl;

import com.blog.service.ChunkedUploadService;
import com.blog.service.TOSService;
import com.blog.utils.RedisDistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * 分片上传服务实现类
 * 
 * 使用 Redis 存储上传会话状态，解决以下问题：
 * 1. 服务重启后会话信息不丢失
 * 2. 多实例部署时会话可共享
 * 3. 利用 Redis TTL 自动清理过期会话
 * 
 * Redis Key 设计：
 * - upload:session:{uploadId} - Hash 类型，存储会话元数据
 * - upload:chunks:{uploadId} - Hash 类型，存储分片索引到文件路径的映射
 * - upload:hash:{fileHash} - String 类型，存储文件哈希到 uploadId 的映射
 */
@Slf4j
@Service
public class ChunkedUploadServiceImpl implements ChunkedUploadService {

    @Autowired
    private TOSService tosService;

    @Autowired
    @Qualifier("uploadCleanupExecutor")
    private Executor uploadCleanupExecutor;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisDistributedLock redisDistributedLock;

    @Value("${upload.chunk.temp-dir:${java.io.tmpdir}/blog-uploads}")
    private String tempDir;

    @Value("${upload.chunk.expire-hours:24}")
    private int expireHours;

    private static final String SESSION_KEY_PREFIX = "upload:session:";
    private static final String CHUNKS_KEY_PREFIX = "upload:chunks:";
    private static final String HASH_KEY_PREFIX = "upload:hash:";

    @Override
    public String initUpload(String uploadId, String fileName, long fileSize, int totalChunks, String fileHash) {
        log.info("初始化分片上传: uploadId={}, fileName={}, fileSize={}, totalChunks={}",
                uploadId, fileName, fileSize, totalChunks);

        String sessionKey = SESSION_KEY_PREFIX + uploadId;
        String chunksKey = CHUNKS_KEY_PREFIX + uploadId;

        try {
            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("uploadId", uploadId);
            sessionData.put("fileName", fileName);
            sessionData.put("fileSize", String.valueOf(fileSize));
            sessionData.put("totalChunks", String.valueOf(totalChunks));
            sessionData.put("uploadedChunks", "0");
            sessionData.put("uploadedBytes", "0");
            sessionData.put("completed", "false");
            sessionData.put("createTime", String.valueOf(System.currentTimeMillis()));

            redisTemplate.opsForHash().putAll(sessionKey, sessionData);
            redisTemplate.expire(sessionKey, expireHours, TimeUnit.HOURS);

            if (fileHash != null && !fileHash.isEmpty()) {
                String hashKey = HASH_KEY_PREFIX + fileHash;
                redisTemplate.opsForValue().set(hashKey, uploadId, expireHours, TimeUnit.HOURS);
                log.debug("文件哈希映射已存储: fileHash={}, uploadId={}", fileHash, uploadId);
            }

            Path sessionDir = Paths.get(tempDir, uploadId);
            Files.createDirectories(sessionDir);
            log.debug("创建临时目录: {}", sessionDir);

            scheduleCleanup(uploadId);

            log.info("分片上传初始化成功: uploadId={}", uploadId);
            return uploadId;

        } catch (IOException e) {
            log.error("创建临时目录失败", e);
            redisTemplate.delete(sessionKey);
            throw new RuntimeException("初始化上传失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("初始化分片上传失败", e);
            redisTemplate.delete(sessionKey);
            throw new RuntimeException("初始化上传失败: " + e.getMessage());
        }
    }

    @Override
    public boolean uploadChunk(String uploadId, int chunkIndex, MultipartFile chunk) {
        log.debug("上传分片: uploadId={}, chunkIndex={}, size={}",
                uploadId, chunkIndex, chunk.getSize());

        String sessionKey = SESSION_KEY_PREFIX + uploadId;
        String chunksKey = CHUNKS_KEY_PREFIX + uploadId;
        String lockKey = "upload:chunk:" + uploadId + ":" + chunkIndex;

        Map<Object, Object> sessionData = redisTemplate.opsForHash().entries(sessionKey);
        if (sessionData.isEmpty()) {
            log.error("上传会话不存在: {}", uploadId);
            return false;
        }

        String completed = (String) sessionData.get("completed");
        if ("true".equals(completed)) {
            log.warn("上传已完成，忽略分片: uploadId={}, chunkIndex={}", uploadId, chunkIndex);
            return true;
        }

        String lockValue = redisDistributedLock.tryLock(lockKey, 1, TimeUnit.MINUTES);
        if (lockValue == null) {
            log.warn("分片上传正在处理: uploadId={}, chunkIndex={}", uploadId, chunkIndex);
            return false;
        }

        try {
            Object existing = redisTemplate.opsForHash().get(chunksKey, String.valueOf(chunkIndex));
            if (existing != null) {
                log.debug("分片已存在，跳过上传: uploadId={}, chunkIndex={}", uploadId, chunkIndex);
                return true;
            }

            Path sessionDir = Paths.get(tempDir, uploadId);
            Path chunkFile = sessionDir.resolve("chunk_" + chunkIndex);

            chunk.transferTo(chunkFile.toFile());

            redisTemplate.opsForHash().putIfAbsent(chunksKey, String.valueOf(chunkIndex), chunkFile.toString());
            redisTemplate.expire(chunksKey, expireHours, TimeUnit.HOURS);

            updateUploadProgress(uploadId, chunksKey, sessionData);

            log.debug("分片上传成功: uploadId={}, chunkIndex={}", uploadId, chunkIndex);
            return true;

        } catch (IOException e) {
            log.error("保存分片失败: uploadId={}, chunkIndex={}", uploadId, chunkIndex, e);
            return false;
        } finally {
            redisDistributedLock.unlock(lockKey, lockValue);
        }
    }

    /**
     * 更新上传进度
     */
    private void updateUploadProgress(String uploadId, String chunksKey, Map<Object, Object> sessionData) {
        try {
            Map<Object, Object> chunks = redisTemplate.opsForHash().entries(chunksKey);

            long uploadedBytes = 0;
            for (Object path : chunks.values()) {
                try {
                    uploadedBytes += Files.size(Paths.get(path.toString()));
                } catch (IOException e) {
                    log.warn("获取分片文件大小失败: {}", path, e);
                }
            }

            String sessionKey = SESSION_KEY_PREFIX + uploadId;
            redisTemplate.opsForHash().put(sessionKey, "uploadedChunks", String.valueOf(chunks.size()));
            redisTemplate.opsForHash().put(sessionKey, "uploadedBytes", String.valueOf(uploadedBytes));

            log.debug("分片上传进度: {}/{} ({} bytes)",
                    chunks.size(), sessionData.get("totalChunks"), uploadedBytes);
        } catch (Exception e) {
            log.warn("更新上传进度失败: uploadId={}", uploadId, e);
        }
    }

    @Override
    public String completeUpload(String uploadId) {
        log.info("完成分片上传: uploadId={}", uploadId);

        ChunkedUploadStatus status = getUploadStatus(uploadId);
        if (status == null) {
            log.error("上传会话不存在: {}", uploadId);
            throw new IllegalArgumentException("上传会话不存在");
        }

        try {
            File mergedFile = mergeChunks(uploadId, status);
            if (mergedFile == null) {
                throw new RuntimeException("合并分片失败");
            }

            String fileUrl = uploadToTOS(mergedFile, status.getFileName());

            String sessionKey = SESSION_KEY_PREFIX + uploadId;
            redisTemplate.opsForHash().put(sessionKey, "completed", "true");

            cleanupUpload(uploadId);

            log.info("分片上传完成: uploadId={}, url={}", uploadId, fileUrl);
            return fileUrl;

        } catch (Exception e) {
            log.error("完成上传失败: uploadId={}", uploadId, e);
            throw new RuntimeException("完成上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean cancelUpload(String uploadId) {
        log.info("取消分片上传: uploadId={}", uploadId);

        String sessionKey = SESSION_KEY_PREFIX + uploadId;
        Boolean exists = redisTemplate.hasKey(sessionKey);
        if (exists == null || !exists) {
            log.warn("上传会话不存在: {}", uploadId);
            return false;
        }

        cleanupUpload(uploadId);

        log.info("分片上传已取消: uploadId={}", uploadId);
        return true;
    }

    @Override
    public String checkResumeUpload(String fileHash) {
        String hashKey = HASH_KEY_PREFIX + fileHash;
        Object uploadId = redisTemplate.opsForValue().get(hashKey);

        if (uploadId != null) {
            ChunkedUploadStatus status = getUploadStatus(uploadId.toString());
            if (status != null && !status.isCompleted()) {
                log.info("发现可恢复的上传: uploadId={}, fileHash={}", uploadId, fileHash);
                return uploadId.toString();
            }
        }
        return null;
    }

    @Override
    public ChunkedUploadStatus getUploadStatus(String uploadId) {
        String sessionKey = SESSION_KEY_PREFIX + uploadId;
        Map<Object, Object> data = redisTemplate.opsForHash().entries(sessionKey);

        if (data.isEmpty()) {
            return null;
        }

        return rebuildStatus(data);
    }

    /**
     * 从 Redis 数据重建状态对象
     */
    private ChunkedUploadStatus rebuildStatus(Map<Object, Object> data) {
        try {
            String uploadId = (String) data.get("uploadId");
            String fileName = (String) data.get("fileName");
            long fileSize = Long.parseLong((String) data.get("fileSize"));
            int totalChunks = Integer.parseInt((String) data.get("totalChunks"));

            ChunkedUploadStatus status = new ChunkedUploadStatus(uploadId, fileName, fileSize, totalChunks);

            if (data.containsKey("uploadedChunks")) {
                status.setUploadedChunks(Integer.parseInt((String) data.get("uploadedChunks")));
            }
            if (data.containsKey("uploadedBytes")) {
                status.setUploadedBytes(Long.parseLong((String) data.get("uploadedBytes")));
            }
            if (data.containsKey("completed")) {
                status.setCompleted("true".equals(data.get("completed")));
            }

            return status;
        } catch (Exception e) {
            log.error("重建上传状态失败", e);
            return null;
        }
    }

    /**
     * 合并所有分片
     */
    private File mergeChunks(String uploadId, ChunkedUploadStatus status) throws IOException {
        log.info("开始合并分片: uploadId={}, totalChunks={}", uploadId, status.getTotalChunks());

        Path sessionDir = Paths.get(tempDir, uploadId);
        String chunksKey = CHUNKS_KEY_PREFIX + uploadId;

        Map<Object, Object> chunks = redisTemplate.opsForHash().entries(chunksKey);

        if (chunks.isEmpty() || chunks.size() < status.getTotalChunks()) {
            log.error("分片不完整: {}/{}", chunks.size(), status.getTotalChunks());
            throw new IOException("分片不完整");
        }

        Path mergedFile = sessionDir.resolve("merged_" + status.getFileName());

        try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(mergedFile))) {
            for (int i = 0; i < status.getTotalChunks(); i++) {
                String chunkPath = (String) chunks.get(String.valueOf(i));
                if (chunkPath == null) {
                    throw new IOException("缺少分片: " + i);
                }

                Path chunkFile = Paths.get(chunkPath);
                try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(chunkFile))) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
            }
        }

        log.info("分片合并完成: uploadId={}, mergedFile={}", uploadId, mergedFile);
        return mergedFile.toFile();
    }

    /**
     * 上传合并后的文件到TOS
     */
    private String uploadToTOS(File file, String fileName) throws IOException {
        MultipartFile multipartFile = new MultipartFile() {
            @Override
            public String getName() {
                return "file";
            }

            @Override
            public String getOriginalFilename() {
                return fileName;
            }

            @Override
            public String getContentType() {
                return "image/jpeg";
            }

            @Override
            public boolean isEmpty() {
                return file.length() == 0;
            }

            @Override
            public long getSize() {
                return file.length();
            }

            @Override
            public byte[] getBytes() throws IOException {
                return Files.readAllBytes(file.toPath());
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return new FileInputStream(file);
            }

            @Override
            public void transferTo(File dest) throws IOException, IllegalStateException {
                Files.copy(file.toPath(), dest.toPath());
            }
        };

        return tosService.uploadFileWithStyle(multipartFile, "covers", true);
    }

    /**
     * 清理上传会话的临时文件和 Redis 数据
     */
    private void cleanupUpload(String uploadId) {
        try {
            Path sessionDir = Paths.get(tempDir, uploadId);
            if (Files.exists(sessionDir)) {
                Files.walk(sessionDir)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                log.warn("删除文件失败: {}", path, e);
                            }
                        });
            }

            String sessionKey = SESSION_KEY_PREFIX + uploadId;
            String chunksKey = CHUNKS_KEY_PREFIX + uploadId;

            redisTemplate.delete(sessionKey);
            redisTemplate.delete(chunksKey);

            Set<String> hashKeys = redisTemplate.keys(HASH_KEY_PREFIX + "*");
            if (hashKeys != null) {
                for (String key : hashKeys) {
                    Object value = redisTemplate.opsForValue().get(key);
                    if (uploadId.equals(value)) {
                        redisTemplate.delete(key);
                        log.debug("删除文件哈希映射: key={}", key);
                    }
                }
            }

            log.debug("上传会话清理完成: uploadId={}", uploadId);

        } catch (IOException e) {
            log.warn("清理临时文件失败: uploadId={}", uploadId, e);
        }
    }

    /**
     * 定时清理过期会话（作为 Redis TTL 的补充）
     * 主要用于清理临时文件，Redis 数据会自动过期
     */
    private void scheduleCleanup(String uploadId) {
        uploadCleanupExecutor.execute(() -> {
            try {
                TimeUnit.HOURS.sleep(expireHours);

                ChunkedUploadStatus status = getUploadStatus(uploadId);
                if (status != null && !status.isCompleted()) {
                    log.info("清理过期上传会话: uploadId={}", uploadId);
                    cleanupUpload(uploadId);
                } else {
                    Path sessionDir = Paths.get(tempDir, uploadId);
                    if (Files.exists(sessionDir)) {
                        Files.walk(sessionDir)
                                .sorted((a, b) -> b.compareTo(a))
                                .forEach(path -> {
                                    try {
                                        Files.deleteIfExists(path);
                                    } catch (IOException e) {
                                        log.warn("删除过期临时文件失败: {}", path, e);
                                    }
                                });
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.warn("清理过期会话异常: uploadId={}", uploadId, e);
            }
        });
    }
}
