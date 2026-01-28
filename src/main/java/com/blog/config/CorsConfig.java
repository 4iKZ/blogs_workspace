package com.blog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置类
 */
@Configuration
public class CorsConfig {

    /**
     * 允许跨域请求的过滤器
     */
    @Bean
    public CorsFilter corsFilter() {
        // 创建CORS配置对象
        CorsConfiguration config = new CorsConfiguration();
        // 允许所有域名进行跨域调用
        config.addAllowedOriginPattern("*");
        // 允许跨越发送cookie
        config.setAllowCredentials(true);
        // 允许所有请求方法
        config.addAllowedMethod("*");
        // 允许所有请求头
        config.addAllowedHeader("*");
        // 暴露额外的响应头
        config.addExposedHeader("Authorization");
        config.addExposedHeader("Refresh-Token");
        config.addExposedHeader("X-Total-Count");
        config.addExposedHeader("Content-Length");
        config.addExposedHeader("Content-Disposition");

        // 添加映射路径，拦截一切请求
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        // 返回新的CorsFilter
        return new CorsFilter(source);
    }
}