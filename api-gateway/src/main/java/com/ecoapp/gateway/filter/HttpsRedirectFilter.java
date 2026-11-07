package com.ecoapp.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * Redirects HTTP → HTTPS, based on the X-Forwarded-Proto header a
 * TLS-terminating load balancer/reverse proxy/CDN is expected to set.
 *
 * OFF by default (app.force-https=false) so local development over plain
 * HTTP (no TLS, no proxy setting X-Forwarded-Proto at all) keeps working
 * unmodified. In most real deployments HTTPS enforcement belongs at the
 * load balancer/CDN layer anyway (it can refuse plain HTTP entirely, before
 * traffic ever reaches this app) — enable this here as a backstop only if
 * your infrastructure forwards plain HTTP through to this gateway.
 *
 * Enable in production with: app.force-https=true  (or APP_FORCE_HTTPS=true)
 */
@Component
public class HttpsRedirectFilter implements GlobalFilter, Ordered {

    @Value("${app.force-https:false}")
    private boolean forceHttps;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!forceHttps) {
            return chain.filter(exchange);
        }

        String proto = exchange.getRequest().getHeaders().getFirst("X-Forwarded-Proto");
        if ("http".equalsIgnoreCase(proto)) {
            URI original = exchange.getRequest().getURI();
            URI httpsUri = URI.create("https://" + original.getHost()
                    + (original.getPort() > 0 && original.getPort() != 80 ? ":" + original.getPort() : "")
                    + original.getRawPath()
                    + (original.getRawQuery() != null ? "?" + original.getRawQuery() : ""));
            exchange.getResponse().setStatusCode(HttpStatus.PERMANENT_REDIRECT);
            exchange.getResponse().getHeaders().setLocation(httpsUri);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -3; // before rate limiting/auth — redirect first, don't waste a rate-limit slot on it
    }
}
