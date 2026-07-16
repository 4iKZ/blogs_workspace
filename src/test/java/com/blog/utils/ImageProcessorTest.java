package com.blog.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImageProcessorTest {

    @Test
    void isFormatSupported_supportedFormats_shouldReturnTrue() {
        assertThat(ImageProcessor.isFormatSupported("jpg")).isTrue();
        assertThat(ImageProcessor.isFormatSupported("JPG")).isTrue();
        assertThat(ImageProcessor.isFormatSupported("png")).isTrue();
        assertThat(ImageProcessor.isFormatSupported("webp")).isTrue();
        assertThat(ImageProcessor.isFormatSupported("bmp")).isTrue();
    }

    @Test
    void isFormatSupported_unsupportedFormats_shouldReturnFalse() {
        assertThat(ImageProcessor.isFormatSupported("tiff")).isFalse();
        assertThat(ImageProcessor.isFormatSupported("svg")).isFalse();
        assertThat(ImageProcessor.isFormatSupported("ico")).isFalse();
        assertThat(ImageProcessor.isFormatSupported("heic")).isFalse();
    }

    @Test
    void getFormatMimeType_shouldReturnCorrectMimeType() {
        assertThat(ImageProcessor.getFormatMimeType("jpg")).isEqualTo("image/jpeg");
        assertThat(ImageProcessor.getFormatMimeType("png")).isEqualTo("image/png");
        assertThat(ImageProcessor.getFormatMimeType("gif")).isEqualTo("image/gif");
        assertThat(ImageProcessor.getFormatMimeType("webp")).isEqualTo("image/webp");
        assertThat(ImageProcessor.getFormatMimeType("bmp")).isEqualTo("image/bmp");
    }

    @Test
    void getMimeTypeFormat_shouldReturnCorrectFormat() {
        assertThat(ImageProcessor.getMimeTypeFormat("image/jpeg")).isEqualTo("jpg");
        assertThat(ImageProcessor.getMimeTypeFormat("image/png")).isEqualTo("png");
        assertThat(ImageProcessor.getMimeTypeFormat("image/gif")).isEqualTo("gif");
        assertThat(ImageProcessor.getMimeTypeFormat("image/webp")).isEqualTo("webp");
        assertThat(ImageProcessor.getMimeTypeFormat("image/bmp")).isEqualTo("bmp");
    }

    @Test
    void getFormatMimeType_unknownFormat_shouldReturnNull() {
        assertThat(ImageProcessor.getFormatMimeType("tiff")).isNull();
        assertThat(ImageProcessor.getFormatMimeType("svg")).isNull();
    }

    @Test
    void getMimeTypeFormat_unknownMimeType_shouldReturnNull() {
        assertThat(ImageProcessor.getMimeTypeFormat("image/svg+xml")).isNull();
        assertThat(ImageProcessor.getMimeTypeFormat("image/tiff")).isNull();
    }
}
