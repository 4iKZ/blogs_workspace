package com.blog.controller;

import com.blog.service.AdminService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AdminControllerModerationSecurityTest {
    @Mock private AdminService adminService;
    @InjectMocks private AdminController controller;

    @Test
    void legacyStatusEndpointRejectsDirectPublicationBeforeCallingService() {
        var result = controller.updateArticleStatus(12L, Map.of("status", 2));

        assertThat(result.isSuccess()).isFalse();
        verifyNoInteractions(adminService);
    }
}
