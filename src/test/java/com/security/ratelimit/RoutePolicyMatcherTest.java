package com.security.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RoutePolicyMatcherTest {

    private final RoutePolicyMatcher matcher = new RoutePolicyMatcher();

    @Test
    void matchesMostSpecificRoute() {
        RateLimiterProperties props = new RateLimiterProperties();

        RateLimiterProperties.RoutePolicy broad = new RateLimiterProperties.RoutePolicy();
        broad.setId("broad");
        broad.setPath("/api/**");
        broad.setMethods(Set.of("GET"));
        broad.setStrategy(RateLimitStrategy.IP);
        broad.setLimit(new RateLimiterProperties.Limit(100, Duration.ofMinutes(1)));

        RateLimiterProperties.RoutePolicy specific = new RateLimiterProperties.RoutePolicy();
        specific.setId("specific");
        specific.setPath("/api/hello");
        specific.setMethods(Set.of("GET"));
        specific.setStrategy(RateLimitStrategy.API_KEY);
        specific.setLimit(new RateLimiterProperties.Limit(1, Duration.ofSeconds(10)));

        props.setRoutes(List.of(broad, specific));

        var matched = matcher.match(props, "/api/hello", "GET");
        assertThat(matched.id()).isEqualTo("specific");
        assertThat(matched.strategy()).isEqualTo(RateLimitStrategy.API_KEY);
    }

    @Test
    void fallsBackToDefaultPolicy() {
        RateLimiterProperties props = new RateLimiterProperties();
        props.getDefaultPolicy().setStrategy(RateLimitStrategy.GLOBAL);
        props.getDefaultPolicy().setLimit(new RateLimiterProperties.Limit(10, Duration.ofSeconds(1)));

        var matched = matcher.match(props, "/nope", "GET");
        assertThat(matched.id()).isEqualTo("default");
        assertThat(matched.strategy()).isEqualTo(RateLimitStrategy.GLOBAL);
    }
}
