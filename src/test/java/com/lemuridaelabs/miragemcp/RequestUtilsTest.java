package com.lemuridaelabs.miragemcp;

import com.lemuridaelabs.miragemcp.utils.RequestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class RequestUtilsTest {

    @Test
    void prefersCloudflareHeader() {
        // CF-Connecting-IP should take precedence when present.
        var request = new MockHttpServletRequest();
        request.addHeader("CF-Connecting-IP", "203.0.113.10");
        request.addHeader("X-Forwarded-For", "198.51.100.5");
        request.setRemoteAddr("192.0.2.1");

        assertThat(RequestUtils.getEffectiveRemoteIp(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void fallsBackToXForwardedFor() {
        // X-Forwarded-For should return the first IP in the list.
        var request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "198.51.100.5, 198.51.100.6");
        request.setRemoteAddr("192.0.2.1");

        assertThat(RequestUtils.getEffectiveRemoteIp(request)).isEqualTo("198.51.100.5");
    }

    @Test
    void fallsBackToRemoteAddr() {
        // Without proxy headers, the remote address should be used.
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.1");

        assertThat(RequestUtils.getEffectiveRemoteIp(request)).isEqualTo("192.0.2.1");
    }
}
