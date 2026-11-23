package com.adaptive.cloud.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("secure")
@Order(-200)
public class ApiSecurityFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Internal-Api-Key";
    private static final long WINDOW_MS = 60000L;

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/v1/cloud/health",
            "/error"
    );

    private final byte[] expectedKey;
    private final int limit;

    private final ConcurrentHashMap<String, Window> windows =
            new ConcurrentHashMap<>();

    public ApiSecurityFilter(
            @Value("${security.internal-api-key:}") String apiKey,
            @Value("${security.rate-limit-per-minute:120}") int limit) {

        if (apiKey == null || apiKey.length() < 32) {
            throw new IllegalStateException(
                    "INTERNAL_API_KEY must contain at least 32 characters"
            );
        }

        this.expectedKey =
                apiKey.getBytes(StandardCharsets.UTF_8);

        this.limit = limit;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        headers(response);

        if (!allowed(request.getRemoteAddr())) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            json(response, "RATE_LIMITED", "Too many requests");
            return;
        }

        if (PUBLIC_PATHS.contains(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        String supplied = request.getHeader(HEADER);

        if (supplied == null ||
                !MessageDigest.isEqual(
                        expectedKey,
                        supplied.getBytes(StandardCharsets.UTF_8))) {

            response.setStatus(401);
            json(response,
                    "UNAUTHORIZED",
                    "Missing or invalid internal API key");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean allowed(String address) {

        long now = System.currentTimeMillis();

        Window window = windows.computeIfAbsent(
                address == null ? "unknown" : address,
                k -> new Window(now)
        );

        synchronized (window) {

            if (now - window.start >= WINDOW_MS) {
                window.start = now;
                window.count = 0;
            }

            window.count++;

            return window.count <= limit;
        }
    }

    private void headers(HttpServletResponse response) {

        response.setHeader("Cache-Control", "no-store");
        response.setHeader(
                "X-Content-Type-Options",
                "nosniff"
        );
        response.setHeader(
                "X-Frame-Options",
                "DENY"
        );
        response.setHeader(
                "Referrer-Policy",
                "no-referrer"
        );
        response.setHeader(
                "Content-Security-Policy",
                "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"
        );
    }

    private void json(
            HttpServletResponse response,
            String code,
            String message)
            throws IOException {

        response.setContentType("application/json");

        response.getWriter().write(
                "{\"code\":\"" + code +
                "\",\"message\":\"" + message +
                "\",\"timestamp\":\"" +
                Instant.now() + "\"}"
        );
    }

    private static class Window {
        long start;
        int count;

        Window(long start) {
            this.start = start;
        }
    }
}
