package com.edgecloud.edge.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
@Profile("secure")
@Order(-150)
public class LegacyPathEndpointGuardFilter extends OncePerRequestFilter {

    private static final Set<String> BLOCKED = Set.of(
            "/api/v1/edge/profile",
            "/api/v1/edge/predict",
            "/api/v1/edge/hybrid/preprocess"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        if (BLOCKED.contains(request.getRequestURI())) {
            response.setStatus(410);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"code\":\"LEGACY_ENDPOINT_DISABLED\","
                    + "\"message\":\"Legacy filesystem-path image endpoints are disabled in secure mode; use the v2 image API\"}"
            );
            return;
        }

        chain.doFilter(request, response);
    }
}

