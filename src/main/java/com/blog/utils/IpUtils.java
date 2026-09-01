package com.blog.utils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端 IP 获取工具
 * 统一处理反向代理场景下的 IP 提取，供统计、登录、访问日志等模块复用
 */
public final class IpUtils {

    private static final String UNKNOWN = "unknown";

    private IpUtils() {
    }

    /**
     * 获取客户端真实 IP
     * 依次尝试常见代理头，多级代理时取第一个 IP，最终回退到 remoteAddr
     *
     * @param request 当前 HTTP 请求
     * @return 客户端 IP
     */
    public static String getClientIp(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP",
                "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"
        };
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !UNKNOWN.equalsIgnoreCase(ip)) {
                return extractFirstIp(ip);
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * 多级代理时 X-Forwarded-For 可能包含逗号分隔的 IP 链，取第一个（最原始客户端）
     */
    private static String extractFirstIp(String ip) {
        if (ip.contains(",")) {
            return ip.substring(0, ip.indexOf(',')).trim();
        }
        return ip.trim();
    }
}
