package com.blog.controller;

import com.blog.common.Result;
import com.blog.dto.UserRegisterDTO;
import com.blog.exception.BusinessException;
import com.blog.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
class UserControllerReflectionTest {

    @Autowired
    private UserController userController;

    @MockBean
    private UserService userService;

    @Test
    @DisplayName("getCurrentUserId - 用户上下文为空时应抛出异常")
    void getCurrentUserId_missingUserContext_shouldThrowException() throws Exception {
        Field requestField = UserController.class.getDeclaredField("request");
        requestField.setAccessible(true);
        requestField.set(userController, mock(HttpServletRequest.class));

        Method getCurrentUserId = UserController.class.getDeclaredMethod("getCurrentUserId");
        getCurrentUserId.setAccessible(true);

        try {
            getCurrentUserId.invoke(userController);
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertEquals(BusinessException.class, e.getCause().getClass());
            assertEquals("用户未登录", e.getCause().getMessage());
            return;
        }
        throw new AssertionError("Expected BusinessException but no exception was thrown");
    }

    @Test
    @DisplayName("register - 日志记录异常时应仍成功")
    void register_loggingFailure_shouldStillSucceed() throws Exception {
        Field objectMapperField = UserController.class.getDeclaredField("objectMapper");
        objectMapperField.setAccessible(true);
        com.fasterxml.jackson.databind.ObjectMapper mockMapper = mock(com.fasterxml.jackson.databind.ObjectMapper.class);
        when(mockMapper.writeValueAsString(any())).thenThrow(new RuntimeException("JSON error"));
        objectMapperField.set(userController, mockMapper);

        when(userService.register(any(UserRegisterDTO.class)))
                .thenReturn(Result.success("success"));

        UserRegisterDTO registerDTO = new UserRegisterDTO();
        registerDTO.setUsername("test");
        registerDTO.setEmail("test@example.com");
        registerDTO.setPassword("password123");
        registerDTO.setConfirmPassword("password123");
        registerDTO.setEmailCode("123456");

        Result<String> result = userController.register(registerDTO);
        assertEquals(200, result.getCode());
    }
}
