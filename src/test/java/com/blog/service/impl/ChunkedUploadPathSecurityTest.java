package com.blog.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ChunkedUploadPathSecurityTest {

    @TempDir
    Path windowsTemp;
    FileSystem fileSystem;
    Path tempDir;

    @BeforeEach
    void secureUnixFileSystem() throws IOException {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        tempDir = fileSystem.getPath("/uploads");
        Files.createDirectories(tempDir);
    }

    @AfterEach
    void closeFileSystem() throws IOException {
        fileSystem.close();
    }

    @Test
    void rejectsTraversalAbsoluteAndNonUuidUploadIds() {
        SafeUploadPathResolver resolver = new SafeUploadPathResolver(tempDir);

        assertThrows(IllegalArgumentException.class, () -> resolver.resolveSessionDirectory(".."));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolveSessionDirectory("/tmp/evil"));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolveSessionDirectory("C:\\temp\\evil"));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolveSessionDirectory("upload_123"));
    }

    @Test
    void rejectsUnsafeFileNamesAndUsesFixedMergedName() {
        SafeUploadPathResolver resolver = new SafeUploadPathResolver(tempDir);
        String uploadId = UUID.randomUUID().toString();

        for (String unsafe : new String[]{"../cover.jpg", "a/b.jpg", "a\\b.jpg", "/tmp/a.jpg",
                "C:\\tmp\\a.jpg", "cover..jpg", "bad\u0000.jpg"}) {
            assertThrows(IllegalArgumentException.class, () -> resolver.validateFileName(unsafe), unsafe);
        }
        assertEquals("cover.jpg", resolver.validateFileName("cover.jpg"));
        assertEquals(uploadId + ".merged", resolver.resolveMergedFile(uploadId).getFileName().toString());
    }

    @Test
    void initializationCreatesMissingDedicatedRootAndSupportsSecureOpen() throws IOException {
        Path missingRoot = tempDir.resolve("new").resolve("blog-uploads");
        String id = UUID.randomUUID().toString();

        SafeUploadPathResolver resolver = new SafeUploadPathResolver(missingRoot);
        Path marker = resolver.createSessionDirectory(id);

        assertTrue(Files.isDirectory(missingRoot));
        assertTrue(Files.exists(marker));
    }

    @Test
    void initializationRejectsPreexistingRootSymlinkAndPreservesExternalSentinel() throws IOException {
        Path actualRoot = Files.createDirectory(tempDir.resolve("init-actual-root"));
        Path sentinel = Files.writeString(actualRoot.resolve("sentinel.txt"), "keep");
        Path rootLink = tempDir.resolve("init-root-link");
        Files.createSymbolicLink(rootLink, actualRoot);

        assertThrows(IllegalStateException.class, () -> new SafeUploadPathResolver(rootLink));
        assertEquals("keep", Files.readString(sentinel));
    }

    @Test
    void cleanupDeletesOnlyUuidFilesAndPreservesSentinel() throws IOException {
        SafeUploadPathResolver resolver = new SafeUploadPathResolver(tempDir);
        String uploadId = UUID.randomUUID().toString();
        Path sentinel = Files.writeString(tempDir.resolve("sentinel.txt"), "keep");
        Path nearMatch = Files.writeString(
                tempDir.resolve(uploadId + ".chunk.00000000.backup"), "keep");
        Path session = resolver.createSessionDirectory(uploadId);
        resolver.writeChunk(uploadId, 0, new ByteArrayInputStream("data".getBytes()), 4);
        Path chunk = resolver.resolveChunkFile(uploadId, 0);

        resolver.deleteSessionDirectory(uploadId);

        assertTrue(Files.exists(sentinel));
        assertTrue(Files.exists(nearMatch));
        assertTrue(Files.notExists(session));
        assertTrue(Files.notExists(chunk));
        assertThrows(IllegalArgumentException.class, () -> resolver.deleteSessionDirectory(".."));
    }

    @Test
    void cleanupDeletesMarkerSymlinkWithoutFollowingIt() throws IOException {
        SafeUploadPathResolver resolver = new SafeUploadPathResolver(tempDir);
        String uploadId = UUID.randomUUID().toString();
        Path sentinel = Files.writeString(tempDir.resolve("outside.txt"), "keep");
        Path link = resolver.resolveSessionDirectory(uploadId);
        try {
            Files.createSymbolicLink(link, sentinel);
        } catch (UnsupportedOperationException | IOException e) {
            assumeTrue(false, "当前Windows环境不允许创建符号链接");
        }

        resolver.deleteSessionDirectory(uploadId);
        assertEquals("keep", Files.readString(sentinel));
        assertTrue(Files.notExists(link));
    }

    @Test
    void refusesSymlinkUploadRoot() throws IOException {
        Path actualRoot = Files.createDirectory(tempDir.resolve("actual-root"));
        Path rootLink = tempDir.resolve("root-link");
        try {
            Files.createSymbolicLink(rootLink, actualRoot);
        } catch (UnsupportedOperationException | IOException e) {
            assumeTrue(false, "当前Windows环境不允许创建符号链接");
        }
        assertThrows(IllegalStateException.class, () -> new SafeUploadPathResolver(rootLink));
    }

    @Test
    void deleteRefusesSymlinkRootAndPreservesExternalUuidSentinel() throws IOException {
        Path outside = Files.createDirectory(tempDir.resolve("delete-outside"));
        String id = UUID.randomUUID().toString();
        Path sentinel = Files.writeString(outside.resolve(id + ".session"), "keep");
        Path rootLink = tempDir.resolve("delete-root-link");
        try {
            Files.createSymbolicLink(rootLink, outside);
        } catch (UnsupportedOperationException | IOException e) {
            assumeTrue(false, "当前Windows环境不允许创建符号链接");
        }
        assertThrows(IllegalStateException.class, () -> new SafeUploadPathResolver(rootLink));
        assertTrue(Files.exists(sentinel));
    }

    @Test
    void atomicChunkOpenDoesNotFollowReplacementSymlink() throws IOException {
        SafeUploadPathResolver resolver = new SafeUploadPathResolver(tempDir);
        String id = UUID.randomUUID().toString();
        resolver.createSessionDirectory(id);
        Path outside = Files.writeString(tempDir.resolve("outside-target.txt"), "sentinel");
        Path chunk = resolver.resolveChunkFile(id, 0);
        try {
            Files.createSymbolicLink(chunk, outside);
        } catch (UnsupportedOperationException | IOException e) {
            assumeTrue(false, "当前Windows环境不允许创建符号链接");
        }
        assertThrows(SecurityException.class, () -> resolver.writeChunk(
                id, 0, new ByteArrayInputStream("attack".getBytes()), 6));
        assertEquals("sentinel", Files.readString(outside));
    }

    @Test
    void openMergedStreamRemainsBoundToOriginalRootHandleAfterPathReplacement() throws IOException {
        SafeUploadPathResolver resolver = new SafeUploadPathResolver(tempDir);
        String id = UUID.randomUUID().toString();
        resolver.createSessionDirectory(id);
        Path fileSystemRoot = tempDir.getRoot();
        Path movedRoot = fileSystemRoot.resolve("bound-uploads");
        Path outside = Files.createDirectory(fileSystemRoot.resolve("replacement-target"));
        Path sentinel = Files.writeString(outside.resolve("sentinel.txt"), "keep");

        try (OutputStream out = resolver.openMergedForWrite(id)) {
            Files.move(tempDir, movedRoot);
            Files.createSymbolicLink(tempDir, outside);
            out.write("bound".getBytes());
        }

        assertEquals("bound", Files.readString(movedRoot.resolve(id + ".merged")));
        assertEquals("keep", Files.readString(sentinel));
        assertTrue(Files.notExists(outside.resolve(id + ".merged")));
    }

    @Test
    void initializationCreatesMarkerInBoundRootDuringConcurrentRootReplacement() throws IOException {
        SafeUploadPathResolver resolver = new SafeUploadPathResolver(tempDir);
        String id = UUID.randomUUID().toString();
        Path fileSystemRoot = tempDir.getRoot();
        Path movedRoot = fileSystemRoot.resolve("init-bound-uploads");
        Path outside = Files.createDirectory(fileSystemRoot.resolve("init-replacement-target"));
        Path sentinel = Files.writeString(outside.resolve("sentinel.txt"), "keep");

        resolver.createSessionDirectory(id, () -> {
            try {
                Files.move(tempDir, movedRoot);
                Files.createSymbolicLink(tempDir, outside);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        assertTrue(Files.exists(movedRoot.resolve(id + ".session")));
        assertTrue(Files.notExists(outside.resolve(id + ".session")));
        assertEquals("keep", Files.readString(sentinel));
    }

    @Test
    void defaultWindowsProviderFailsClosedWithoutSecureDirectoryStream() throws IOException {
        SafeUploadPathResolver resolver = new SafeUploadPathResolver(windowsTemp);
        String newId = UUID.randomUUID().toString();
        assertThrows(SecurityException.class, () -> resolver.createSessionDirectory(newId));
        assertTrue(Files.notExists(windowsTemp.resolve(newId)));

        String id = UUID.randomUUID().toString();
        Path session = Files.write(windowsTemp.resolve(id + ".session"), new byte[0]);
        assertThrows(SecurityException.class, () -> resolver.writeChunk(
                id, 0, new ByteArrayInputStream(new byte[]{1}), 1));
        assertThrows(SecurityException.class, () -> resolver.openMergedForWrite(id));
        assertThrows(SecurityException.class, () -> resolver.deleteSessionDirectory(id));
        assertTrue(Files.exists(session));
    }
}
