package com.ecoapp.reward.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * This service has no JWT verification of its own — it trusts the X-User-Id
 * header set by api-gateway's JwtAuthFilter after verifying the caller's JWT.
 * That trust is only valid if this service is unreachable except through the
 * gateway. This filter is the fail-closed backstop for that assumption: if
 * this port is ever accidentally exposed directly (bad network/firewall
 * config, a misconfigured load balancer, etc.), requests without a verified
 * identity are rejected here too, instead of silently operating on whatever
 * a client claims its own user ID is.
 */
@Component
public class RequireUserIdFilter extends OncePerRequestFilter {

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
