package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 错误详情DTO
 * 提供详细的错误信息用于前端调试
 */
@Data
@Schema(description = "错误详情")
public class ErrorDetailDTO {

    @Schema(description = "错误类型", example = "ValidationError")
    private String errorType;

    @Schema(description = "错误码", example = "IMAGE_FORMAT_NOT_SUPPORTED")
    private String errorCode;

    @Schema(description = "错误消息", example = "不支持的图片格式")
    private String errorMessage;

    @Schema(description = "详细描述", example = "当前系统仅支持 JPG、PNG、GIF、WEBP 格式的图片")
    private String detail;

    @Schema(description = "错误路径", example = "/api/article/upload-cover")
    private String path;

    @Schema(description = "请求方法", example = "POST")
    private String method;

    @Schema(description = "时间戳")
    private LocalDateTime timestamp;

    @Schema(description = "请求ID（用于追踪）")
    private String requestId;

    @Schema(description = "堆栈跟踪（开发环境）")
    private List<String> stackTrace;

    @Schema(description = "额外信息")
    private Object additionalInfo;

    /**
     * 创建验证错误
     */
    public static ErrorDetailDTO validationError(String field, String message) {
        ErrorDetailDTO dto = new ErrorDetailDTO();
        dto.setErrorType("ValidationError");
        dto.setErrorCode("VALIDATION_ERROR");
        dto.setErrorMessage(message);
        dto.setDetail(String.format("字段 '%s' 验证失败: %s", field, message));
        dto.setTimestamp(LocalDateTime.now());
        return dto;
    }

    /**
     * 创建业务错误
     */
    public static ErrorDetailDTO businessError(String code, String message, String detail) {
        ErrorDetailDTO dto = new ErrorDetailDTO();
        dto.setErrorType("BusinessError");
        dto.setErrorCode(code);
        dto.setErrorMessage(message);
        dto.setDetail(detail);
        dto.setTimestamp(LocalDateTime.now());
        return dto;
    }

    /**
     * 创建系统错误
     */
    public static ErrorDetailDTO systemError(String message, Throwable cause) {
        ErrorDetailDTO dto = new ErrorDetailDTO();
        dto.setErrorType("SystemError");
        dto.setErrorCode("SYSTEM_ERROR");
        dto.setErrorMessage(message);
        dto.setDetail(cause != null ? cause.getMessage() : "系统内部错误");
        dto.setTimestamp(LocalDateTime.now());
        return dto;
    }

    /**
     * 创建文件上传错误
     */
    public static ErrorDetailDTO uploadError(String code, String message, String fileName) {
        ErrorDetailDTO dto = new ErrorDetailDTO();
        dto.setErrorType("UploadError");
        dto.setErrorCode(code);
        dto.setErrorMessage(message);
        dto.setDetail(String.format("文件 '%s' 上传失败: %s", fileName, message));
        dto.setTimestamp(LocalDateTime.now());
        dto.setAdditionalInfo(new java.util.HashMap<String, Object>() {{
            put("fileName", fileName);
        }});
        return dto;
    }
}
