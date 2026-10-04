package com.blog.utils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端 IP 获取工具
 * 统一处理反向代理场景下的 IP 提取，供统计、登录、访问日志等模块复用
 */
public final class IpUtils {

    private static final String UNKNOWN = "unknown";
    private static final String X_REAL_IP = "X-Real-IP";
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    private IpUtils() {
    }

    /**
     * 获取客户端真实 IP
     * 信任顺序：X-Real-IP（Nginx 覆盖式写入，客户端无法伪造）
     * → X-Forwarded-For 最右值（Nginx 追加式写入，最近一跳最可信）
     * → remoteAddr。
     *
     * ponytail: 仅适配单层 Nginx 反代部署（见仓库根 nginx.conf）。
     * 多级代理如需精确信任链，应引入可信网段逐跳解析，当前刻意保持最小实现。
     *
     * @param request 当前 HTTP 请求
     * @return 客户端 IP
     */
    public static String getClientIp(HttpServletRequest request) {
        String realIp = trimToNull(request.getHeader(X_REAL_IP));
        if (realIp != null && !UNKNOWN.equalsIgnoreCase(realIp)) {
            return realIp;
        }
        String forwarded = extractLastIp(request.getHeader(X_FORWARDED_FOR));
        if (forwarded != null) {
            return forwarded;
        }
        return request.getRemoteAddr();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * X-Forwarded-For 是逗号分隔的 IP 链，客户端可伪造最左前缀，
     * 因此取最右非空、非 unknown 的值（追加语义下最近一跳最可信）
     */
    private static String extractLastIp(String header) {
        if (header == null || header.isEmpty()) {
            return null;
        }
        String[] parts = header.split(",");
        for (int i = parts.length - 1; i >= 0; i--) {
            String candidate = parts[i].trim();
            if (!candidate.isEmpty() && !UNKNOWN.equalsIgnoreCase(candidate)) {
                return candidate;
            }
        }
        return null;
    }
}