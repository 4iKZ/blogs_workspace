package com.blog.service;

/**
 * 邮件模板服务接口
 */
public interface EmailTemplateService {

    /**
     * 获取注册邮箱验证码邮件内容（HTML 格式）
     *
     * @param verifyCode 验证码
     * @param expireMinutes 有效期（分钟）
     * @return HTML 邮件内容
     */
    String getRegisterVerifyCodeEmailHtml(String verifyCode, long expireMinutes);

    /**
     * 获取重置密码验证码邮件内容（HTML 格式）
     *
     * @param verifyCode 验证码
     * @param expireMinutes 有效期（分钟）
     * @return HTML 邮件内容
     */
    String getResetPasswordEmailHtml(String verifyCode, long expireMinutes);
}