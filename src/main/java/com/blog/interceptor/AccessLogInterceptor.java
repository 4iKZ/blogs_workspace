package com.blog.interceptor;

import com.blog.entity.WebsiteAccessLog;
import com.blog.service.AccessLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import ua_parser.Client;
import ua_parser.Parser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 访问日志拦截器
 * preHandle 记录请求开始时间，afterCompletion 委托 AccessLogService 异步写入 website_access_log
 */
@Slf4j
@Component
public class AccessLogInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTR = "accessLogStartTime";

    private final Parser uaParser = new Parser();

    @Autowired
    private AccessLogService accessLogService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        try {
            Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
            long responseTime = startTime != null ? System.currentTimeMillis() - startTime : 0L;

            WebsiteAccessLog accessLog = buildLog(request, response, responseTime);
            accessLogService.saveAsync(accessLog);
        } catch (Exception e) {
            log.warn("构建访问日志失败，不影响主流程: {}", e.getMessage());
        }
    }

    private WebsiteAccessLog buildLog(HttpServletRequest request, HttpServletResponse response, long responseTime) {
        String uaString = request.getHeader("User-Agent");

        WebsiteAccessLog accessLog = new WebsiteAccessLog();
        accessLog.setAccessDate(LocalDate.now().toString());
        accessLog.setAccessTime(LocalDateTime.now());
        accessLog.setIpAddress(getClientIp(request));
        accessLog.setUserAgent(truncate(uaString, 500));
        accessLog.setRequestUrl(truncate(request.getRequestURI(), 500));
        accessLog.setPageUrl(truncate(request.getHeader("Referer"), 500));
        accessLog.setRequestMethod(request.getMethod());
        accessLog.setResponseStatus(response.getStatus());
        accessLog.setResponseTime(responseTime);
        accessLog.setReferer(truncate(request.getHeader("Referer"), 500));

        jakarta.servlet.http.HttpSession session = request.getSession(false);
        accessLog.setSessionId(session != null ? session.getId() : null);

        Object userId = request.getAttribute("userId");
        if (userId instanceof Long) {
            accessLog.setUserId((Long) userId);
        }

        // 解析 User-Agent → 设备类型 / 浏览器 / 操作系统
        parseUserAgent(uaString, accessLog);

        return accessLog;
    }

    /**
     * 使用 ua-parser 解析 User-Agent 字符串，填充 deviceType、browser、operatingSystem 字段
     */
    private void parseUserAgent(String uaString, WebsiteAccessLog accessLog) {
        if (uaString == null || uaString.isEmpty()) {
            return;
        }
        try {
            Client client = uaParser.parse(uaString);

            // 浏览器：取 family（如 Chrome、Firefox、Safari）
            if (client.userAgent != null && client.userAgent.family != null) {
                accessLog.setBrowser(client.userAgent.family);
            }

            // 操作系统：取 family（如 Windows、Mac OS X、Linux、Android、iOS）
            if (client.os != null && client.os.family != null) {
                accessLog.setOperatingSystem(client.os.family);
            }

            // 设备类型：ua-parser 的 device.family 返回具体设备名（如 iPhone、Spider 等），
            // 需要映射到 desktop / mobile / tablet 三分类
            accessLog.setDeviceType(inferDeviceType(uaString, client));
        } catch (Exception e) {
            log.debug("UA 解析失败，跳过: {}", e.getMessage());
        }
    }

    /**
     * 根据 UA 信息推断设备类型（desktop / mobile / tablet）
     */
    private String inferDeviceType(String uaString, Client client) {
        String ua = uaString.toLowerCase();

        // tablet 特征词优先判定
        if (ua.contains("ipad") || ua.contains("tablet") || ua.contains("kindle")
                || ua.contains("silk") || ua.contains("playbook")) {
            return "tablet";
        }

        // mobile 特征词
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")
                || ua.contains("ipod") || ua.contains("windows phone")
                || ua.contains("blackberry") || ua.contains("opera mini")
                || ua.contains("opera mobi")) {
            // Android 平板通常不含 "Mobile"，但走到这里说明已含 mobile 关键词
            return "mobile";
        }

        // 爬虫 / Bot
        if (client.device != null && "Spider".equalsIgnoreCase(client.device.family)) {
            return "bot";
        }

        return "desktop";
    }

    private String getClientIp(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR", "X-Real-IP"
        };
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
