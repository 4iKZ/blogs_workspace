package com.blog.controller;

import com.blog.common.Result;
import com.blog.service.CaptchaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 验证码控制器
 */
@RestController
@RequestMapping("/api/captcha")
@Tag(name = "验证码接口")
@Slf4j
public class CaptchaController {

    @Autowired
    private CaptchaService captchaService;

    /**
     * 生成验证码
     * @return 验证码key
     */
    @PostMapping("/generate")
    @Operation(summary = "生成验证码")
    public Result<String> generateCaptcha() {
        log.info("生成验证码");
        return captchaService.generateCaptcha();
    }

    /**
     * 获取验证码图片
     * @return 验证码图片和key
     */
    @GetMapping("")
    @Operation(summary = "获取验证码图片")
    public Result<CaptchaResponse> getCaptchaImage() {
        log.info("获取验证码图片");
        return captchaService.getCaptchaImage();
    }

    /**
     * 验证码响应数据
     */
    public static class CaptchaResponse {
        private String captchaKey;
        private String captchaImage;

        public CaptchaResponse(String captchaKey, String captchaImage) {
            this.captchaKey = captchaKey;
            this.captchaImage = captchaImage;
        }

        public String getCaptchaKey() {
            return captchaKey;
        }

        public void setCaptchaKey(String captchaKey) {
            this.captchaKey = captchaKey;
        }

        public String getCaptchaImage() {
            return captchaImage;
        }

        public void setCaptchaImage(String captchaImage) {
            this.captchaImage = captchaImage;
        }
    }
}
