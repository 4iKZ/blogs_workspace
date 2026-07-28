package com.blog.service.impl;

import com.blog.service.TOSService;
import com.blog.config.TOSConfig;
import com.volcengine.tos.TOSV2;
import com.volcengine.tos.model.object.PutObjectInput;
import com.volcengine.tos.model.object.PutObjectOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileUploadImageSecurityTest {
    @ParameterizedTest
    @ValueSource(strings = {"jpeg", "png", "gif"})
    void imageEndpointPassesDetectedMimeAndSafeExtensionToTos(String format) throws Exception {
        TOSService tos = mock(TOSService.class);
        when(tos.uploadFileWithStyle(org.mockito.ArgumentMatchers.any(), eq("covers"), eq(true)))
                .thenReturn("https://example.com/image");
        FileUploadServiceImpl service = new FileUploadServiceImpl();
        ReflectionTestUtils.setField(service, "tosService", tos);
        ReflectionTestUtils.setField(service, "maxFileSize", 10_485_760L);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), format, bytes);

        service.uploadImage(new MockMultipartFile("file", "attack.webp", "image/webp", bytes.toByteArray()));

        ArgumentCaptor<MultipartFile> file = ArgumentCaptor.forClass(MultipartFile.class);
        verify(tos).uploadFileWithStyle(file.capture(), eq("covers"), eq(true));
        String mime = format.equals("jpeg") ? "image/jpeg" : "image/" + format;
        String extension = format.equals("jpeg") ? ".jpg" : "." + format;
        assertEquals(mime, file.getValue().getContentType());
        assertEquals("validated" + extension, file.getValue().getOriginalFilename());
    }

    @Test
    void imageEndpointRejectsPseudoImageWithoutCallingTos() {
        TOSService tos = mock(TOSService.class);
        FileUploadServiceImpl service = new FileUploadServiceImpl();
        ReflectionTestUtils.setField(service, "tosService", tos);
        ReflectionTestUtils.setField(service, "maxFileSize", 10_485_760L);

        assertFalse(service.uploadImage(new MockMultipartFile(
                "file", "a.png", "image/png", "fake".getBytes())).isSuccess());
        verify(tos, org.mockito.Mockito.never()).uploadFileWithStyle(
                org.mockito.ArgumentMatchers.any(), eq("covers"), eq(true));
    }

    @Test
    void tosPutObjectUsesDetectedMimeAndMimeDerivedExtension() throws Exception {
        TOSV2 client = mock(TOSV2.class);
        when(client.putObject(org.mockito.ArgumentMatchers.any())).thenReturn(new PutObjectOutput());
        TOSConfig config = new TOSConfig();
        config.setBucketName("bucket");
        config.setEndpoint("tos.example.com");
        config.setBaseFolder("blog");
        TOSServiceImpl tos = new TOSServiceImpl();
        ReflectionTestUtils.setField(tos, "tosClient", client);
        ReflectionTestUtils.setField(tos, "tosConfig", config);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        MultipartFile validated = ValidatedImage.from(new MockMultipartFile(
                "file", "evil.webp", "application/octet-stream", bytes.toByteArray()), 10_485_760)
                .asMultipartFile();

        tos.uploadFile(validated, "covers");

        ArgumentCaptor<PutObjectInput> input = ArgumentCaptor.forClass(PutObjectInput.class);
        verify(client).putObject(input.capture());
        assertEquals("image/png", input.getValue().getOptions().getContentType());
        org.junit.jupiter.api.Assertions.assertTrue(input.getValue().getKey().endsWith(".png"));
    }

    @Test
    void tosStableObjectKeyIsUsedExactlyAcrossRetries() {
        TOSV2 client = mock(TOSV2.class);
        when(client.putObject(org.mockito.ArgumentMatchers.any())).thenReturn(new PutObjectOutput());
        TOSConfig config = new TOSConfig();
        config.setBucketName("bucket");
        config.setEndpoint("tos.example.com");
        config.setBaseFolder("blog");
        TOSServiceImpl tos = new TOSServiceImpl();
        ReflectionTestUtils.setField(tos, "tosClient", client);
        ReflectionTestUtils.setField(tos, "tosConfig", config);
        MultipartFile file = new MockMultipartFile(
                "file", "validated.png", "image/png", new byte[]{1, 2, 3});

        tos.uploadFileWithStyleAtObjectKey(file, "covers/chunked/upload-id.png", false);
        tos.uploadFileWithStyleAtObjectKey(file, "covers/chunked/upload-id.png", false);

        ArgumentCaptor<PutObjectInput> input = ArgumentCaptor.forClass(PutObjectInput.class);
        verify(client, org.mockito.Mockito.times(2)).putObject(input.capture());
        assertEquals("blog/covers/chunked/upload-id.png", input.getAllValues().get(0).getKey());
        assertEquals(input.getAllValues().get(0).getKey(), input.getAllValues().get(1).getKey());
    }
}
