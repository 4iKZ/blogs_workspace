package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码DTO
 */
@Data
@Schema(description = "修改密码请求")
public class ChangePasswordDTO {

    @Schema(description = "原密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "oldPassword123")
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    @Schema(description = "新密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "newPassword456")
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "新密码长度必须在6-20个字符之间")
    private String newPassword;
}
