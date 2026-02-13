package com.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RateLimitKeyResolver {

    public static final String DEFAULT_API_KEY_HEADER = "X-Api-Key";

    public String resolveKey(RateLimitStrategy strategy, HttpServletRequest request) {
        return switch (strategy) {
            case GLOBAL -> "global";
            case API_KEY -> resolveApiKey(request).orElse("anonymous");
            case IP -> resolveClientIp(request).orElse("unknown");
        };
    }

    private Optional<String> resolveApiKey(HttpServletRequest request) {
        String value = request.getHeader(DEFAULT_API_KEY_HEADER);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.trim());
    }

    /**
     * Attempts to honor X-Forwarded-For (first IP) when behind a proxy.
     * In real deployments, ensure your reverse proxy sanitizes this header.
     */
    private Optional<String> resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String first = xff.split(",")[0].trim();
            if (!first.isBlank()) {
                return Optional.of(first);
            }
        }
        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(remoteAddr);
    }
}
