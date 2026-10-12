package cn.edu.teamtoolbox.security;

import cn.edu.teamtoolbox.transfer.PickupRateLimitFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTests {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void authLimitIsSeparatedByClientIp() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(objectMapper, 1);
        AtomicInteger passed = new AtomicInteger();

        MockHttpServletResponse first = invoke(filter, "POST", "/api/v1/auth/login", "198.51.100.10", passed);
        MockHttpServletResponse blocked = invoke(filter, "POST", "/api/v1/auth/login", "198.51.100.10", passed);
        MockHttpServletResponse otherClient = invoke(filter, "POST", "/api/v1/auth/login", "198.51.100.11", passed);

        assertThat(first.getStatus()).isEqualTo(200);
        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getContentAsString()).contains("RATE_LIMITED");
        assertThat(otherClient.getStatus()).isEqualTo(200);
        assertThat(passed).hasValue(2);
    }

    @Test
    void pickupLimitIsSeparatedByCodeAndClientIp() throws Exception {
        PickupRateLimitFilter filter = new PickupRateLimitFilter(objectMapper, 1);
        AtomicInteger passed = new AtomicInteger();

        MockHttpServletResponse first = invoke(filter, "GET", "/api/v1/pickup/CODE-A", "203.0.113.10", passed);
        MockHttpServletResponse sameCodeBlocked = invoke(filter, "GET", "/api/v1/pickup/CODE-A", "203.0.113.10", passed);
        MockHttpServletResponse otherCode = invoke(filter, "GET", "/api/v1/pickup/CODE-B", "203.0.113.10", passed);
        MockHttpServletResponse otherClient = invoke(filter, "GET", "/api/v1/pickup/CODE-A", "203.0.113.11", passed);

        assertThat(first.getStatus()).isEqualTo(200);
        assertThat(sameCodeBlocked.getStatus()).isEqualTo(429);
        assertThat(sameCodeBlocked.getContentAsString()).contains("RATE_LIMITED");
        assertThat(otherCode.getStatus()).isEqualTo(200);
        assertThat(otherClient.getStatus()).isEqualTo(200);
        assertThat(passed).hasValue(3);
    }

    private MockHttpServletResponse invoke(
            jakarta.servlet.Filter filter,
            String method,
            String uri,
            String realIp,
            AtomicInteger passed) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.addHeader("X-Real-IP", realIp);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> passed.incrementAndGet());
        return response;
    }
}
