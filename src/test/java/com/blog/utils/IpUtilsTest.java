package com.blog.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IpUtils 客户端 IP 信任序测试")
class IpUtilsTest {

    @Test
    @DisplayName("X-Real-IP 优先于 X-Forwarded-For")
    void shouldPreferXRealIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "203.0.113.9");
        request.addHeader("X-Forwarded-For", "192.168.1.1, 10.0.0.1");
        request.setRemoteAddr("127.0.0.1");

        assertThat(IpUtils.getClientIp(request)).isEqualTo("203.0.113.9");
    }

    @Test
    @DisplayName("无 X-Real-IP 时取 X-Forwarded-For 最右值，客户端伪造前缀不生效")
    void shouldTakeRightmostForwardedForIgnoringForgedPrefix() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "1.2.3.4, 10.0.0.1");
        request.setRemoteAddr("127.0.0.1");

        assertThat(IpUtils.getClientIp(request)).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("X-Forwarded-For 最右为 unknown 时继续向左取有效值")
    void shouldSkipUnknownAtRightmostForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "1.2.3.4, unknown");
        request.setRemoteAddr("127.0.0.1");

        assertThat(IpUtils.getClientIp(request)).isEqualTo("1.2.3.4");
    }

    @Test
    @DisplayName("头缺失或为 unknown 时回退 remoteAddr")
    void shouldFallbackToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "unknown");
        request.addHeader("X-Forwarded-For", "unknown");
        request.setRemoteAddr("127.0.0.1");

        assertThat(IpUtils.getClientIp(request)).isEqualTo("127.0.0.1");
    }

    @Test
    @DisplayName("纯客户端可控头不再被信任")
    void shouldNotTrustClientControlledHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Proxy-Client-IP", "1.2.3.4");
        request.addHeader("WL-Proxy-Client-IP", "1.2.3.4");
        request.addHeader("HTTP_CLIENT_IP", "1.2.3.4");
        request.addHeader("HTTP_X_FORWARDED_FOR", "1.2.3.4");
        request.setRemoteAddr("127.0.0.1");

        assertThat(IpUtils.getClientIp(request)).isEqualTo("127.0.0.1");
    }
}