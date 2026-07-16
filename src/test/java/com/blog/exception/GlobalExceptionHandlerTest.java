package com.blog.exception;

import com.blog.common.Result;
import com.blog.common.ResultCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBusinessException_userNotFound_shouldReturn404() {
        BusinessException ex = new BusinessException(ResultCode.USER_NOT_FOUND, "用户不存在");
        ResponseEntity<Result<Void>> response = handler.handleBusinessException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ResultCode.USER_NOT_FOUND.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo("用户不存在");
    }

    @Test
    void handleBusinessException_usernameExist_shouldReturn409() {
        BusinessException ex = new BusinessException(ResultCode.USERNAME_EXIST, "用户名已存在");
        ResponseEntity<Result<Void>> response = handler.handleBusinessException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void handleIllegalArgumentException_shouldReturn400() {
        IllegalArgumentException ex = new IllegalArgumentException("invalid param");
        ResponseEntity<Result<Void>> response = handler.handleIllegalArgumentException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("invalid param");
    }

    @Test
    void handleRuntimeException_shouldReturn500() {
        RuntimeException ex = new RuntimeException("boom");
        ResponseEntity<Result<Void>> response = handler.handleRuntimeException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("服务器内部错误");
    }

    @Test
    void handleNullPointerException_shouldReturn500WithDataMessage() {
        NullPointerException ex = new NullPointerException("npe");
        ResponseEntity<Result<Void>> response = handler.handleNullPointerException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("数据异常");
    }

    @Test
    void handleMethodArgumentNotValidException_shouldReturn400WithFieldErrors() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "obj");
        bindingResult.addError(new FieldError("obj", "name", "must not be blank"));
        java.lang.reflect.Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("handleMethodArgumentNotValidException_shouldReturn400WithFieldErrors");
        org.springframework.core.MethodParameter parameter = new org.springframework.core.MethodParameter(method, -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<Result<Void>> response = handler.handleMethodArgumentNotValidException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("must not be blank");
    }

    @Test
    void handleConstraintViolationException_shouldReturn400WithMessage() {
        ConstraintViolationException ex = new ConstraintViolationException(
                "constraint violated", Collections.emptySet());
        ResponseEntity<Result<Void>> response = handler.handleConstraintViolationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void handleException_shouldReturn500() {
        Exception ex = new Exception("unknown");
        ResponseEntity<Result<Void>> response = handler.handleException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("系统错误");
    }

    @Test
    void handleBusinessException_variousCodes_shouldMapCorrectly() {
        assertThat(statusFor(ResultCode.USER_NOT_FOUND)).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(statusFor(ResultCode.ARTICLE_NOT_FOUND)).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(statusFor(ResultCode.USERNAME_EXIST)).isEqualTo(HttpStatus.CONFLICT);
        assertThat(statusFor(ResultCode.EMAIL_EXIST)).isEqualTo(HttpStatus.CONFLICT);
        assertThat(statusFor(ResultCode.PASSWORD_ERROR)).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(statusFor(ResultCode.USER_DISABLED)).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleBusinessException_systemCode_shouldReturn500() {
        BusinessException ex = new BusinessException(ResultCode.SYSTEM_ERROR, "系统错误");
        ResponseEntity<Result<Void>> response = handler.handleBusinessException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private HttpStatus statusFor(ResultCode code) {
        return HttpStatus.valueOf(handler.handleBusinessException(new BusinessException(code)).getStatusCode().value());
    }
}
