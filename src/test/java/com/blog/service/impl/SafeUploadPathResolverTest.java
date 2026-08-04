package com.blog.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SafeUploadPathResolverTest {

    @TempDir
    Path tempDir;

    // ==================== 构造器与根目录校验 ====================

    @Test
    void constructor_nullRoot_shouldThrowIllegalState() {
        assertThatThrownBy(() -> new SafeUploadPathResolver(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("上传根目录配置不能为空");
    }

    @Test
    void constructor_normalDirectory_shouldSucceed() throws Exception {
        Path root = tempDir.resolve("uploads");
        SafeUploadPathResolver resolver = new SafeUploadPathResolver(root);

        assertThat(resolver.root()).isEqualTo(root.toRealPath());
    }

    @Test
    void constructor_rootIsFile_shouldThrowIllegalState() throws Exception {
        Path file = tempDir.resolve("afile");
        java.nio.file.Files.writeString(file, "x");

        assertThatThrownBy(() -> new SafeUploadPathResolver(file))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void constructor_filesystemRoot_shouldThrowIllegalState() {
        Path root = Path.of("/");

        assertThatThrownBy(() -> new SafeUploadPathResolver(root))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("专用子目录");
    }

    // ==================== validateUploadId ====================

    @Test
    void validateUploadId_validUuid_shouldNormalizeToLowercase() {
        SafeUploadPathResolver resolver = createResolver();

        String result = resolver.validateUploadId("550E8400-E29B-41D4-A716-446655440000");

        assertThat(result).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
    }

    @Test
    void validateUploadId_null_shouldThrowException() {
        SafeUploadPathResolver resolver = createResolver();

        assertThatThrownBy(() -> resolver.validateUploadId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("uploadId格式无效");
    }

    @Test
    void validateUploadId_invalidFormat_shouldThrowException() {
        SafeUploadPathResolver resolver = createResolver();

        assertThatThrownBy(() -> resolver.validateUploadId("not-a-uuid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("uploadId格式无效");
    }

    // ==================== validateFileName ====================

    @Test
    void validateFileName_normalName_shouldReturnSame() {
        SafeUploadPathResolver resolver = createResolver();

        String result = resolver.validateFileName("avatar.jpg");

        assertThat(result).isEqualTo("avatar.jpg");
    }

    @Test
    void validateFileName_null_shouldThrowException() {
        SafeUploadPathResolver resolver = createResolver();

        assertThatThrownBy(() -> resolver.validateFileName(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("文件名不安全");
    }

    @Test
    void validateFileName_blank_shouldThrowException() {
        SafeUploadPathResolver resolver = createResolver();

        assertThatThrownBy(() -> resolver.validateFileName("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("文件名不安全");
    }

    @Test
    void validateFileName_pathTraversal_shouldThrowException() {
        SafeUploadPathResolver resolver = createResolver();

        assertThatThrownBy(() -> resolver.validateFileName("..\\..\\etc\\passwd"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("文件名不安全");
    }

    @Test
    void validateFileName_absolutePath_shouldThrowException() {
        SafeUploadPathResolver resolver = createResolver();

        assertThatThrownBy(() -> resolver.validateFileName("/etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("文件名不安全");
    }

    @Test
    void validateFileName_controlChar_shouldThrowException() {
        SafeUploadPathResolver resolver = createResolver();

        assertThatThrownBy(() -> resolver.validateFileName("bad\u0001name.jpg"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("文件名包含控制字符");
    }

    // ==================== resolveChunkFile / resolveMergedFile
    // ====================

    @Test
    void resolveChunkFile_validInput_shouldReturnConfinedPath() throws Exception {
        SafeUploadPathResolver resolver = createResolver();

        Path chunkPath = resolver.resolveChunkFile("550e8400-e29b-41d4-a716-446655440000", 1);

        assertThat(chunkPath.getFileName().toString())
                .isEqualTo("550e8400-e29b-41d4-a716-446655440000.chunk.00000001");
    }

    @Test
    void resolveChunkFile_negativeIndex_shouldThrowException() {
        SafeUploadPathResolver resolver = createResolver();

        assertThatThrownBy(() -> resolver.resolveChunkFile("550e8400-e29b-41d4-a716-446655440000", -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("分片索引无效");
    }

    @Test
    void resolveMergedFile_validInput_shouldReturnConfinedPath() throws Exception {
        SafeUploadPathResolver resolver = createResolver();

        Path mergedPath = resolver.resolveMergedFile("550e8400-e29b-41d4-a716-446655440000");

        assertThat(mergedPath.getFileName().toString())
                .isEqualTo("550e8400-e29b-41d4-a716-446655440000.merged");
    }

    // ==================== createSessionDirectory / writeChunk / deleteChunk ====================

    @Test
    void writeChunkAndChunkSize_shouldRoundTrip() throws Exception {
        java.nio.file.FileSystem jimfs = com.google.common.jimfs.Jimfs.newFileSystem(com.google.common.jimfs.Configuration.unix());
        try {
            SafeUploadPathResolver resolver = new SafeUploadPathResolver(jimfs.getPath("/uploads"));
            String id = resolver.validateUploadId("550e8400-e29b-41d4-a716-446655440000");
            resolver.createSessionDirectory(id);
            byte[] content = "chunk-data".getBytes();

            resolver.writeChunk(id, 0, new java.io.ByteArrayInputStream(content), content.length);

            assertThat(resolver.chunkSize(id, 0)).isEqualTo(content.length);
            assertThat(java.nio.file.Files.exists(resolver.resolveChunkFile(id, 0))).isTrue();
        } finally {
            jimfs.close();
        }
    }

    @Test
    void writeChunk_sizeMismatch_shouldThrowSecurity() throws Exception {
        java.nio.file.FileSystem jimfs = com.google.common.jimfs.Jimfs.newFileSystem(com.google.common.jimfs.Configuration.unix());
        try {
            SafeUploadPathResolver resolver = new SafeUploadPathResolver(jimfs.getPath("/uploads"));
            String id = resolver.validateUploadId("550e8400-e29b-41d4-a716-446655440000");
            resolver.createSessionDirectory(id);

            assertThatThrownBy(() -> resolver.writeChunk(id, 0,
                    new java.io.ByteArrayInputStream("too-long-content".getBytes()), 3))
                    .isInstanceOf(java.io.IOException.class);
        } finally {
            jimfs.close();
        }
    }

    @Test
    void createSessionDirectory_duplicateMarker_shouldThrowSecurity() throws Exception {
        java.nio.file.FileSystem jimfs = com.google.common.jimfs.Jimfs.newFileSystem(com.google.common.jimfs.Configuration.unix());
        try {
            SafeUploadPathResolver resolver = new SafeUploadPathResolver(jimfs.getPath("/uploads"));
            String id = resolver.validateUploadId("550e8400-e29b-41d4-a716-446655440000");
            resolver.createSessionDirectory(id);

            assertThatThrownBy(() -> resolver.createSessionDirectory(id))
                    .isInstanceOf(SecurityException.class);
        } finally {
            jimfs.close();
        }
    }

    @Test
    void deleteChunk_shouldRemoveFile() throws Exception {
        java.nio.file.FileSystem jimfs = com.google.common.jimfs.Jimfs.newFileSystem(com.google.common.jimfs.Configuration.unix());
        try {
            SafeUploadPathResolver resolver = new SafeUploadPathResolver(jimfs.getPath("/uploads"));
            String id = resolver.validateUploadId("550e8400-e29b-41d4-a716-446655440000");
            resolver.createSessionDirectory(id);
            byte[] content = "chunk-data".getBytes();
            resolver.writeChunk(id, 0, new java.io.ByteArrayInputStream(content), content.length);

            resolver.deleteChunk(id, 0);

            assertThat(java.nio.file.Files.exists(resolver.resolveChunkFile(id, 0))).isFalse();
        } finally {
            jimfs.close();
        }
    }

    @Test
    void openMergedForWriteAndRead_shouldRoundTrip() throws Exception {
        java.nio.file.FileSystem jimfs = com.google.common.jimfs.Jimfs.newFileSystem(com.google.common.jimfs.Configuration.unix());
        try {
            SafeUploadPathResolver resolver = new SafeUploadPathResolver(jimfs.getPath("/uploads"));
            String id = resolver.validateUploadId("550e8400-e29b-41d4-a716-446655440000");
            resolver.createSessionDirectory(id);
            byte[] content = "merged-content".getBytes();

            try (java.io.OutputStream out = resolver.openMergedForWrite(id)) {
                out.write(content);
            }
            try (java.io.InputStream in = resolver.openMergedForRead(id)) {
                byte[] read = in.readAllBytes();
                assertThat(read).isEqualTo(content);
            }
        } finally {
            jimfs.close();
        }
    }

    @Test
    void deleteMerged_shouldRemoveFile() throws Exception {
        java.nio.file.FileSystem jimfs = com.google.common.jimfs.Jimfs.newFileSystem(com.google.common.jimfs.Configuration.unix());
        try {
            SafeUploadPathResolver resolver = new SafeUploadPathResolver(jimfs.getPath("/uploads"));
            String id = resolver.validateUploadId("550e8400-e29b-41d4-a716-446655440000");
            resolver.createSessionDirectory(id);
            try (java.io.OutputStream out = resolver.openMergedForWrite(id)) {
                out.write("x".getBytes());
            }

            resolver.deleteMerged(id);

            assertThat(java.nio.file.Files.exists(resolver.resolveMergedFile(id))).isFalse();
        } finally {
            jimfs.close();
        }
    }

    // ==================== helpers ====================

    private SafeUploadPathResolver createResolver() {
        try {
            return new SafeUploadPathResolver(tempDir.resolve("uploads"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
