package com.blog.exception;

import com.blog.common.Result;
import com.blog.common.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理类
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 将业务错误码映射到HTTP状态码
     */
    private HttpStatus getHttpStatus(Integer code) {
        if (code == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        // 标准HTTP状态码直接映射
        if (code == 200) return HttpStatus.OK;
        if (code == 400) return HttpStatus.BAD_REQUEST;
        if (code == 401) return HttpStatus.UNAUTHORIZED;
        if (code == 403) return HttpStatus.FORBIDDEN;
        if (code == 404) return HttpStatus.NOT_FOUND;
        if (code == 405) return HttpStatus.METHOD_NOT_ALLOWED;
        if (code == 409) return HttpStatus.CONFLICT;
        if (code == 422) return HttpStatus.UNPROCESSABLE_ENTITY;
        if (code == 429) return HttpStatus.TOO_MANY_REQUESTS;
        if (code == 500) return HttpStatus.INTERNAL_SERVER_ERROR;
        if (code == 503) return HttpStatus.SERVICE_UNAVAILABLE;

        // 业务错误码映射 (1000-9999)
        if (code >= 1000 && code < 10000) {
            // 用户不存在、文章不存在、分类不存在等 -> 404
            if (code == 1001 || code == 2001 || code == 2004 || code == 3001 || code == 4001 || code == 5001 || code == 6001) {
                return HttpStatus.NOT_FOUND;
            }
            // 用户已存在、邮箱已存在等 -> 409
            if (code == 1004 || code == 1005 || code == 1006 || code == 6002 || code == 7001 || code == 7002) {
                return HttpStatus.CONFLICT;
            }
            // 密码错误、用户禁用等 -> 400
            if (code == 1002 || code == 1003 || code == 1007 || code == 1008 || code == 2002 || code == 2003 || code == 2005) {
                return HttpStatus.BAD_REQUEST;
            }
            // 其他业务错误 -> 400
            return HttpStatus.BAD_REQUEST;
        }

        // 系统错误码映射 (10000+)
        if (code >= 10000) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage(), e);
        Result<Void> result = Result.error(e.getCode(), e.getMessage());
        return ResponseEntity.status(getHttpStatus(e.getCode())).body(result);
    }

    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("参数验证异常: {}", e.getMessage(), e);
        BindingResult bindingResult = e.getBindingResult();
        String errorMessage = bindingResult.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        Result<Void> result = Result.error(ResultCode.BAD_REQUEST.getCode(), errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * 处理约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        log.error("约束违反异常: {}", e.getMessage(), e);
        String errorMessage = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        Result<Void> result = Result.error(ResultCode.BAD_REQUEST.getCode(), errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * 处理文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.error("文件上传大小超限: {}", e.getMessage(), e);
        Result<Void> result = Result.error(ResultCode.FILE_SIZE_ERROR.getCode(), "文件大小超出限制");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Result<Void>> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常: {}", e.getMessage(), e);
        Result<Void> result = Result.error(ResultCode.ERROR.getCode(), "服务器内部错误");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Result<Void>> handleNullPointerException(NullPointerException e) {
        log.error("空指针异常: {}", e.getMessage(), e);
        Result<Void> result = Result.error(ResultCode.ERROR.getCode(), "数据异常");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("非法参数异常: {}", e.getMessage(), e);
        Result<Void> result = Result.error(ResultCode.BAD_REQUEST.getCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("未知异常: {}", e.getMessage(), e);
        Result<Void> result = Result.error(ResultCode.SYSTEM_ERROR.getCode(), "系统错误");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }
}