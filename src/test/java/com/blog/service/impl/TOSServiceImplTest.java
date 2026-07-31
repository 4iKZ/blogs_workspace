package com.blog.service.impl;

import com.blog.config.TOSConfig;
import com.blog.service.TOSService;
import com.volcengine.tos.TOSV2;
import com.volcengine.tos.model.object.DeleteObjectInput;
import com.volcengine.tos.model.object.HeadObjectV2Input;
import com.volcengine.tos.model.object.PutObjectInput;
import com.volcengine.tos.model.object.PutObjectOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TOSServiceImplTest {

    @Mock
    private TOSV2 tosClient;

    @Mock
    private TOSConfig tosConfig;

    @InjectMocks
    private TOSServiceImpl tosService;

    private void stubTosConfig() {
        when(tosConfig.getBucketName()).thenReturn("bucket");
        when(tosConfig.getFullObjectKey(any())).thenAnswer(i -> "base/" + i.getArgument(0));
        when(tosConfig.getPublicUrl(any())).thenAnswer(i -> "https://bucket.endpoint/" + i.getArgument(0));
        when(tosClient.putObject(any(PutObjectInput.class))).thenReturn(new PutObjectOutput());
        when(tosClient.deleteObject(any(DeleteObjectInput.class)))
                .thenReturn(new com.volcengine.tos.model.object.DeleteObjectOutput());
        when(tosClient.headObject(any(HeadObjectV2Input.class)))
                .thenReturn(new com.volcengine.tos.model.object.HeadObjectV2Output());
    }

    // ==================== uploadFile ====================

    @Test
    void uploadFile_whenJpeg_shouldReturnPublicUrl() throws Exception {
        stubTosConfig();

        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[] { 1, 2, 3 });

        String url = tosService.uploadFile(file, "covers");

        assertThat(url).startsWith("https://bucket.endpoint/base/covers/");
        assertThat(url).doesNotContain("?x-tos-process");
        verify(tosClient).putObject(any(PutObjectInput.class));
    }

    @Test
    void uploadFile_whenPng_shouldUsePngExtension() throws Exception {
        stubTosConfig();

        MultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[] { 4, 5, 6 });

        String url = tosService.uploadFile(file, "covers");

        assertThat(url).startsWith("https://bucket.endpoint/base/covers/");
        assertThat(url).endsWith(".png");
        verify(tosClient).putObject(any(PutObjectInput.class));
    }

    @Test
    void uploadFile_whenGif_shouldUseGifExtension() throws Exception {
        stubTosConfig();

        MultipartFile file = new MockMultipartFile("file", "anim.gif", "image/gif", new byte[] { 7, 8, 9 });

        String url = tosService.uploadFile(file, "articles");

        assertThat(url).startsWith("https://bucket.endpoint/base/articles/");
        assertThat(url).endsWith(".gif");
    }

    @Test
    void uploadFile_whenUnknownMime_withValidExtension_shouldPreserveExtension() throws Exception {
        stubTosConfig();

        MultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[] { 1, 2, 3 });

        String url = tosService.uploadFile(file, "attachments");

        assertThat(url).endsWith(".pdf");
    }

    @Test
    void uploadFile_whenUnknownMime_withInvalidExtension_shouldFallbackToBin() throws Exception {
        stubTosConfig();

        MultipartFile file = new MockMultipartFile("file", "badfile", "application/octet-stream",
                new byte[] { 1, 2, 3 });

        String url = tosService.uploadFile(file, "attachments");

        assertThat(url).endsWith(".bin");
    }

    @Test
    void uploadFile_whenTosClientException_shouldWrapRuntimeException() throws Exception {
        stubTosConfig();
        when(tosClient.putObject(any(PutObjectInput.class))).thenThrow(new RuntimeException("client error"));

        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[] { 1, 2, 3 });

        assertThatThrownBy(() -> tosService.uploadFile(file, "covers"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void uploadFile_whenTosServerException_shouldWrapRuntimeException() throws Exception {
        stubTosConfig();
        when(tosClient.putObject(any(PutObjectInput.class))).thenThrow(new RuntimeException("server error"));

        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[] { 1, 2, 3 });

        assertThatThrownBy(() -> tosService.uploadFile(file, "covers"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void uploadFile_whenIOException_shouldWrapRuntimeException() throws Exception {
        stubTosConfig();

        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[] { 1, 2, 3 }) {
            @Override
            public InputStream getInputStream() {
                throw new RuntimeException(new IOException("read error"));
            }
        };

        assertThatThrownBy(() -> tosService.uploadFile(file, "covers"))
                .isInstanceOf(RuntimeException.class);
    }

    // ==================== uploadFileWithStyle(boolean) ====================

    @Test
    void uploadFileWithStyle_useStyleTrue_withDefaultStyle_shouldAppendStyleParam() throws Exception {
        stubTosConfig();
        when(tosConfig.getDefaultImageStyle()).thenReturn("lumina");

        MultipartFile file = new MockMultipartFile("file", "cover.jpg", "image/jpeg", new byte[] { 1, 2, 3 });

        String url = tosService.uploadFileWithStyle(file, "covers", true);

        assertThat(url).startsWith("https://bucket.endpoint/base/covers/");
        assertThat(url).contains("?x-tos-process=style/lumina");
    }

    @Test
    void uploadFileWithStyle_useStyleTrue_withEmptyDefaultStyle_shouldReturnOriginalUrl() throws Exception {
        stubTosConfig();
        when(tosConfig.getDefaultImageStyle()).thenReturn("");

        MultipartFile file = new MockMultipartFile("file", "cover.jpg", "image/jpeg", new byte[] { 1, 2, 3 });

        String url = tosService.uploadFileWithStyle(file, "covers", true);

        assertThat(url).startsWith("https://bucket.endpoint/base/covers/");
        assertThat(url).doesNotContain("?x-tos-process");
    }

    @Test
    void uploadFileWithStyle_useStyleFalse_shouldReturnOriginalUrl() throws Exception {
        stubTosConfig();

        MultipartFile file = new MockMultipartFile("file", "cover.jpg", "image/jpeg", new byte[] { 1, 2, 3 });

        String url = tosService.uploadFileWithStyle(file, "covers", false);

        assertThat(url).startsWith("https://bucket.endpoint/base/covers/");
        assertThat(url).doesNotContain("?x-tos-process");
    }

    // ==================== uploadFileWithStyle(String) ====================

    @Test
    void uploadFileWithStyle_namedStyle_shouldAppendNamedStyleParam() throws Exception {
        stubTosConfig();

        MultipartFile file = new MockMultipartFile("file", "cover.jpg", "image/jpeg", new byte[] { 1, 2, 3 });

        String url = tosService.uploadFileWithStyle(file, "covers", "thumbnail");

        assertThat(url).startsWith("https://bucket.endpoint/base/covers/");
        assertThat(url).contains("?x-tos-process=style/thumbnail");
    }

    @Test
    void uploadFileWithStyle_nullStyle_shouldReturnOriginalUrl() throws Exception {
        stubTosConfig();

        MultipartFile file = new MockMultipartFile("file", "cover.jpg", "image/jpeg", new byte[] { 1, 2, 3 });

        String url = tosService.uploadFileWithStyle(file, "covers", (String) null);

        assertThat(url).startsWith("https://bucket.endpoint/base/covers/");
        assertThat(url).doesNotContain("?x-tos-process");
    }

    @Test
    void uploadFileWithStyle_emptyStyle_shouldReturnOriginalUrl() throws Exception {
        stubTosConfig();

        MultipartFile file = new MockMultipartFile("file", "cover.jpg", "image/jpeg", new byte[] { 1, 2, 3 });

        String url = tosService.uploadFileWithStyle(file, "covers", "   ");

        assertThat(url).startsWith("https://bucket.endpoint/base/covers/");
        assertThat(url).contains("?x-tos-process=style/   ");
    }

    // ==================== uploadFileWithStyleAtObjectKey ====================

    @Test
    void uploadFileWithStyleAtObjectKey_useStyleTrue_shouldAppendStyleParam() throws Exception {
        stubTosConfig();
        when(tosConfig.getFullObjectKey("avatars/uuid.jpg")).thenReturn("base/avatars/uuid.jpg");
        when(tosConfig.getPublicUrl("base/avatars/uuid.jpg"))
                .thenReturn("https://bucket.endpoint/base/avatars/uuid.jpg");
        when(tosConfig.getDefaultImageStyle()).thenReturn("lumina");

        MultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", new byte[] { 1, 2, 3 });

        String url = tosService.uploadFileWithStyleAtObjectKey(file, "avatars/uuid.jpg", true);

        assertThat(url).isEqualTo("https://bucket.endpoint/base/avatars/uuid.jpg?x-tos-process=style/lumina");
        verify(tosClient).putObject(any(PutObjectInput.class));
    }

    @Test
    void uploadFileWithStyleAtObjectKey_useStyleFalse_shouldReturnOriginalUrl() throws Exception {
        stubTosConfig();
        when(tosConfig.getFullObjectKey("avatars/uuid.jpg")).thenReturn("base/avatars/uuid.jpg");
        when(tosConfig.getPublicUrl("base/avatars/uuid.jpg"))
                .thenReturn("https://bucket.endpoint/base/avatars/uuid.jpg");

        MultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", new byte[] { 1, 2, 3 });

        String url = tosService.uploadFileWithStyleAtObjectKey(file, "avatars/uuid.jpg", false);

        assertThat(url).isEqualTo("https://bucket.endpoint/base/avatars/uuid.jpg");
    }

    @Test
    void uploadFileWithStyleAtObjectKey_whenTosException_shouldWrapRuntimeException() throws Exception {
        stubTosConfig();
        when(tosClient.putObject(any(PutObjectInput.class))).thenThrow(new RuntimeException("client error"));

        MultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", new byte[] { 1, 2, 3 });

        assertThatThrownBy(() -> tosService.uploadFileWithStyleAtObjectKey(file, "avatars/uuid.jpg", true))
                .isInstanceOf(RuntimeException.class);
    }

    // ==================== uploadBytes ====================

    @Test
    void uploadBytes_whenValidBytes_shouldReturnPublicUrl() throws Exception {
        stubTosConfig();

        String url = tosService.uploadBytes(new byte[] { 1, 2, 3 }, "doc.pdf", "attachments", "application/pdf");

        assertThat(url).startsWith("https://bucket.endpoint/base/attachments/");
        assertThat(url).endsWith(".pdf");
        verify(tosClient).putObject(any(PutObjectInput.class));
    }

    @Test
    void uploadBytes_whenTosClientException_shouldWrapRuntimeException() throws Exception {
        stubTosConfig();
        when(tosClient.putObject(any(PutObjectInput.class))).thenThrow(new RuntimeException("client error"));

        assertThatThrownBy(
                () -> tosService.uploadBytes(new byte[] { 1, 2, 3 }, "doc.pdf", "attachments", "application/pdf"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void uploadBytes_whenTosServerException_shouldWrapRuntimeException() throws Exception {
        stubTosConfig();
        when(tosClient.putObject(any(PutObjectInput.class))).thenThrow(new RuntimeException("server error"));

        assertThatThrownBy(
                () -> tosService.uploadBytes(new byte[] { 1, 2, 3 }, "doc.pdf", "attachments", "application/pdf"))
                .isInstanceOf(RuntimeException.class);
    }

    // ==================== deleteFile ====================

    @Test
    void deleteFile_whenSuccess_shouldReturnTrue() throws Exception {
        stubTosConfig();

        boolean result = tosService.deleteFile("objects/img.jpg");

        assertThat(result).isTrue();
        verify(tosClient).deleteObject(any(DeleteObjectInput.class));
    }

    // ==================== batchDeleteFiles ====================

    @Test
    void batchDeleteFiles_whenNull_shouldReturnTrue() {
        boolean result = tosService.batchDeleteFiles(null);

        assertThat(result).isTrue();
        verify(tosClient, never()).deleteObject(any());
    }

    @Test
    void batchDeleteFiles_whenEmpty_shouldReturnTrue() {
        boolean result = tosService.batchDeleteFiles(List.of());

        assertThat(result).isTrue();
        verify(tosClient, never()).deleteObject(any());
    }

    @Test
    void batchDeleteFiles_whenAllSuccess_shouldReturnTrue() throws Exception {
        stubTosConfig();

        boolean result = tosService.batchDeleteFiles(List.of("a.jpg", "b.jpg"));

        assertThat(result).isTrue();
        verify(tosClient, times(2)).deleteObject(any(DeleteObjectInput.class));
    }

    // ==================== getPublicUrl ====================

    @Test
    void getPublicUrl_shouldDelegateToConfig() {
        when(tosConfig.getPublicUrl("objects/img.jpg")).thenReturn("https://bucket.endpoint/objects/img.jpg");

        String url = tosService.getPublicUrl("objects/img.jpg");

        assertThat(url).isEqualTo("https://bucket.endpoint/objects/img.jpg");
        verify(tosConfig).getPublicUrl("objects/img.jpg");
    }

    // ==================== fileExists ====================

    @Test
    void fileExists_whenHeadSucceeds_shouldReturnTrue() throws Exception {
        stubTosConfig();

        boolean result = tosService.fileExists("objects/img.jpg");

        assertThat(result).isTrue();
        verify(tosClient).headObject(any(HeadObjectV2Input.class));
    }
}
