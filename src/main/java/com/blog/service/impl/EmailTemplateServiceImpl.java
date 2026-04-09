package com.blog.service.impl;

import com.blog.service.EmailTemplateService;
import org.springframework.stereotype.Service;

/**
 * 邮件模板服务实现类
 * 使用专业的前端设计理念，创建美观、响应式的 HTML 邮件模板
 */
@Service
public class EmailTemplateServiceImpl implements EmailTemplateService {

    // 可配置的品牌常量
    private static final String WEBSITE_NAME = "Lumina";
    private static final String WEBSITE_LOGO_URL = "https://syhaox.tos-cn-beijing.volces.com/old_book_system/Gemini_Generated_Image_m6sivrm6sivrm6si_compressed.jpg";
    private static final String PRIMARY_COLOR = "#409EFF";
    private static final String GRADIENT_START = "#3b82f6";
    private static final String GRADIENT_END = "#2563eb";

    @Override
    public String getRegisterVerifyCodeEmailHtml(String verifyCode, long expireMinutes) {
        return buildEmailHtml(
            "欢迎注册 Lumina",
            "感谢您注册 Lumina！请使用以下验证码完成邮箱验证：",
            verifyCode,
            expireMinutes,
            "如果这不是您本人的操作，请忽略此邮件。"
        );
    }

    @Override
    public String getResetPasswordEmailHtml(String verifyCode, long expireMinutes) {
        return buildEmailHtml(
            "Lumina 密码重置",
            "您正在重置密码，请使用以下验证码完成操作：",
            verifyCode,
            expireMinutes,
            "如果这不是您本人的操作，请忽略此邮件。"
        );
    }

    @Override
    public String getWelcomeEmailHtml(String username) {
        String displayName = (username == null || username.trim().isEmpty()) ? "朋友" : username.trim();
        String safeUsername = escapeHtml(displayName);
        String contentHtml = String.format(
            "<p class=\"message\">亲爱的 <strong>%s</strong>，欢迎加入 Lumina！</p>" +
            "<p class=\"message\">我们非常高兴能与您一起开启这段创作与探索之旅。在这里，您可以自由地记录思考、分享知识，并与志同道合的朋友交流。</p>" +
            "<div style=\"text-align: center; margin: 40px 0;\">" +
            "  <a href=\"https://luminablog.cn\" style=\"background: linear-gradient(135deg, %s 0%%, %s 100%%); color: #ffffff; padding: 14px 32px; text-decoration: none; border-radius: 8px; font-size: 16px; font-weight: 600; display: inline-block; box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);\">" +
            "    开启创作之旅" +
            "  </a>" +
            "</div>" +
            "<p class=\"footer-note\">如果您有任何问题或建议，欢迎随时与我们联系。</p>",
            safeUsername, GRADIENT_START, GRADIENT_END
        );
        
        return buildBaseEmailHtml("欢迎加入 Lumina", contentHtml);
    }

