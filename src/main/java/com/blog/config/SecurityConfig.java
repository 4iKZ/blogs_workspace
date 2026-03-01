package com.blog.config;

import com.blog.security.CustomAuthenticationEntryPoint;
import com.blog.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF保护，因为我们使用JWT
            .csrf(csrf -> csrf.disable())
            // 不创建会话
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 配置细粒度访问控制
            .authorizeHttpRequests(authorize -> authorize
                // 公开端点
                .requestMatchers("/api/user/register", "/api/user/login", "/api/user/refresh-token", "/api/user/top-authors", "/api/captcha/**", "/api/user/avatar/upload").permitAll()
                // 公开API - 文章相关
                .requestMatchers("/api/article/list", "/api/article/{id}", "/api/article/hot", "/api/article/recommended").permitAll()
                // 公开API - 分类和标签
                .requestMatchers("/api/category/**", "/api/tag/**").permitAll()
                // 公开API - 搜索
                .requestMatchers("/api/search/**").permitAll()
                // 公开API - 关于
            .requestMatchers("/api/about/**").permitAll()
            // 公开API - 评论相关
            .requestMatchers("/api/comment/list", "/api/comment/hot", "/api/comment/article/*/count").permitAll()
            .requestMatchers("/api/comment/check-sensitive", "/api/comment/replace-sensitive").permitAll()
            .requestMatchers("/api/comment/children", "/api/comment/*/like-status").permitAll()
            // 公开API - 统计相关（文章浏览量、热门/推荐/置顶文章等）
            .requestMatchers("/api/statistics/**").permitAll()
            // 需要认证的评论操作
            .requestMatchers("/api/comment", "/api/comment/*/like", "/api/comment/*/delete").authenticated()
            // 需要认证的用户相关API
            .requestMatchers("/api/user/info", "/api/user/profile", "/api/user/password").authenticated()
            // 需要认证的消息通知API
            .requestMatchers("/api/notification/**").authenticated()
                // 需要认证的文章操作
                .requestMatchers("/api/article/publish", "/api/article/edit/**", "/api/article/delete/**", "/api/article/upload-cover").authenticated()
                // 需要认证的互动操作
                .requestMatchers("/api/user/like/**", "/api/user/favorite/**", "/api/user/follow/**").authenticated()
                // 管理员API
                .requestMatchers("/api/admin/**").hasRole("admin")
                // 其他所有请求都需要认证
                .anyRequest().authenticated()
            )
            // 添加JWT认证过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // 配置自定义认证入口点，未认证时返回 401 而不是 403
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(customAuthenticationEntryPoint)
            )
            // 配置内容安全策略，防止XSS攻击
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'" +
                        "; script-src 'self'" +
                        "; style-src 'self'" +
                        "; img-src 'self' data:" +
                        "; connect-src 'self'" +
                        "; frame-ancestors 'self'" +
                        "; form-action 'self'" +
                        "; base-uri 'self'" +
                        "; object-src 'none'" +
                        "; upgrade-insecure-requests")
                )
                .xssProtection(xss -> xss.headerValue(org.springframework.security.web.header.writers.XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                // 移除strictTransportSecurity配置，使用默认设置
                .frameOptions(frame -> frame
                    .sameOrigin()
                )
            )
            // 禁用默认的表单登录
            .formLogin(form -> form.disable())
            // 禁用HTTP基本认证
            .httpBasic(basic -> basic.disable());
            
        return http.build();
    }
}