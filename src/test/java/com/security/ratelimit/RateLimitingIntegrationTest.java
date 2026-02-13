package com.security.ratelimit;

import com.security.RateLimiterApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(
        classes = RateLimiterApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "rate-limiter.default-policy.limit.capacity=1000",
                "rate-limiter.default-policy.limit.period=1m",
                "rate-limiter.routes[0].id=hello",
                "rate-limiter.routes[0].path=/api/hello",
                "rate-limiter.routes[0].methods[0]=GET",
                "rate-limiter.routes[0].strategy=GLOBAL",
                "rate-limiter.routes[0].limit.capacity=2",
                "rate-limiter.routes[0].limit.period=5s"
        }
)
class RateLimitingIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.4.2-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    TestRestTemplate rest;

    @Test
    void blocksAfterLimitExceeded() {
        ResponseEntity<String> r1 = rest.getForEntity("/api/hello", String.class);
        ResponseEntity<String> r2 = rest.getForEntity("/api/hello", String.class);
        ResponseEntity<String> r3 = rest.getForEntity("/api/hello", String.class);

        assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r2.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r3.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(r3.getHeaders().getFirst("Retry-After")).isNotBlank();
        assertThat(r3.getHeaders().getFirst("X-RateLimit-Limit")).isEqualTo("2");
    }
}
