package com.blog.dto;

/**
 * 验证码响应数据
 */
public class CaptchaResponseDTO {
    private String captchaKey;
    private String captchaImage;

    public CaptchaResponseDTO(String captchaKey, String captchaImage) {
        this.captchaKey = captchaKey;
        this.captchaImage = captchaImage;
    }

    public String getCaptchaKey() {
        return captchaKey;
    }

    public String getCaptchaImage() {
        return captchaImage;
    }
}
