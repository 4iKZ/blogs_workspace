package com.blog.service.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplateServiceImplTest {

    private final EmailTemplateServiceImpl service = new EmailTemplateServiceImpl();

    @Test
    void getRegisterVerifyCodeEmailHtml_shouldContainCodeAndExpiry() {
        String html = service.getRegisterVerifyCodeEmailHtml("123456", 10);

        assertThat(html).contains("123456");
        assertThat(html).contains("10 分钟");
        assertThat(html).contains("欢迎注册 Lumina");
        assertThat(html).contains("<!DOCTYPE html>");
    }

    @Test
    void getResetPasswordEmailHtml_shouldContainCodeAndExpiry() {
        String html = service.getResetPasswordEmailHtml("654321", 5);

        assertThat(html).contains("654321");
        assertThat(html).contains("5 分钟");
        assertThat(html).contains("Lumina 密码重置");
    }

    @Test
    void getWelcomeEmailHtml_shouldContainUsername() {
        String html = service.getWelcomeEmailHtml("Alice");

        assertThat(html).contains("Alice");
        assertThat(html).contains("欢迎加入 Lumina");
        assertThat(html).contains("开启创作之旅");
    }

    @Test
    void getWelcomeEmailHtml_null_shouldUseDefault() {
        String html = service.getWelcomeEmailHtml(null);

        assertThat(html).contains("朋友");
        assertThat(html).doesNotContain("null");
    }

    @Test
    void getWelcomeEmailHtml_specialChars_shouldBeEscaped() {
        String html = service.getWelcomeEmailHtml("<script>alert('xss')</script>");

        assertThat(html).contains("&lt;script&gt;");
        assertThat(html).contains("&lt;/script&gt;");
        assertThat(html).doesNotContain("<script>");
    }
}
