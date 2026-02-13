package com.security.ratelimit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Validated
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {

    /** Enable/disable the rate limiter filter entirely. */
    private boolean enabled = true;

    /** Prefix for Redis keys (namespace). */
    @NotBlank
    private String redisKeyPrefix = "rl";

    @Valid
    private DefaultPolicy defaultPolicy = new DefaultPolicy();

    @Valid
    private List<RoutePolicy> routes = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRedisKeyPrefix() {
        return redisKeyPrefix;
    }

    public void setRedisKeyPrefix(String redisKeyPrefix) {
        this.redisKeyPrefix = redisKeyPrefix;
    }

    public DefaultPolicy getDefaultPolicy() {
        return defaultPolicy;
    }

    public void setDefaultPolicy(DefaultPolicy defaultPolicy) {
        this.defaultPolicy = defaultPolicy;
    }

    public List<RoutePolicy> getRoutes() {
        return routes;
    }

    public void setRoutes(List<RoutePolicy> routes) {
        this.routes = routes;
    }

    public static class DefaultPolicy {
        @NotNull
        private RateLimitStrategy strategy = RateLimitStrategy.IP;

        @Valid
        @NotNull
        private Limit limit = new Limit(100, Duration.ofMinutes(1));

        public RateLimitStrategy getStrategy() {
            return strategy;
        }

        public void setStrategy(RateLimitStrategy strategy) {
            this.strategy = strategy;
        }

        public Limit getLimit() {
            return limit;
        }

        public void setLimit(Limit limit) {
            this.limit = limit;
        }
    }

    public static class RoutePolicy {
        /** Stable identifier for tagging metrics. */
        @NotBlank
        private String id;

        /** Ant-style path pattern, e.g. /api/** */
        @NotBlank
        private String path;

        /** HTTP methods this policy applies to. If empty, applies to all. */
        private Set<String> methods;

        @NotNull
        private RateLimitStrategy strategy = RateLimitStrategy.IP;

        @Valid
        @NotNull
        private Limit limit;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public Set<String> getMethods() {
            return methods;
        }

        public void setMethods(Set<String> methods) {
            this.methods = methods;
        }

        public RateLimitStrategy getStrategy() {
            return strategy;
        }

        public void setStrategy(RateLimitStrategy strategy) {
            this.strategy = strategy;
        }

        public Limit getLimit() {
            return limit;
        }

        public void setLimit(Limit limit) {
            this.limit = limit;
        }
    }

    public static class Limit {
        /** Max requests in the refill period. */
        private long capacity;

        /** Refill period (classic fixed window-ish token bucket). */
        @NotNull
        private Duration period;

        public Limit() {
        }

        public Limit(long capacity, Duration period) {
            this.capacity = capacity;
            this.period = period;
        }

        public long getCapacity() {
            return capacity;
        }

        public void setCapacity(long capacity) {
            this.capacity = capacity;
        }

        public Duration getPeriod() {
            return period;
        }

        public void setPeriod(Duration period) {
            this.period = period;
        }
    }
}
