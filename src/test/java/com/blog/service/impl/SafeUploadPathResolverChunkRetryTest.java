package com.blog.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SafeUploadPathResolverChunkRetryTest {

    @TempDir
    Path tempDir;

    @Test
    void shortWrite_shouldCleanPartialChunkAndAllowRetry_withoutDeletingExistingChunk() throws Exception {
        FileSystem jimfs = com.google.common.jimfs.Jimfs
                .newFileSystem(com.google.common.jimfs.Configuration.unix());
        try {
            SafeUploadPathResolver resolver = new SafeUploadPathResolver(jimfs.getPath("/uploads"));
            String id = resolver.validateUploadId("550e8400-e29b-41d4-a716-446655440000");
            resolver.createSessionDirectory(id);

            // 场景 1：短写产生残文件，必须被清理，重试才能成功
            assertThatThrownBy(() -> resolver.writeChunk(id, 0,
                    new ByteArrayInputStream("too-long-content".getBytes()), 3))
                    .isInstanceOf(IOException.class);
            assertThat(Files.exists(resolver.resolveChunkFile(id, 0))).isFalse();

            byte[] content = "ok".getBytes();
            resolver.writeChunk(id, 0, new ByteArrayInputStream(content), content.length);
            assertThat(resolver.chunkSize(id, 0)).isEqualTo(content.length);

            // 场景 2：分片已存在时 CREATE_NEW 失败，不得误删已有分片
            byte[] original = "original".getBytes();
            resolver.writeChunk(id, 1, new ByteArrayInputStream(original), original.length);
            assertThatThrownBy(() -> resolver.writeChunk(id, 1,
                    new ByteArrayInputStream("x".getBytes()), 99))
                    .isInstanceOf(SecurityException.class);
            assertThat(resolver.chunkSize(id, 1)).isEqualTo(original.length);
        } finally {
            jimfs.close();
        }
    }
}
