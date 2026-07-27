package com.blog.controller;

import com.blog.common.Result;
import com.blog.dto.ChunkedUploadIdRequest;
import com.blog.dto.ChunkedUploadInitRequest;
import com.blog.service.ChunkedUploadService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleControllerTest {
    @Mock com.blog.service.ArticleService articleService;
    @Mock com.blog.service.ArticleRankService articleRankService;
    @Mock ChunkedUploadService uploads;
    @InjectMocks ArticleController controller;

    @BeforeEach
    void requestContext() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 42L);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void clear() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void completeUsesCurrentUserAndDelegatesLifecycleLockingToService() {
        String id = "6ba7b810-9dad-11d1-80b4-00c04fd430c8";
        when(uploads.completeUpload(42L, id)).thenReturn("https://example.com/a.png");

        Result<Map<String, String>> result = controller.completeChunkedUpload(new ChunkedUploadIdRequest(id));

        assertTrue(result.isSuccess());
        assertEquals("https://example.com/a.png", result.getData().get("url"));
        verify(uploads).completeUpload(42L, id);
    }

    @Test
    void initializationRejectsClientSuppliedUploadId() {
        assertThrows(IllegalArgumentException.class, () -> controller.initChunkedUpload(
                new ChunkedUploadInitRequest("client-id", "cover.png", 1L, 1, null)));
    }
}
