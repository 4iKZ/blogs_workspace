package com.blog.service.impl;

import com.blog.config.ImageValidationProperties;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;

public record ValidatedImage(byte[] bytes, String mimeType, String extension) {
    static final int MAX_DIMENSION = 8192;
    static final long MAX_FRAME_PIXELS = 25_000_000L;
    static final int MAX_GIF_FRAMES = 100;
    static final long MAX_TOTAL_PIXELS = 50_000_000L;

    public static ValidatedImage from(MultipartFile file, long maxSize) {
        try {
            if (file == null || file.isEmpty() || file.getSize() > maxSize) {
                throw new IllegalArgumentException("图片大小无效");
            }
            return decode(file.getBytes(), null);
        } catch (IOException e) {
            throw new IllegalArgumentException("图片读取失败", e);
        }
    }

    public static ValidatedImage from(Path path, long maxSize) {
        try {
            long size = Files.size(path);
            if (size <= 0 || size > maxSize) {
                throw new IllegalArgumentException("图片大小无效");
            }
            return decode(Files.readAllBytes(path), null);
        } catch (IOException e) {
            throw new IllegalArgumentException("图片读取失败", e);
        }
    }

    public static ValidatedImage from(InputStream input, long maxSize) {
        try {
            byte[] bytes = input.readNBytes(Math.toIntExact(maxSize + 1));
            if (bytes.length == 0 || bytes.length > maxSize) {
                throw new IllegalArgumentException("图片大小无效");
            }
            return decode(bytes, null);
        } catch (IOException | ArithmeticException e) {
            throw new IllegalArgumentException("图片读取失败", e);
        }
    }

    public static ValidatedImage from(MultipartFile file, long maxSize, ImageValidationProperties limits) {
        try {
            if (file == null || file.isEmpty() || file.getSize() > maxSize) {
                throw new IllegalArgumentException("图片大小无效");
            }
            return decode(file.getBytes(), limits);
        } catch (IOException e) {
            throw new IllegalArgumentException("图片读取失败", e);
        }
    }

    public static ValidatedImage from(InputStream input, long maxSize, ImageValidationProperties limits) {
        try {
            byte[] bytes = input.readNBytes(Math.toIntExact(maxSize + 1));
            if (bytes.length == 0 || bytes.length > maxSize) {
                throw new IllegalArgumentException("图片大小无效");
            }
            return decode(bytes, limits);
        } catch (IOException | ArithmeticException e) {
            throw new IllegalArgumentException("图片读取失败", e);
        }
    }

    private static ValidatedImage decode(byte[] bytes, ImageValidationProperties limits) throws IOException {
        int maxDimension = limits == null ? MAX_DIMENSION : limits.getMaxDimension();
        long maxFramePixels = limits == null ? MAX_FRAME_PIXELS : limits.getMaxFramePixels();
        long maxTotalPixels = limits == null ? MAX_TOTAL_PIXELS : limits.getMaxTotalPixels();
        int maxGifFrames = limits == null ? MAX_GIF_FRAMES : limits.getMaxGifFrames();
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("图片内容无效");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, false, false);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                String mime;
                String extension;
                switch (format) {
                    case "jpeg", "jpg" -> { mime = "image/jpeg"; extension = ".jpg"; }
                    case "png" -> { mime = "image/png"; extension = ".png"; }
                    case "gif" -> { mime = "image/gif"; extension = ".gif"; }
                    default -> throw new IllegalArgumentException("仅支持JPEG/PNG/GIF");
                }
                int count = reader.getNumImages(true);
                if (count <= 0 || ("gif".equals(format) && count > maxGifFrames)) {
                    throw new IllegalArgumentException("图片帧数超限");
                }
                long totalPixels = 0;
                for (int i = 0; i < count; i++) {
                    int width = reader.getWidth(i);
                    int height = reader.getHeight(i);
                    if (width <= 0 || height <= 0 || width > maxDimension || height > maxDimension) {
                        throw new IllegalArgumentException("图片尺寸超限");
                    }
                    long pixels = (long) width * height;
                    totalPixels += pixels;
                    if (pixels > maxFramePixels || totalPixels > maxTotalPixels) {
                        throw new IllegalArgumentException("图片像素数超限");
                    }
                }
                for (int i = 0; i < count; i++) {
                    BufferedImage image = reader.read(i);
                    if (image == null) {
                        throw new IllegalArgumentException("图片解码失败");
                    }
                }
                return new ValidatedImage(bytes, mime, extension);
            } finally {
                reader.dispose();
            }
        }
    }

    public MultipartFile asMultipartFile() {
        return new MultipartFile() {
            @Override public String getName() { return "file"; }
            @Override public String getOriginalFilename() { return "validated" + extension; }
            @Override public String getContentType() { return mimeType; }
            @Override public boolean isEmpty() { return bytes.length == 0; }
            @Override public long getSize() { return bytes.length; }
            @Override public byte[] getBytes() { return bytes.clone(); }
            @Override public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }
            @Override public void transferTo(File dest) throws IOException { Files.write(dest.toPath(), bytes); }
        };
    }
}
