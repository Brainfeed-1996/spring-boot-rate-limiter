package com.security.ratelimit;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
@EnableConfigurationProperties(RateLimiterProperties.class)
@ConditionalOnProperty(prefix = "rate-limiter", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateLimiterAutoConfiguration {

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    public RedisClient bucket4jRedisClient(LettuceConnectionFactory connectionFactory) {
        RedisStandaloneConfiguration standalone = connectionFactory.getStandaloneConfiguration();
        if (standalone == null) {
            throw new IllegalStateException("Only standalone Redis configuration is supported in this sample");
        }

        RedisURI.Builder builder = RedisURI.builder()
                .withHost(standalone.getHostName())
                .withPort(standalone.getPort());

        RedisPassword password = standalone.getPassword();
        if (password != null && password.isPresent()) {
            builder.withPassword(password.get());
        }
        if (standalone.getDatabase() != 0) {
            builder.withDatabase(standalone.getDatabase());
        }

        return RedisClient.create(builder.build());
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public ProxyManager<byte[]> bucket4jProxyManager(RedisClient redisClient) {
        return LettuceBasedProxyManager.builderFor(redisClient).build();
    }
}
