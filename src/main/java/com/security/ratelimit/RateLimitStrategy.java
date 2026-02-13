package com.security.ratelimit;

public enum RateLimitStrategy {
    /** Keyed by client IP (supports X-Forwarded-For). */
    IP,
    /** Keyed by API key header value (default: X-Api-Key). */
    API_KEY,
    /** Single global bucket shared by everyone. */
    GLOBAL
}
