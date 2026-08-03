package com.blog.controller;

import com.blog.common.Result;
import com.blog.service.AdminService;
import com.blog.service.ArticleModerationSubmissionService;
import com.blog.utils.AuthUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminControllerUnitTest {

    private MockMvc mockMvc;
    private AdminService adminService;
    private ArticleModerationSubmissionService moderationSubmissionService;
    private AdminController controller;

    @BeforeEach
    void setUp() throws Exception {
        adminService = mock(AdminService.class);
        moderationSubmissionService = mock(ArticleModerationSubmissionService.class);
        controller = new AdminController();
        
        // 使用反射注入 @Autowired 字段
        Field adminServiceField = AdminController.class.getDeclaredField("adminService");
        adminServiceField.setAccessible(true);
        adminServiceField.set(controller, adminService);
        
        Field moderationServiceField = AdminController.class.getDeclaredField("moderationSubmissionService");
        moderationServiceField.setAccessible(true);
        moderationServiceField.set(controller, moderationSubmissionService);
        
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new com.blog.exception.GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("approveModerationSubmission - 缺少原因应返回 400")
    void approveModerationSubmission_missingReason_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/moderation/submissions/token123/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("rejectModerationSubmission - 缺少原因应返回 400")
    void rejectModerationSubmission_missingReason_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/moderation/submissions/token123/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("requireReason - null Map 应抛出异常")
    void requireReason_nullMap_shouldThrowException() throws Exception {
        java.lang.reflect.Method method = AdminController.class.getDeclaredMethod("requireReason", Map.class);
        method.setAccessible(true);
        assertThrows(IllegalArgumentException.class, () -> {
            try {
                method.invoke(controller, (Object) null);
            } catch (java.lang.reflect.InvocationTargetException e) {
                Throwable target = e.getCause();
                if (target instanceof IllegalArgumentException) {
                    throw (IllegalArgumentException) target;
                }
                throw new RuntimeException(target);
            }
        });
    }

    @Test
    @DisplayName("requireReason - 空 reason 应抛出异常")
    void requireReason_emptyReason_shouldThrowException() throws Exception {
        java.lang.reflect.Method method = AdminController.class.getDeclaredMethod("requireReason", Map.class);
        method.setAccessible(true);
        assertThrows(IllegalArgumentException.class, () -> {
            try {
                method.invoke(controller, Map.of("reason", ""));
            } catch (java.lang.reflect.InvocationTargetException e) {
                Throwable target = e.getCause();
                if (target instanceof IllegalArgumentException) {
                    throw (IllegalArgumentException) target;
                }
                throw new RuntimeException(target);
            }
        });
    }

    @Test
    @DisplayName("requireReason - 空白 reason 应抛出异常")
    void requireReason_blankReason_shouldThrowException() throws Exception {
        java.lang.reflect.Method method = AdminController.class.getDeclaredMethod("requireReason", Map.class);
        method.setAccessible(true);
        assertThrows(IllegalArgumentException.class, () -> {
            try {
                method.invoke(controller, Map.of("reason", "   "));
            } catch (java.lang.reflect.InvocationTargetException e) {
                Throwable target = e.getCause();
                if (target instanceof IllegalArgumentException) {
                    throw (IllegalArgumentException) target;
                }
                throw new RuntimeException(target);
            }
        });
    }

    @Test
    @DisplayName("requireReason - 正常 reason 应返回trim后结果")
    void requireReason_validReason_shouldReturnTrimmed() throws Exception {
        java.lang.reflect.Method method = AdminController.class.getDeclaredMethod("requireReason", Map.class);
        method.setAccessible(true);
        Object result = method.invoke(controller, Map.of("reason", "  正常原因  "));
        assert result.equals("正常原因");
    }

    @Test
    @DisplayName("getVisitStatistics - day 类型应返回统计结果")
    void getVisitStatistics_day_shouldReturnResult() {
        when(adminService.getVisitStatistics(any(), any())).thenReturn(Result.success(Map.of("visits", 10)));

        Result<?> result = controller.getVisitStatistics("day");

        assert result.getCode() == 200;
    }

    @Test
    @DisplayName("getVisitStatistics - week 类型应返回统计结果")
    void getVisitStatistics_week_shouldReturnResult() {
        when(adminService.getVisitStatistics(any(), any())).thenReturn(Result.success(Map.of("visits", 50)));

        Result<?> result = controller.getVisitStatistics("week");

        assert result.getCode() == 200;
    }

    @Test
    @DisplayName("getVisitStatistics - month 类型应返回统计结果")
    void getVisitStatistics_month_shouldReturnResult() {
        when(adminService.getVisitStatistics(any(), any())).thenReturn(Result.success(Map.of("visits", 200)));

        Result<?> result = controller.getVisitStatistics("month");

        assert result.getCode() == 200;
    }

    @Test
    @DisplayName("approveModerationSubmission - 缺少原因应抛出异常")
    void approveModerationSubmission_missingReason_shouldThrowException() {
        try (MockedStatic<AuthUtils> mocked = mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            assertThrows(IllegalArgumentException.class, () ->
                    controller.approveModerationSubmission("token123", Map.of("other", "value")));
        }
    }

    @Test
    @DisplayName("approveModerationSubmission - 正常原因应返回结果")
    void approveModerationSubmission_validReason_shouldReturnResult() {
        try (MockedStatic<AuthUtils> mocked = mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            Result<Void> result = controller.approveModerationSubmission("token123", Map.of("reason", "  正常原因  "));

            assert result.getCode() == 200;
            verify(moderationSubmissionService).approve(eq("token123"), eq(1L), eq("正常原因"));
        }
    }

    @Test
    @DisplayName("rejectModerationSubmission - 缺少原因应抛出异常")
    void rejectModerationSubmission_missingReason_shouldThrowException() {
        try (MockedStatic<AuthUtils> mocked = mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            assertThrows(IllegalArgumentException.class, () ->
                    controller.rejectModerationSubmission("token123", Map.of()));
        }
    }

    @Test
    @DisplayName("rejectModerationSubmission - 正常原因应返回结果")
    void rejectModerationSubmission_validReason_shouldReturnResult() {
        try (MockedStatic<AuthUtils> mocked = mockStatic(AuthUtils.class)) {
            mocked.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            Result<Void> result = controller.rejectModerationSubmission("token123", Map.of("reason", "拒绝原因"));

            assert result.getCode() == 200;
            verify(moderationSubmissionService).reject(eq("token123"), eq(1L), eq("拒绝原因"));
        }
    }
}
