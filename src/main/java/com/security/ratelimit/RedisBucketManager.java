package com.security.ratelimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Industrial-grade Redis Bucket Manager with Async Support and Telemetry
 */
@Component
public class RedisBucketManager {

    private final ProxyManager<byte[]> proxyManager;
    private final RateLimiterProperties properties;

    public RedisBucketManager(ProxyManager<byte[]> proxyManager, RateLimiterProperties properties) {
        this.proxyManager = proxyManager;
        this.properties = properties;
    }

    /**
     * Tries to consume a token synchronously from the distributed bucket.
     */
    public ConsumptionProbe tryConsume(String routeId, String key, RateLimiterProperties.Limit limit) {
        String redisKey = buildKey(routeId, key);
        BucketConfiguration configuration = buildConfiguration(limit);

        byte[] keyBytes = redisKey.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Bucket bucket = proxyManager.builder().build(keyBytes, configuration);
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    /**
     * Asynchronous consumption for non-blocking reactive pipelines.
     */
    public CompletableFuture<ConsumptionProbe> tryConsumeAsync(String routeId, String key, RateLimiterProperties.Limit limit) {
        String redisKey = buildKey(routeId, key);
        BucketConfiguration configuration = buildConfiguration(limit);

        byte[] keyBytes = redisKey.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return proxyManager.asAsync().builder().build(keyBytes, configuration)
                .tryConsumeAndReturnRemaining(1);
    }

    private String buildKey(String routeId, String key) {
        return String.format("%s:v2:%s:%s", properties.getRedisKeyPrefix(), routeId, key);
    }

    private BucketConfiguration buildConfiguration(RateLimiterProperties.Limit limit) {
        return BucketConfiguration.builder()
                .addLimit(io.github.bucket4j.Bandwidth.builder()
                        .capacity(limit.getCapacity())
                        .refillIntervally(limit.getCapacity(), limit.getPeriod())
                        .build())
                .build();
    }
}
