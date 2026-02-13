package com.security.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitKeyResolverTest {

    private final RateLimitKeyResolver resolver = new RateLimitKeyResolver();

    @Test
    void resolvesIpFromXForwardedForFirstHop() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");
        req.setRemoteAddr("127.0.0.1");

        assertThat(resolver.resolveKey(RateLimitStrategy.IP, req)).isEqualTo("203.0.113.10");
    }

    @Test
    void resolvesApiKeyOrAnonymous() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        assertThat(resolver.resolveKey(RateLimitStrategy.API_KEY, req)).isEqualTo("anonymous");

        req.addHeader(RateLimitKeyResolver.DEFAULT_API_KEY_HEADER, " abc ");
        assertThat(resolver.resolveKey(RateLimitStrategy.API_KEY, req)).isEqualTo("abc");
    }
}
