package com.blog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * 审计配置类
 * 启用安全审计功能
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class AuditConfig {
    // 安全审计功能已经通过EnableMethodSecurity启用
    // 可以根据需要添加更多审计相关的配置
    // 例如：配置审计事件仓库、审计监听器等
    // 简单起见，这里只启用基础的安全审计功能
}
