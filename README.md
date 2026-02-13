# spring-boot-rate-limiter

Production-oriented rate limiting for Spring Boot 3 backed by Redis, with:

- Redis-backed token buckets (Bucket4j)
- Multiple strategies (IP / API key / global)
- Per-route configuration (path + HTTP methods)
- Standard rate limit headers + `429 Too Many Requests`
- Micrometer metrics (`http.rate_limit.requests`)
- Unit + integration tests (Testcontainers Redis)
- GitHub Actions CI

## Requirements

- Java 17+
- Redis 6+ (standalone)

## Run locally

Start Redis:

```bash
docker run --rm -p 6379:6379 redis:7-alpine
```

Run the app:

```bash
mvn spring-boot:run
```

Test the demo endpoint:

```bash
curl -i http://localhost:8080/api/hello
```

After the configured limit is exceeded you will receive `429` with `Retry-After`.

## Configuration

Configure in `application.yml`:

```yaml
rate-limiter:
  enabled: true
  redis-key-prefix: rl
  default-policy:
    strategy: IP
    limit:
      capacity: 60
      period: 1m
  routes:
    - id: hello-ip
      path: /api/hello
      methods: [GET]
      strategy: IP
      limit:
        capacity: 5
        period: 10s
```

### Strategies

- `IP`: bucket per client IP (honors `X-Forwarded-For` first hop)
- `API_KEY`: bucket per `X-Api-Key` header value (falls back to `anonymous`)
- `GLOBAL`: single bucket shared by all clients

## Metrics

Micrometer counter:

- `http.rate_limit.requests{route="...", outcome="allowed|blocked"}`

Expose metrics via Spring Boot Actuator (see `management.endpoints.web.exposure.include`).

## Notes for production

- If you use `X-Forwarded-For`, ensure your reverse proxy sanitizes/overwrites it.
- This sample supports Redis standalone configuration.

## License

MIT (see [LICENSE](LICENSE)).
