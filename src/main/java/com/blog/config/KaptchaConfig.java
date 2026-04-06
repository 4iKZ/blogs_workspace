package com.blog.config;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * Kaptcha 验证码配置
 */
@Configuration
public class KaptchaConfig {
    
    @Bean
    public DefaultKaptcha captchaProducer() {
        Properties props = new Properties();
        
        // 无边框
        props.setProperty("kaptcha.border", "no");
        
        // 验证码字符集（字母 + 数字）
        props.setProperty("kaptcha.textproducer.char.string", 
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
        
        // 验证码长度
        props.setProperty("kaptcha.textproducer.char.length", "4");
        
        // 验证码字体颜色
        props.setProperty("kaptcha.textproducer.font.color", "black");
        
        // 验证码字体
        props.setProperty("kaptcha.textproducer.font.names", "Arial,Verdana");
        props.setProperty("kaptcha.textproducer.font.size", "38");
        
        // 图片背景（渐变）
        props.setProperty("kaptcha.background.clear.from", "lightGray");
        props.setProperty("kaptcha.background.clear.to", "white");
        
        // 干扰实现
        props.setProperty("kaptcha.noise.impl", 
            "com.google.code.kaptcha.impl.DefaultNoise");
        props.setProperty("kaptcha.noise.color", "blue");
        
        // 图片尺寸
        props.setProperty("kaptcha.image.width", "200");
        props.setProperty("kaptcha.image.height", "60");
        
        Config config = new Config(props);
        DefaultKaptcha kaptcha = new DefaultKaptcha();
        kaptcha.setConfig(config);
        
        return kaptcha;
    }
}
