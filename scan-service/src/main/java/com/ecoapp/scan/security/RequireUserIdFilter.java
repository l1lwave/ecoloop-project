package com.ecoapp.scan.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Same rationale as reward-service's RequireUserIdFilter: this service trusts
 * the gateway to have already verified the caller's JWT and to be the only
 * way in. This is the fail-closed backstop if that network assumption ever
 * breaks — without it, an anonymous caller who reaches this port directly
 * could use it as a free, unauthenticated Open Food Facts lookup proxy.
 */
@Component
public class RequireUserIdFilter extends OncePerRequestFilter {

    /** Service-to-service calls under /internal carry no user; they are guarded by InternalTokenFilter. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String userId = request.getHeader("X-User-Id");
        if (userId == null || userId.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"message\":\"Missing verified identity\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
