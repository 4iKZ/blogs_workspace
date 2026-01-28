package com.blog.service.impl;

import com.blog.common.Result;
import com.blog.controller.CaptchaController.CaptchaResponse;
import com.blog.service.CaptchaService;
import com.blog.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

/**
 * 验证码服务实现类
 */
@Service
public class CaptchaServiceImpl implements CaptchaService {

    @Autowired
    private RedisUtils redisUtils;
    
    private final Random random = new Random();

    @Override
    public Result<String> generateCaptcha() {
        // 生成6位随机数字验证码
        String captcha = String.format("%06d", random.nextInt(1000000));

        // 生成UUID作为验证码key
        String captchaKey = UUID.randomUUID().toString();

        // 存储验证码到Redis，设置5分钟过期时间
        redisUtils.set("captcha:" + captchaKey, captcha, 5, TimeUnit.MINUTES);

        // 这里在实际应用中会返回图片或其他格式的验证码
        return Result.success(captchaKey);
    }

    @Override
    public Result<CaptchaResponse> getCaptchaImage() {
        // 生成4位随机数字验证码
        String captcha = String.format("%04d", random.nextInt(10000));

        // 生成UUID作为验证码key
        String captchaKey = UUID.randomUUID().toString();

        // 存储验证码到Redis，设置5分钟过期时间
        redisUtils.set("captcha:" + captchaKey, captcha, 5, TimeUnit.MINUTES);

        // 生成验证码图片
        String captchaImage = generateCaptchaImage(captcha);

        // 返回验证码key和图片
        CaptchaResponse response = new CaptchaResponse(captchaKey, captchaImage);
        return Result.success(response);
    }

    @Override
    public boolean verifyCaptcha(String captchaKey, String captcha) {
        if (captchaKey == null || captcha == null) {
            return false;
        }

        // 从Redis获取验证码
        String storedCaptcha = redisUtils.get("captcha:" + captchaKey);
        if (storedCaptcha != null && storedCaptcha.equals(captcha)) {
            // 验证成功后删除验证码（一次性使用）
            redisUtils.delete("captcha:" + captchaKey);
            return true;
        }

        return false;
    }

    /**
     * 生成验证码图片
     */
    private String generateCaptchaImage(String captcha) {
        try {
            // 创建图片
            int width = 120;
            int height = 40;
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();

            // 设置背景色
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);

            // 设置字体
            g.setFont(new Font("Arial", Font.BOLD, 20));

            // 绘制验证码
            g.setColor(Color.BLACK);
            for (int i = 0; i < captcha.length(); i++) {
                g.drawString(String.valueOf(captcha.charAt(i)), 20 + i * 20, 25);
            }

            // 添加干扰线
            g.setColor(Color.LIGHT_GRAY);
            for (int i = 0; i < 5; i++) {
                int x1 = random.nextInt(width);
                int y1 = random.nextInt(height);
                int x2 = random.nextInt(width);
                int y2 = random.nextInt(height);
                g.drawLine(x1, y1, x2, y2);
            }

            // 添加噪点
            for (int i = 0; i < 50; i++) {
                int x = random.nextInt(width);
                int y = random.nextInt(height);
                g.fillRect(x, y, 1, 1);
            }

            g.dispose();

            // 将图片转换为Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            throw new RuntimeException("生成验证码图片失败", e);
        }
    }
}