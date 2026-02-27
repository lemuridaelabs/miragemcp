package com.lemuridaelabs.miragemcp;

import com.lemuridaelabs.miragemcp.filters.RequestResponseCachingFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class RequestResponseCachingFilterTest {

    @Test
    void wrapsRequestAndResponseForNormalPaths() throws Exception {
        // Normal paths should be wrapped to enable request/response body capture.
        var filter = new RequestResponseCachingFilter();
        var request = new MockHttpServletRequest("GET", "/api/test");
        var response = new MockHttpServletResponse();

        var chain = new CapturingFilterChain();
        filter.doFilter(request, response, chain);

        assertThat(chain.capturedRequest).isInstanceOf(ContentCachingRequestWrapper.class);
        assertThat(response.getContentAsString()).isEqualTo("ok");
    }

    @Test
    void skipsWrappingForSsePaths() throws Exception {
        // SSE paths must not be wrapped to preserve streaming behavior.
        var filter = new RequestResponseCachingFilter();
        var request = new MockHttpServletRequest("GET", "/sse/stream");
        var response = new MockHttpServletResponse();

        var chain = new CapturingFilterChain();
        filter.doFilter(request, response, chain);

        assertThat(chain.capturedRequest).isSameAs(request);
        assertThat(response.getContentAsString()).isEqualTo("ok");
    }

    private static class CapturingFilterChain implements FilterChain {
        private ServletRequest capturedRequest;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) throws IOException {
            this.capturedRequest = request;
            response.getWriter().write("ok");
        }
    }
}
