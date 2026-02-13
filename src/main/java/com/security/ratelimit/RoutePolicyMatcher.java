package com.security.ratelimit;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.Comparator;
import java.util.Optional;

@Component
public class RoutePolicyMatcher {

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public MatchedPolicy match(RateLimiterProperties properties, String path, String method) {
        Optional<RateLimiterProperties.RoutePolicy> route = properties.getRoutes().stream()
                .filter(r -> antPathMatcher.match(r.getPath(), path))
                .filter(r -> r.getMethods() == null || r.getMethods().isEmpty() || r.getMethods().contains(method))
                // Most specific first: longer pattern is usually more specific.
                .sorted(Comparator.comparingInt((RateLimiterProperties.RoutePolicy r) -> r.getPath().length()).reversed())
                .findFirst();

        if (route.isPresent()) {
            var rp = route.get();
            return new MatchedPolicy(rp.getId(), rp.getStrategy(), rp.getLimit());
        }

        var dp = properties.getDefaultPolicy();
        return new MatchedPolicy("default", dp.getStrategy(), dp.getLimit());
    }

    public record MatchedPolicy(String id, RateLimitStrategy strategy, RateLimiterProperties.Limit limit) {
        public HttpMethod httpMethod(String method) {
            return HttpMethod.valueOf(method);
        }
    }
}
