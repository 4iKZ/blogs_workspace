package com.blog.service.impl;

import java.io.IOException;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;
import java.util.regex.Pattern;

public final class SafeUploadPathResolver {
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$");
    private final Path root;

    public SafeUploadPathResolver(Path root) {
        if (root == null) {
            throw new IllegalStateException("上传根目录配置不能为空");
        }
        Path configured = root.toAbsolutePath().normalize();
        if (configured.getParent() == null) {
            throw new IllegalStateException("上传根目录必须是专用子目录: " + configured);
        }
        try {
            Files.createDirectories(configured);
            if (Files.isSymbolicLink(configured)) {
                throw new IllegalStateException("上传根目录不能是符号链接: " + configured);
            }
            Path initialized = configured.toRealPath(LinkOption.NOFOLLOW_LINKS);
            if (Files.isSymbolicLink(initialized)
                    || !Files.isDirectory(initialized, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalStateException("上传根目录不是安全目录: " + configured);
            }
            this.root = initialized;
        } catch (IOException e) {
            throw new IllegalStateException("无法初始化上传根目录: " + configured, e);
        }
    }

    public Path root() {
        return root;
    }

    public String validateUploadId(String uploadId) {
        if (uploadId == null || !UUID_PATTERN.matcher(uploadId).matches()) {
            throw new IllegalArgumentException("uploadId格式无效");
        }
        return uploadId.toLowerCase();
    }

    public String validateFileName(String fileName) {
        if (fileName == null || fileName.isBlank() || fileName.contains("..")
                || fileName.indexOf('/') >= 0 || fileName.indexOf('\\') >= 0) {
            throw new IllegalArgumentException("文件名不安全");
        }
        for (int i = 0; i < fileName.length(); i++) {
            if (Character.isISOControl(fileName.charAt(i))) {
                throw new IllegalArgumentException("文件名包含控制字符");
            }
        }
        Path candidate;
        try {
            candidate = Path.of(fileName);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("文件名无效", e);
        }
        if (candidate.isAbsolute() || !candidate.getFileName().toString().equals(fileName)) {
            throw new IllegalArgumentException("文件名不安全");
        }
        return fileName;
    }

    public Path resolveSessionDirectory(String uploadId) {
        Path resolved = root.resolve(markerName(uploadId)).normalize();
        if (!resolved.startsWith(root) || resolved.equals(root)) {
            throw new SecurityException("上传会话路径越界");
        }
        return resolved;
    }

    public Path createSessionDirectory(String uploadId) throws IOException {
        return createSessionDirectory(uploadId, () -> { });
    }

    Path createSessionDirectory(String uploadId, Runnable afterRootOpened) throws IOException {
        String marker = markerName(uploadId);
        try (RootHandles handles = openRoot()) {
            afterRootOpened.run();
            try (SeekableByteChannel ignored = handles.root.newByteChannel(
                    relative(marker),
                    Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE,
                            LinkOption.NOFOLLOW_LINKS))) {
                // The marker is the logical session and is created relative to the bound root handle.
            }
        } catch (java.nio.file.FileSystemException e) {
            throw new SecurityException("拒绝创建不安全的上传会话", e);
        }
        return resolveSessionDirectory(uploadId);
    }

    public Path resolveChunkFile(String uploadId, int chunkIndex) {
        if (chunkIndex < 0) {
            throw new IllegalArgumentException("分片索引无效");
        }
        return confine(root.resolve(chunkName(uploadId, chunkIndex)));
    }

    public Path resolveMergedFile(String uploadId) {
        return confine(root.resolve(mergedName(uploadId)));
    }

    public void deleteSessionDirectory(String uploadId) throws IOException {
        String id = validateUploadId(uploadId);
        String marker = markerName(id);
        String merged = mergedName(id);
        Pattern chunkPattern = Pattern.compile(Pattern.quote(id) + "\\.chunk\\.[0-9]{8}");
        try (RootHandles handles = openRoot()) {
            for (Path entry : handles.root) {
                String name = entry.getFileName().toString();
                if (name.equals(merged) || chunkPattern.matcher(name).matches()) {
                    handles.root.deleteFile(relative(name));
                }
            }
            handles.root.deleteFile(relative(marker));
        } catch (java.nio.file.NoSuchFileException e) {
            return;
        }
    }

