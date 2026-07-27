package com.blog.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
}
