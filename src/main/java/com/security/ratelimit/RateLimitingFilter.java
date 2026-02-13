package com.security.ratelimit;

import io.github.bucket4j.ConsumptionProbe;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimiterProperties properties;
    private final RoutePolicyMatcher matcher;
    private final RateLimitKeyResolver keyResolver;
    private final RedisBucketManager bucketManager;
    private final MeterRegistry meterRegistry;

    public RateLimitingFilter(RateLimiterProperties properties,
                              RoutePolicyMatcher matcher,
                              RateLimitKeyResolver keyResolver,
                              RedisBucketManager bucketManager,
                              MeterRegistry meterRegistry) {
        this.properties = properties;
        this.matcher = matcher;
        this.keyResolver = keyResolver;
        this.bucketManager = bucketManager;
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.isEnabled();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        var policy = matcher.match(properties, path, method);
        String key = keyResolver.resolveKey(policy.strategy(), request);

        ConsumptionProbe probe = bucketManager.tryConsume(policy.id(), key, policy.limit());

        addStandardHeaders(response, policy.limit(), probe);

        if (probe.isConsumed()) {
            counter(policy.id(), "allowed").increment();
            filterChain.doFilter(request, response);
        } else {
            counter(policy.id(), "blocked").increment();
            long waitForRefillMs = probe.getNanosToWaitForRefill() / 1_000_000;
            long retryAfterSeconds = Math.max(1, waitForRefillMs / 1000);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"too_many_requests\",\"route\":\"" + policy.id() + "\"}");
        }
    }

    private void addStandardHeaders(HttpServletResponse response,
                                    RateLimiterProperties.Limit limit,
                                    ConsumptionProbe probe) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit.getCapacity()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, probe.getRemainingTokens())));
        long resetSeconds = Duration.ofNanos(probe.getNanosToWaitForReset()).toSeconds();
        response.setHeader("X-RateLimit-Reset", String.valueOf(Math.max(0, resetSeconds)));
    }

    private Counter counter(String routeId, String outcome) {
        return Counter.builder("http.rate_limit.requests")
                .tag("route", routeId)
                .tag("outcome", outcome)
                .register(meterRegistry);
    }
}
