package com.ecoapp.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Basic per-IP rate limiting for brute-force-prone routes (login/register).
 *
 * This is an IN-MEMORY sliding window — fine for a single gateway instance,
 * but each replica would track its own counters independently if you ever
 * run more than one, making the effective limit N× the configured value. For
 * a real multi-instance deployment, replace this with Spring Cloud Gateway's
 * built-in RequestRateLimiter backed by Redis (spring-boot-starter-data-redis
 * -reactive + a RedisRateLimiter bean), which shares counters across
 * instances.
 */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final List<String> LIMITED_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/register"
    );

    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_MS = 60_000; // 1 minute

    private record Window(long windowStart, int count) {}

    private final Map<String, Window> buckets = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (LIMITED_PATHS.stream().noneMatch(path::equals)) {
            return chain.filter(exchange);
        }

        String clientIp = resolveClientIp(exchange);
        String key = clientIp + "|" + path;
        long now = Instant.now().toEpochMilli();

        boolean limited;
        lock.lock();
        try {
            Window current = buckets.get(key);
            if (current == null || now - current.windowStart() > WINDOW_MS) {
                buckets.put(key, new Window(now, 1));
                limited = false;
            } else if (current.count() < MAX_REQUESTS) {
                buckets.put(key, new Window(current.windowStart(), current.count() + 1));
                limited = false;
            } else {
                limited = true;
            }
        } finally {
            lock.unlock();
        }

        if (limited) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }

    /**
     * CAUTION: X-Forwarded-For is client-settable and only trustworthy if a
     * reverse proxy/load balancer in front of this gateway overwrites it
     * (rather than appending to whatever the client sent). If this gateway
     * is ever directly internet-facing with no such proxy, remove the
     * X-Forwarded-For branch below — otherwise an attacker can send a
     * different fake value on every request to dodge the limit entirely.
     */
    private String resolveClientIp(ServerWebExchange exchange) {
        String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
        return remote != null ? remote.getAddress().getHostAddress() : "unknown";
    }

    @Override
    public int getOrder() {
        return -2; // before JwtAuthFilter, so unauthenticated brute-force attempts are throttled too
    }
}