    public void writeChunk(String uploadId, int index, InputStream source, long expectedSize) throws IOException {
        try (RootHandles handles = openSession(uploadId);
             SeekableByteChannel targetHandle = handles.root.newByteChannel(relative(
                     chunkName(uploadId, index)),
                     Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS));
             OutputStream out = Channels.newOutputStream(targetHandle)) {
            long written = source.transferTo(out);
            if (written != expectedSize) {
                throw new IOException("分片字节数变化");
            }
        } catch (java.nio.file.FileSystemException e) {
            throw new SecurityException("拒绝跟随上传路径链接", e);
        }
    }

    public InputStream openChunkForRead(String uploadId, int index) throws IOException {
        RootHandles handles = openSession(uploadId);
        try {
            SeekableByteChannel file = handles.root.newByteChannel(relative(
                    chunkName(uploadId, index)),
                    Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS));
            return new FilterInputStream(Channels.newInputStream(file)) {
                @Override public void close() throws IOException {
                    try { super.close(); } finally { handles.close(); }
                }
            };
        } catch (IOException | RuntimeException e) {
            handles.close();
            throw e;
        }
    }

    public long chunkSize(String uploadId, int index) throws IOException {
        try (RootHandles handles = openSession(uploadId)) {
            return attributes(handles.root, chunkName(uploadId, index)).size();
        }
    }

    public void deleteChunk(String uploadId, int index) throws IOException {
        try (RootHandles handles = openSession(uploadId)) {
            handles.root.deleteFile(relative(chunkName(uploadId, index)));
        }
    }

    public OutputStream openMergedForWrite(String uploadId) throws IOException {
        RootHandles handles = openSession(uploadId);
        try {
            SeekableByteChannel file = handles.root.newByteChannel(relative(mergedName(uploadId)),
                    Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS));
            return new FilterOutputStream(Channels.newOutputStream(file)) {
                @Override public void close() throws IOException {
                    try { super.close(); } finally { handles.close(); }
                }
            };
        } catch (IOException | RuntimeException e) {
            handles.close();
            throw e;
        }
    }

    public InputStream openMergedForRead(String uploadId) throws IOException {
        RootHandles handles = openSession(uploadId);
        try {
            SeekableByteChannel file = handles.root.newByteChannel(relative(mergedName(uploadId)),
                    Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS));
            return new FilterInputStream(Channels.newInputStream(file)) {
                @Override public void close() throws IOException {
                    try { super.close(); } finally { handles.close(); }
                }
            };
        } catch (IOException | RuntimeException e) {
            handles.close();
            throw e;
        }
    }

    public void deleteMerged(String uploadId) throws IOException {
        try (RootHandles handles = openSession(uploadId)) {
            try {
                handles.root.deleteFile(relative(mergedName(uploadId)));
            } catch (java.nio.file.NoSuchFileException ignored) {
                // Retry cleanup is idempotent.
            }
        }
    }

    private RootHandles openRoot() throws IOException {
        Path parentPath = root.getParent();
        DirectoryStream<Path> parentStream = Files.newDirectoryStream(parentPath);
        if (!(parentStream instanceof SecureDirectoryStream<Path> parent)) {
            parentStream.close();
            throw new SecurityException("文件系统不支持SecureDirectoryStream，拒绝操作");
        }
        try {
            SecureDirectoryStream<Path> rootStream =
                    parent.newDirectoryStream(root.getFileName(), LinkOption.NOFOLLOW_LINKS);
            return new RootHandles(parent, rootStream);
        } catch (NotDirectoryException e) {
            parent.close();
            throw new SecurityException("上传根目录不是安全目录", e);
        } catch (IOException | RuntimeException e) {
            parent.close();
            throw e;
        }
    }

    private RootHandles openSession(String uploadId) throws IOException {
        RootHandles rootHandles = openRoot();
        try {
            attributes(rootHandles.root, markerName(uploadId));
            return rootHandles;
        } catch (IOException | RuntimeException e) {
            rootHandles.close();
            throw e;
        }
    }

    private BasicFileAttributes attributes(SecureDirectoryStream<Path> directory, String name)
            throws IOException {
        BasicFileAttributeView view = directory.getFileAttributeView(
                relative(name), BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        BasicFileAttributes attributes = view.readAttributes();
        if (!attributes.isRegularFile() || attributes.isSymbolicLink()) {
            throw new SecurityException("上传分片不是普通文件");
        }
        return attributes;
    }

    private Path relative(String value) {
        return root.getFileSystem().getPath(value);
    }

    private String markerName(String uploadId) {
        return validateUploadId(uploadId) + ".session";
    }

    private String chunkName(String uploadId, int index) {
        if (index < 0) {
            throw new IllegalArgumentException("分片索引无效");
        }
        return validateUploadId(uploadId) + String.format(".chunk.%08d", index);
    }

    private String mergedName(String uploadId) {
        return validateUploadId(uploadId) + ".merged";
    }

    private record RootHandles(SecureDirectoryStream<Path> parent,
                               SecureDirectoryStream<Path> root) implements AutoCloseable {
        @Override public void close() throws IOException {
            try { root.close(); } finally { parent.close(); }
        }
    }

    private Path confine(Path candidate) {
        Path normalized = candidate.toAbsolutePath().normalize();
        if (!normalized.startsWith(root)) {
            throw new SecurityException("上传路径越界");
        }
        return normalized;
    }
}
