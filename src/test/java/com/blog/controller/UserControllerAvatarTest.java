package com.blog.controller;

import com.blog.service.TOSService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserControllerAvatarTest {

    @Test
    void uploadAvatar_spoofedImageContentType_shouldReject() {
        UserController controller = new UserController();
        TOSService tosService = mock(TOSService.class);
        setField(controller, "tosService", tosService);
        MockMultipartFile spoofed = new MockMultipartFile(
                "file", "avatar.jpg", "image/jpeg", "not-an-image".getBytes());

        var result = controller.uploadAvatar(spoofed);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("图片");
        verify(tosService, never()).uploadFile(any(), any());
    }

    @Test
    void uploadAvatar_realPng_shouldUploadUsingMockedTos() throws Exception {
        UserController controller = new UserController();
        TOSService tosService = mock(TOSService.class);
        setField(controller, "tosService", tosService);
        when(tosService.uploadFile(any(), eq("avatar"))).thenReturn("https://example.test/avatar.png");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        MockMultipartFile png = new MockMultipartFile(
                "file", "avatar.png", "image/png", bytes.toByteArray());

        var result = controller.uploadAvatar(png);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo("https://example.test/avatar.png");
    }

    private static void setField(UserController target, String fieldName, Object value) {
        try {
            var field = UserController.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
