package com.blog.controller;

import com.blog.common.Result;
import com.blog.service.AdminService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllerModerationSecurityTest {
    @Mock private AdminService adminService;
    @InjectMocks private AdminController controller;

    @Test
    void legacyStatusEndpointDelegatesPublicationRejectionToService() {
        when(adminService.updateArticleStatus(12L, 2)).thenReturn(Result.error("文章发布必须通过审核决定"));

        var result = controller.updateArticleStatus(12L, Map.of("status", 2));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("文章发布必须通过审核决定");
        verify(adminService).updateArticleStatus(12L, 2);
    }
}
