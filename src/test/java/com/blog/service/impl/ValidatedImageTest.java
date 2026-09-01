package com.blog.service.impl;

import com.blog.config.ImageValidationProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ValidatedImageTest {
    @ParameterizedTest
    @ValueSource(strings = {"jpeg", "png", "gif"})
    void detectsMimeAndSafeExtensionFromDecodedContent(String format) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), format, bytes);
        ValidatedImage image = ValidatedImage.from(
                new MockMultipartFile("file", "evil.webp", "image/webp", bytes.toByteArray()), 10_485_760);

        String expectedMime = format.equals("jpeg") ? "image/jpeg" : "image/" + format;
        assertEquals(expectedMime, image.mimeType());
        assertEquals(format.equals("jpeg") ? ".jpg" : "." + format, image.extension());
    }

    @Test
    void rejectsPseudoImageEvenWhenClaimedAsPng() {
        assertThrows(IllegalArgumentException.class, () -> ValidatedImage.from(
                new MockMultipartFile("file", "a.png", "image/png", "not-image".getBytes()), 10_485_760));
    }

    @Test
    void rejectsOversizedDimensionsBeforePixelDecode() throws Exception {
        ImageInputStream stream = mock(ImageInputStream.class);
        ImageReader reader = mock(ImageReader.class);
        try (var imageIo = mockStatic(ImageIO.class)) {
            imageIo.when(() -> ImageIO.createImageInputStream(org.mockito.ArgumentMatchers.any()))
                    .thenReturn(stream);
            imageIo.when(() -> ImageIO.getImageReaders(stream)).thenReturn(List.of(reader).iterator());
            when(reader.getFormatName()).thenReturn("png");
            when(reader.getNumImages(true)).thenReturn(1);
            when(reader.getWidth(0)).thenReturn(8193);
            when(reader.getHeight(0)).thenReturn(1);

            assertThrows(IllegalArgumentException.class,
                    () -> ValidatedImage.from(new java.io.ByteArrayInputStream(new byte[]{1}), 10));
            verify(reader, never()).read(0);
        }
    }

    @Test
    void rejectsTooManyGifFramesBeforePixelDecode() throws Exception {
        ImageInputStream stream = mock(ImageInputStream.class);
        ImageReader reader = mock(ImageReader.class);
        try (var imageIo = mockStatic(ImageIO.class)) {
            imageIo.when(() -> ImageIO.createImageInputStream(org.mockito.ArgumentMatchers.any()))
                    .thenReturn(stream);
            imageIo.when(() -> ImageIO.getImageReaders(stream)).thenReturn(List.of(reader).iterator());
            when(reader.getFormatName()).thenReturn("gif");
            when(reader.getNumImages(true)).thenReturn(101);

            assertThrows(IllegalArgumentException.class,
                    () -> ValidatedImage.from(new java.io.ByteArrayInputStream(new byte[]{1}), 10));
            verify(reader, never()).read(org.mockito.ArgumentMatchers.anyInt());
        }
    }

    // ==================== Path 工厂方法 ====================

    @Test
    void fromPath_validImage_shouldSucceed() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        Path tmp = Files.createTempFile("img", ".png");
        Files.write(tmp, bytes.toByteArray());

        ValidatedImage image = ValidatedImage.from(tmp, 10_485_760);

        assertEquals("image/png", image.mimeType());
        Files.deleteIfExists(tmp);
    }

    @Test
    void fromPath_zeroSize_shouldThrow() throws Exception {
        Path tmp = Files.createTempFile("img", ".png");

        assertThrows(IllegalArgumentException.class, () -> ValidatedImage.from(tmp, 10_485_760));
        Files.deleteIfExists(tmp);
    }

    @Test
    void fromPath_tooLarge_shouldThrow() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        Path tmp = Files.createTempFile("img", ".png");
        Files.write(tmp, bytes.toByteArray());

        assertThrows(IllegalArgumentException.class, () -> ValidatedImage.from(tmp, 1));
        Files.deleteIfExists(tmp);
    }

    @Test
    void fromPath_missingFile_shouldThrowReadError() {
        Path missing = Path.of("does-not-exist-anywhere.png");
        assertThrows(IllegalArgumentException.class, () -> ValidatedImage.from(missing, 10_485_760));
    }

    // ==================== InputStream 工厂方法 ====================

    @Test
    void fromInputStream_validImage_shouldSucceed() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "jpeg", bytes);
        InputStream in = new ByteArrayInputStream(bytes.toByteArray());

        ValidatedImage image = ValidatedImage.from(in, 10_485_760);

        assertEquals("image/jpeg", image.mimeType());
    }

    @Test
    void fromInputStream_empty_shouldThrow() {
        InputStream in = new ByteArrayInputStream(new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> ValidatedImage.from(in, 10_485_760));
    }

    @Test
    void fromInputStream_tooLarge_shouldThrow() {
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[100]);
        assertThrows(IllegalArgumentException.class, () -> ValidatedImage.from(in, 10));
    }

    // ==================== 自定义限额工厂方法 ====================

    @Test
    void fromMultipartFile_withLimits_shouldApplyLimits() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        ImageValidationProperties limits = new ImageValidationProperties();
        limits.setMaxDimension(8192);
        limits.setMaxFramePixels(25_000_000L);
        limits.setMaxTotalPixels(50_000_000L);
        limits.setMaxGifFrames(100);

        ValidatedImage image = ValidatedImage.from(
                new MockMultipartFile("file", "a.png", "image/png", bytes.toByteArray()), 10_485_760, limits);

        assertEquals("image/png", image.mimeType());
    }

    @Test
    void fromMultipartFile_withLimits_nullFile_shouldThrow() {
        ImageValidationProperties limits = new ImageValidationProperties();
        assertThrows(IllegalArgumentException.class,
                () -> ValidatedImage.from((MultipartFile) null, 10_485_760, limits));
    }

    @Test
    void fromInputStream_withLimits_valid_shouldSucceed() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "gif", bytes);
        ImageValidationProperties limits = new ImageValidationProperties();
        limits.setMaxDimension(8192);
        limits.setMaxFramePixels(25_000_000L);
        limits.setMaxTotalPixels(50_000_000L);
        limits.setMaxGifFrames(100);

        ValidatedImage image = ValidatedImage.from(
                new ByteArrayInputStream(bytes.toByteArray()), 10_485_760, limits);

        assertEquals("image/gif", image.mimeType());
    }

    // ==================== asMultipartFile ====================

    @Test
    void asMultipartFile_shouldExposeValidatedContent() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        ValidatedImage image = ValidatedImage.from(
                new MockMultipartFile("file", "a.png", "image/png", bytes.toByteArray()), 10_485_760);

        MultipartFile mf = image.asMultipartFile();
        assertEquals("validated.png", mf.getOriginalFilename());
        assertEquals("image/png", mf.getContentType());
        assertEquals(bytes.size(), mf.getSize());
        assertEquals(bytes.size(), mf.getBytes().length);
        assertTrue(mf.getInputStream().read() != -1);
        mf.transferTo(Files.createTempFile("dest", ".png"));
    }

    // ==================== decode 分支补充 ====================

    @Test
    void decode_framePixelsExceeded_shouldThrow() throws Exception {
        ImageInputStream stream = mock(ImageInputStream.class);
        ImageReader reader = mock(ImageReader.class);
        try (var imageIo = mockStatic(ImageIO.class)) {
            imageIo.when(() -> ImageIO.createImageInputStream(org.mockito.ArgumentMatchers.any()))
                    .thenReturn(stream);
            imageIo.when(() -> ImageIO.getImageReaders(stream)).thenReturn(List.of(reader).iterator());
            when(reader.getFormatName()).thenReturn("png");
            when(reader.getNumImages(true)).thenReturn(1);
            when(reader.getWidth(0)).thenReturn(5000);
            when(reader.getHeight(0)).thenReturn(5000);

            assertThrows(IllegalArgumentException.class,
                    () -> ValidatedImage.from(new ByteArrayInputStream(new byte[]{1}), 10));
        }
    }

    @Test
    void decode_unreadableImage_shouldThrow() throws Exception {
        ImageInputStream stream = mock(ImageInputStream.class);
        ImageReader reader = mock(ImageReader.class);
        try (var imageIo = mockStatic(ImageIO.class)) {
            imageIo.when(() -> ImageIO.createImageInputStream(org.mockito.ArgumentMatchers.any()))
                    .thenReturn(stream);
            imageIo.when(() -> ImageIO.getImageReaders(stream)).thenReturn(List.of(reader).iterator());
            when(reader.getFormatName()).thenReturn("png");
            when(reader.getNumImages(true)).thenReturn(1);
            when(reader.getWidth(0)).thenReturn(1);
            when(reader.getHeight(0)).thenReturn(1);
            when(reader.read(0)).thenReturn(null);

            assertThrows(IllegalArgumentException.class,
                    () -> ValidatedImage.from(new ByteArrayInputStream(new byte[]{1}), 10));
        }
    }
}
