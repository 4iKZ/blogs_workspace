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
import java.nio.ByteBuffer;
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
        assertThat(result.getMessage()).contains("图片内容无效");
    }

    @Test
    void extractMetadata_oversizedDimensions_shouldRejectBeforeDecode() throws Exception {
        // 构造一个声明超大尺寸的 PNG 头（宽=16384, 高=16384），触发解压炸弹防护
        MockMultipartFile file = new MockMultipartFile("file", "bomb.png", "image/png", oversizedPngHeader());

        Result<ImageMetadataDTO> result = service.extractMetadata(file);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("图片尺寸超限");
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
        assertThat(result.getMessage()).contains("图片内容无效");
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
    void compressImage_invalidImageBytes_shouldReturnError() {
        MockMultipartFile file = new MockMultipartFile("file", "evil.png", "image/png", "not-an-image".getBytes());

        Result<byte[]> result = service.compressImage(file, null, null, null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("图片内容无效");
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

    // ==================== 异常分支补充 ====================

    private MockMultipartFile throwingInputFile() {
        return new MockMultipartFile("file", "test.png", "image/png", pngBytes) {
            @Override
            public java.io.InputStream getInputStream() throws IOException {
                throw new IOException("read failed");
            }
        };
    }

    private MockMultipartFile oversizedFile() {
        return new MockMultipartFile("file", "big.png", "image/png", pngBytes) {
            @Override
            public long getSize() {
                return 100L * 1024 * 1024 + 1;
            }
        };
    }

    @Test
    void extractMetadata_ioException_shouldReturnError() {
        Result<ImageMetadataDTO> result = service.extractMetadata(throwingInputFile());

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("提取图片元信息失败");
    }

    @Test
    void convertFormat_ioException_shouldReturnError() {
        Result<ImageConvertDTO> result = service.convertFormat(throwingInputFile(), "jpg", 0.8f);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("转换图片格式失败");
    }

    @Test
    void compressImage_ioException_shouldReturnError() {
        Result<byte[]> result = service.compressImage(throwingInputFile(), null, null, null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("压缩图片失败");
    }

    @Test
    void batchConvertFormat_ioExceptionInsideLoop_shouldCollectError() {
        MockMultipartFile good = new MockMultipartFile("good", "good.png", "image/png", pngBytes);

        Result<List<ImageConvertDTO>> result = service.batchConvertFormat(List.of(good, throwingInputFile()), "jpg", 0.8f);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    void extractMetadata_oversizedFile_shouldReturnError() {
        Result<ImageMetadataDTO> result = service.extractMetadata(oversizedFile());

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("文件大小不能超过");
    }

    @Test
    void convertFormat_oversizedFile_shouldReturnError() {
        Result<ImageConvertDTO> result = service.convertFormat(oversizedFile(), "jpg", 0.8f);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("文件大小不能超过");
    }

    @Test
    void compressImage_oversizedFile_shouldReturnError() {
        Result<byte[]> result = service.compressImage(oversizedFile(), null, null, null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("文件大小不能超过");
    }

    // ==================== helpers ====================

    private static byte[] createPngBytes() {
        return createImageBytes("png");
    }

    private static byte[] oversizedPngHeader() throws Exception {
        // PNG 签名 + IHDR chunk，宽高声明为 16384x16384（超过 MAX_DIMENSION=8192）
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A // PNG signature
        });
        baos.write(java.nio.ByteBuffer.allocate(4).putInt(13).array()); // IHDR length
        baos.write("IHDR".getBytes("US-ASCII"));
        baos.write(java.nio.ByteBuffer.allocate(4).putInt(16384).array()); // width
        baos.write(java.nio.ByteBuffer.allocate(4).putInt(16384).array()); // height
        baos.write(new byte[] { 8, 6, 0, 0, 0 }); // bit depth, color type, etc.
        // CRC (不校验，仅用于头部解析)
        baos.write(new byte[4]);
        return baos.toByteArray();
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
