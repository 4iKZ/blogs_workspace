package com.blog.service;

import com.blog.common.Result;
import com.blog.controller.CaptchaController.CaptchaResponse;

/**
 * 验证码服务接口
 */
public interface CaptchaService {
    /**
     * 生成验证码
     */
    Result<String> generateCaptcha();

    /**
     * 获取验证码图片
     */
    Result<CaptchaResponse> getCaptchaImage();

    /**
     * 验证验证码
     */
    boolean verifyCaptcha(String captchaKey, String captcha);
}