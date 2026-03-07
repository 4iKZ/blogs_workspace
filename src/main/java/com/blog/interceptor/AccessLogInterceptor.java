package com.blog.interceptor;

import com.blog.entity.WebsiteAccessLog;
import com.blog.mapper.WebsiteAccessLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 访问日志拦截器
 * 在每次请求完成后异步将访问信息写入 website_access_log 表
 */
@Slf4j
@Component
public class AccessLogInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTR = "accessLogStartTime";

    @Autowired
    private WebsiteAccessLogMapper websiteAccessLogMapper;

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

            WebsiteAccessLog log = buildLog(request, response, responseTime);
            saveLogAsync(log);
        } catch (Exception e) {
            log.warn("记录访问日志失败，不影响主流程: {}", e.getMessage());
        }
    }

    private WebsiteAccessLog buildLog(HttpServletRequest request, HttpServletResponse response, long responseTime) {
        LocalDateTime now = LocalDateTime.now();

        WebsiteAccessLog accessLog = new WebsiteAccessLog();
        accessLog.setAccessDate(LocalDate.now().toString());
        accessLog.setAccessTime(now);
        accessLog.setIpAddress(getClientIp(request));
        accessLog.setUserAgent(truncate(request.getHeader("User-Agent"), 500));
        accessLog.setRequestUrl(truncate(request.getRequestURI(), 500));
        accessLog.setPageUrl(truncate(request.getHeader("Referer"), 500));
        accessLog.setRequestMethod(request.getMethod());
        accessLog.setResponseStatus(response.getStatus());
        accessLog.setResponseTime(responseTime);
        accessLog.setReferer(truncate(request.getHeader("Referer"), 500));
        accessLog.setSessionId(request.getSession(false) != null ? request.getSession(false).getId() : null);

        Object userId = request.getAttribute("userId");
        if (userId instanceof Long) {
            accessLog.setUserId((Long) userId);
        }

        return accessLog;
    }

    @Async
    public void saveLogAsync(WebsiteAccessLog accessLog) {
        try {
            websiteAccessLogMapper.insert(accessLog);
        } catch (Exception e) {
            log.error("异步写入访问日志失败: {}", e.getMessage());
        }
    }

    /**
     * 获取真实客户端 IP（兼容反向代理）
     */
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
