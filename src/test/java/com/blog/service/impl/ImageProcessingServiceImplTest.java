package com.blog.service.impl;

import com.blog.common.Result;
import com.blog.dto.ImageConvertDTO;
import com.blog.dto.ImageMetadataDTO;
import com.blog.service.ImageProcessingService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageProcessingServiceImplTest {

    private final ImageProcessingService service = new ImageProcessingServiceImpl();
    private final byte[] pngBytes = createPngBytes();
    private final byte[] jpegBytes = createJpegBytes();

    // ==================== extractMetadata ====================

    @Test
    void extractMetadata_validPng_shouldReturnMetadata() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", pngBytes);

        Result<ImageMetadataDTO> result = service.extractMetadata(file);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getWidth()).isEqualTo(2);
        assertThat(result.getData().getHeight()).isEqualTo(2);
        assertThat(result.getData().getFormat()).isEqualTo("png");
        assertThat(result.getData().getMimeType()).isEqualTo("image/png");
    }

    @Test
    void extractMetadata_nullFile_shouldReturnError() {
        Result<ImageMetadataDTO> result = service.extractMetadata(null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("文件不能为空");
    }

    @Test
    void extractMetadata_emptyFile_shouldReturnError() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[] {});

        Result<ImageMetadataDTO> result = service.extractMetadata(file);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("文件不能为空");
    }

    @Test
    void extractMetadata_wrongContentType_shouldReturnError() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", pngBytes);

        Result<ImageMetadataDTO> result = service.extractMetadata(file);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("只允许上传图片文件");
    }

    @Test
    void extractMetadata_invalidImageBytes_shouldReturnError() {
        MockMultipartFile file = new MockMultipartFile("file", "evil.png", "image/png", "not-an-image".getBytes());

        Result<ImageMetadataDTO> result = service.extractMetadata(file);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("提取图片元信息失败");
    }

    // ==================== convertFormat ====================

    @Test
    void convertFormat_pngToJpg_shouldConvertSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", pngBytes);

        Result<ImageConvertDTO> result = service.convertFormat(file, "jpg", 0.8f);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getTargetFormat()).isEqualTo("jpg");
        assertThat(result.getData().getOriginalSize()).isEqualTo(pngBytes.length);
        assertThat(result.getData().getConvertedSize()).isGreaterThan(0);
        assertThat(result.getData().getImageData()).isNotNull();
    }

    @Test
    void convertFormat_unsupportedTargetFormat_shouldReturnError() {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", pngBytes);

        Result<ImageConvertDTO> result = service.convertFormat(file, "tiff", null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("不支持的目标格式");
    }

    @Test
    void convertFormat_nullFile_shouldReturnError() {
        Result<ImageConvertDTO> result = service.convertFormat(null, "jpg", 0.8f);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("文件不能为空");
    }

    @Test
    void convertFormat_wrongContentType_shouldReturnError() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", pngBytes);

        Result<ImageConvertDTO> result = service.convertFormat(file, "jpg", 0.8f);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("只允许上传图片文件");
    }

    @Test
    void convertFormat_invalidImageBytes_shouldReturnError() {
        MockMultipartFile file = new MockMultipartFile("file", "evil.png", "image/png", "not-an-image".getBytes());

        Result<ImageConvertDTO> result = service.convertFormat(file, "jpg", 0.8f);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("参数错误");
    }

    @Test
    void convertFormat_defaultQualityForJpg_shouldApplyDefault() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", pngBytes);

        Result<ImageConvertDTO> result = service.convertFormat(file, "jpg", null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getTargetFormat()).isEqualTo("jpg");
    }

    // ==================== batchConvertFormat ====================

    @Test
    void batchConvertFormat_validFiles_shouldReturnAllResults() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("file1", "a.png", "image/png", pngBytes);
        MockMultipartFile file2 = new MockMultipartFile("file2", "b.png", "image/png", pngBytes);

        Result<List<ImageConvertDTO>> result = service.batchConvertFormat(List.of(file1, file2), "jpg", 0.8f);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(2);
    }

    @Test
    void batchConvertFormat_emptyList_shouldReturnError() {
        Result<List<ImageConvertDTO>> result = service.batchConvertFormat(List.of(), "jpg", 0.8f);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("批量转换失败");
    }

    @Test
    void batchConvertFormat_nullList_shouldThrowException() {
        assertThatThrownBy(() -> service.batchConvertFormat(null, "jpg", 0.8f))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void batchConvertFormat_partialFailure_shouldReturnPartialSuccess() throws Exception {
        MockMultipartFile good = new MockMultipartFile("good", "good.png", "image/png", pngBytes);
        MockMultipartFile bad = new MockMultipartFile("bad", "bad.png", "image/png", "not-image".getBytes());

        Result<List<ImageConvertDTO>> result = service.batchConvertFormat(List.of(good, bad), "jpg", 0.8f);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    void batchConvertFormat_allFailure_shouldReturnError() {
        MockMultipartFile bad1 = new MockMultipartFile("bad1", "bad1.png", "image/png", "not-image".getBytes());
        MockMultipartFile bad2 = new MockMultipartFile("bad2", "bad2.png", "image/png", "not-image".getBytes());

        Result<List<ImageConvertDTO>> result = service.batchConvertFormat(List.of(bad1, bad2), "jpg", 0.8f);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("批量转换失败");
    }

    // ==================== getSupportedFormats ====================

    @Test
    void getSupportedFormats_shouldReturnUppercaseFormats() {
        Result<List<String>> result = service.getSupportedFormats();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).containsExactlyInAnyOrder("JPG", "JPEG", "PNG", "WEBP", "BMP");
    }

    // ==================== compressImage ====================

    @Test
    void compressImage_validImage_shouldReturnSmallerBytes() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", pngBytes);

        Result<byte[]> result = service.compressImage(file, null, null, null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData()).isNotEmpty();
    }

    @Test
    void compressImage_customDimensions_shouldResize() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", pngBytes);

        Result<byte[]> result = service.compressImage(file, 1, 1, 0.8f);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
    }

    @Test
    void compressImage_nullFile_shouldReturnError() {
        Result<byte[]> result = service.compressImage(null, null, null, null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("文件不能为空");
    }

    @Test
    void compressImage_wrongContentType_shouldReturnError() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", pngBytes);

        Result<byte[]> result = service.compressImage(file, null, null, null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("只允许上传图片文件");
    }

    @Test
    void compressImage_invalidImageBytes_shouldThrowIllegalArgument() {
        MockMultipartFile file = new MockMultipartFile("file", "evil.png", "image/png", "not-an-image".getBytes());

        assertThatThrownBy(() -> service.compressImage(file, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ==================== validateImage ====================

    @Test
    void validateImage_validImage_shouldReturnTrue() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", pngBytes);

        Result<Boolean> result = service.validateImage(file);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isTrue();
    }

    @Test
    void validateImage_invalidImageBytes_shouldReturnError() {
        MockMultipartFile file = new MockMultipartFile("file", "evil.png", "image/png", "not-an-image".getBytes());

        Result<Boolean> result = service.validateImage(file);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("无效的图片文件");
    }

    @Test
    void validateImage_nullFile_shouldReturnError() {
        Result<Boolean> result = service.validateImage(null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("无效的图片文件");
    }

    @Test
    void validateImage_emptyFile_shouldReturnError() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[] {});

        Result<Boolean> result = service.validateImage(file);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("无效的图片文件");
    }

    @Test
    void validateImage_wrongContentType_shouldReturnError() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", pngBytes);

        Result<Boolean> result = service.validateImage(file);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("无效的图片文件");
    }

    // ==================== helpers ====================

    private static byte[] createPngBytes() {
        return createImageBytes("png");
    }

    private static byte[] createJpegBytes() {
        return createImageBytes("jpeg");
    }

    private static byte[] createImageBytes(String format) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
            ImageIO.write(image, format, baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create test image", e);
        }
    }
}
