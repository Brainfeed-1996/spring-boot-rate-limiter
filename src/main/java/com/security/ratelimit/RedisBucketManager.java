package com.security.ratelimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisBucketManager {

    private final ProxyManager<byte[]> proxyManager;
    private final RateLimiterProperties properties;

    public RedisBucketManager(ProxyManager<byte[]> proxyManager, RateLimiterProperties properties) {
        this.proxyManager = proxyManager;
        this.properties = properties;
    }

    public ConsumptionProbe tryConsume(String routeId, String key, RateLimiterProperties.Limit limit) {
        String redisKey = properties.getRedisKeyPrefix() + ":" + routeId + ":" + key;
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(bandwidth(limit))
                .build();

        byte[] keyBytes = redisKey.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Bucket bucket = proxyManager.builder().build(keyBytes, configuration);
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    private io.github.bucket4j.Bandwidth bandwidth(RateLimiterProperties.Limit limit) {
        long capacity = limit.getCapacity();
        Duration period = limit.getPeriod();
        // Refill capacity tokens every period (classic token bucket).
        return io.github.bucket4j.Bandwidth.builder()
                .capacity(capacity)
                .refillIntervally(capacity, period)
                .build();
    }
}
