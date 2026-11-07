package com.ecoapp.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Adds security headers to every response that passes through the gateway —
 * i.e. every API response. These are real HTTP headers (unlike the frontend's
 * <meta> equivalents in eco-frontend/index.html), so they cover things a
 * <meta> tag cannot: X-Frame-Options, X-Content-Type-Options,
 * Strict-Transport-Security, Permissions-Policy.
 *
 * Note: HSTS only has any effect once this gateway is actually served over
 * HTTPS (real TLS termination is normally done by a load balancer/reverse
 * proxy/CDN in front of this, not by Spring itself) — see also
 * HttpsRedirectFilter for the accompanying HTTP→HTTPS redirect.
 */
@Component
public class SecurityHeadersFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        HttpHeaders headers = exchange.getResponse().getHeaders();
        headers.add("X-Content-Type-Options", "nosniff");
        headers.add("X-Frame-Options", "DENY");
        headers.add("Referrer-Policy", "strict-origin-when-cross-origin");
        headers.add("Permissions-Policy", "geolocation=(), camera=(), microphone=()");
        // The API itself serves no HTML, so a strict CSP here is mainly defense-in-depth
        // against any response ever being reflected/rendered somewhere unexpected.
        headers.add("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
        headers.add("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload");
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE; // run last, on the way out, so it applies to every response
    }
}
