package com.blog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * 配置跨域资源共享(CORS)
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 配置跨域资源共享
     * @param registry CORS注册表
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            // 允许所有来源，生产环境应配置具体的域名
            .allowedOrigins("*")
            // 允许的HTTP方法
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            // 允许的HTTP头
            .allowedHeaders("*")
            // 允许发送Cookie，生产环境应根据需求配置
            .allowCredentials(false)
            // 预检请求的有效期（秒）
            .maxAge(3600);
    }
}
