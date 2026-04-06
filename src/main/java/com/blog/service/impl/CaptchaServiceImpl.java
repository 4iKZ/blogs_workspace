package com.blog.service.impl;

import com.blog.common.Result;
import com.blog.controller.CaptchaController.CaptchaResponse;
import com.blog.service.CaptchaService;
import com.blog.utils.RedisUtils;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

/**
 * 验证码服务实现类（使用 Kaptcha）
 */
@Service
@Slf4j
public class CaptchaServiceImpl implements CaptchaService {

    @Autowired
    private RedisUtils redisUtils;
    
    @Autowired
    private DefaultKaptcha captchaProducer;  // 注入 Kaptcha 生成器
    
    @Override
    public Result<String> generateCaptcha() {
        // 使用 Kaptcha 生成验证码文本
        String captcha = captchaProducer.createText();
        
        // 生成 UUID 作为验证码 key
        String captchaKey = UUID.randomUUID().toString();
        
        // 存储验证码到 Redis，设置 5 分钟过期时间
        redisUtils.set("captcha:" + captchaKey, captcha, 5, TimeUnit.MINUTES);
        
        log.info("生成 Kaptcha 验证码：key={}", captchaKey);
        return Result.success(captchaKey);
    }
    
    @Override
    public Result<CaptchaResponse> getCaptchaImage() {
        // 使用 Kaptcha 生成验证码文本
        String captcha = captchaProducer.createText();
        
        // 生成 UUID 作为验证码 key
        String captchaKey = UUID.randomUUID().toString();
        
        // 存储验证码到 Redis，设置 5 分钟过期时间
        redisUtils.set("captcha:" + captchaKey, captcha, 5, TimeUnit.MINUTES);
        
        // 使用 Kaptcha 生成验证码图片
        BufferedImage image = captchaProducer.createImage(captcha);
        String captchaImage = convertToBase64(image);
        
        log.info("生成 Kaptcha 验证码图片：key={}", captchaKey);
        
        // 返回验证码 key 和图片
        CaptchaResponse response = new CaptchaResponse(captchaKey, captchaImage);
        return Result.success(response);
    }
    
    @Override
    public boolean verifyCaptcha(String captchaKey, String captcha) {
        if (captchaKey == null || captcha == null) {
            return false;
        }
        
        // 从 Redis 获取验证码
        String storedCaptcha = redisUtils.get("captcha:" + captchaKey);
        if (storedCaptcha != null && storedCaptcha.equalsIgnoreCase(captcha)) {
            // 验证成功后删除验证码（一次性使用）
            redisUtils.delete("captcha:" + captchaKey);
            return true;
        }
        
        return false;
    }
    
    /**
     * 将 BufferedImage 转换为 Base64
     */
    private String convertToBase64(BufferedImage image) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            log.error("转换验证码图片为 Base64 失败", e);
            throw new RuntimeException("生成验证码图片失败", e);
        }
    }
}