    /**
     * 构建基础的 HTML 邮件模板框架
     * 包含完整的样式、头部和底部设计
     */
    private String buildBaseEmailHtml(String title, String contentHtml) {
        return String.format(
            "<!DOCTYPE html>" +
            "<html lang=\"zh-CN\" xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:v=\"urn:schemas-microsoft-com:vml\" xmlns:o=\"urn:schemas-microsoft-com:office:office\">" +
            "<head>" +
            "  <meta charset=\"UTF-8\">" +
            "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
            "  <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">" +
            "  <title>%s</title>" +
            "  <!--[if mso]>" +
            "  <noscript>" +
            "    <xml>" +
            "      <o:OfficeDocumentSettings>" +
            "        <o:PixelsPerInch>96</o:PixelsPerInch>" +
            "      </o:OfficeDocumentSettings>" +
            "    </xml>" +
            "  </noscript>" +
            "  <![endif]-->" +
            "  <style>" +
            "    body { margin: 0; padding: 0; background-color: #f5f7fa; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Helvetica Neue', 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif; -webkit-text-size-adjust: 100%%; -ms-text-size-adjust: 100%%; }" +
            "    table { border-collapse: collapse; mso-table-lspace: 0pt; mso-table-rspace: 0pt; }" +
            "    img { border: 0; height: auto; line-height: 100%%; outline: none; text-decoration: none; -ms-interpolation-mode: bicubic; }" +
            "    .wrapper { width: 100%%; table-layout: fixed; background-color: #f5f7fa; padding-bottom: 40px; }" +
            "    .main-table { background-color: #ffffff; border-radius: 12px; box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1); overflow: hidden; margin: 0 auto; max-width: 600px; width: 100%%; }" +
            "    .header { background: linear-gradient(135deg, %s 0%%, %s 100%%); padding: 48px 30px; text-align: center; }" +
            "    .logo-container { width: 80px; height: 80px; margin: 0 auto 20px; background-color: #ffffff; border-radius: 50%%; display: flex; align-items: center; justify-content: center; overflow: hidden; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15); }" +
            "    .logo-image { width: 100%%; height: 100%%; object-fit: cover; }" +
            "    .header-title { color: #ffffff; margin: 0; font-size: 28px; font-weight: 600; }" +
            "    .content { padding: 40px 30px; }" +
            "    .message { color: #333333; font-size: 16px; line-height: 1.6; margin: 0 0 30px 0; }" +
            "    .verify-code-container { text-align: center; margin: 35px 0; }" +
            "    .verify-code-box { display: inline-block; background: linear-gradient(135deg, %s 0%%, %s 100%%); padding: 4px; border-radius: 12px; }" +
            "    .verify-code-inner { background-color: #ffffff; padding: 28px 48px; border-radius: 10px; }" +
            "    .verify-code { font-size: 38px; font-weight: bold; color: %s; letter-spacing: 10px; font-family: 'Courier New', 'Consolas', monospace; }" +
            "    .expire-info { text-align: center; color: #999999; font-size: 14px; margin: 25px 0 0 0; }" +
            "    .expire-info strong { color: %s; }" +
            "    .footer-note { color: #999999; font-size: 13px; line-height: 1.6; margin: 35px 0 0 0; padding-top: 25px; border-top: 1px solid #eeeeee; }" +
            "    .footer { background-color: #f9fafb; padding: 25px 30px; text-align: center; border-top: 1px solid #eeeeee; }" +
            "    .footer-text { color: #999999; font-size: 12px; margin: 0; }" +
            "    @media only screen and (max-width: 620px) {" +
            "      .main-table { width: 100%% !important; border-radius: 0 !important; }" +
            "      .header { padding: 35px 20px !important; }" +
            "      .content { padding: 30px 20px !important; }" +
            "      .verify-code { font-size: 28px !important; letter-spacing: 6px !important; }" +
            "      .verify-code-inner { padding: 20px 30px !important; }" +
            "    }" +
            "  </style>" +
            "</head>" +
            "<body>" +
            "  <div class=\"wrapper\">" +
            "    <table role=\"presentation\" class=\"main-table\">" +
            "      <!-- 头部 -->" +
            "      <tr>" +
            "        <td class=\"header\">" +
            "          <div class=\"logo-container\">" +
            "            <img src=\"%s\" alt=\"%s\" class=\"logo-image\" />" +
            "          </div>" +
            "          <h1 class=\"header-title\">%s</h1>" +
            "        </td>" +
            "      </tr>" +
            "      <!-- 内容 -->" +
            "      <tr>" +
            "        <td class=\"content\">" +
            "          %s" +
            "        </td>" +
            "      </tr>" +
            "      <!-- 底部 -->" +
            "      <tr>" +
            "        <td class=\"footer\">" +
            "          <p class=\"footer-text\">&copy; %d %s. All rights reserved.</p>" +
            "        </td>" +
            "      </tr>" +
            "    </table>" +
            "  </div>" +
            "</body>" +
            "</html>",
            title,
            GRADIENT_START,
            GRADIENT_END,
            GRADIENT_START,
            GRADIENT_END,
            PRIMARY_COLOR,
            PRIMARY_COLOR,
            WEBSITE_LOGO_URL,
            WEBSITE_NAME,
            title,
            contentHtml,
            java.time.Year.now().getValue(),
            WEBSITE_NAME
        );
    }

    /**
     * 构建带有验证码的 HTML 邮件模板
     */
    private String buildEmailHtml(String title, String message, String verifyCode, 
                                   long expireMinutes, String footerNote) {
        String contentHtml = String.format(
            "<p class=\"message\">%s</p>" +
            "<div class=\"verify-code-container\">" +
            "  <div class=\"verify-code-box\">" +
            "    <div class=\"verify-code-inner\">" +
            "      <span class=\"verify-code\">%s</span>" +
            "    </div>" +
            "  </div>" +
            "</div>" +
            "<p class=\"expire-info\">验证码有效期：<strong>%d 分钟</strong></p>" +
            "<p class=\"footer-note\">%s</p>",
            message, verifyCode, expireMinutes, footerNote
        );
        return buildBaseEmailHtml(title, contentHtml);
    }

    private String escapeHtml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
