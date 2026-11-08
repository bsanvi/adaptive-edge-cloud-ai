package com.edgecloud.edge.security;

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

    private static final String API_KEY_HEADER = "X-Internal-Api-Key";
    private static final long WINDOW_MS = 60_000L;
    private static final int MAX_TRACKED_CLIENTS = 1024;

    private final byte[] expectedKey;
    private final int requestsPerMinute;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/v1/edge/health",
            "/health",
            "/error"
    );

    public ApiSecurityFilter(
            @Value("${security.internal-api-key:}") String internalApiKey,
            @Value("${security.rate-limit-per-minute:120}") int requestsPerMinute) {

        if (internalApiKey == null || internalApiKey.length() < 32) {
            throw new IllegalStateException(
                    "INTERNAL_API_KEY must contain at least 32 characters in secure mode"
            );
        }
        if (requestsPerMinute < 1) {
            throw new IllegalStateException(
                    "security.rate-limit-per-minute must be positive"
            );
        }

        this.expectedKey = internalApiKey.getBytes(StandardCharsets.UTF_8);
        this.requestsPerMinute = requestsPerMinute;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        securityHeaders(response);

        if (!allow(request.getRemoteAddr())) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            json(response, "RATE_LIMITED", "Too many requests");
            return;
        }

        String path = request.getRequestURI();

        if (PUBLIC_PATHS.contains(path)) {
            chain.doFilter(request, response);
            return;
        }

        if (requiresJson(request) && !isJson(request.getContentType())) {
            response.setStatus(415);
            json(response, "UNSUPPORTED_MEDIA_TYPE",
                    "Content-Type must be application/json");
            return;
        }

        String supplied = request.getHeader(API_KEY_HEADER);
        if (!constantTimeEquals(supplied)) {
            response.setStatus(401);
            json(response, "UNAUTHORIZED",
                    "Missing or invalid internal API key");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean requiresJson(HttpServletRequest request) {
        String method = request.getMethod();
        return ("POST".equals(method)
                || "PUT".equals(method)
                || "PATCH".equals(method))
                && request.getRequestURI().startsWith("/api/");
    }

    private boolean isJson(String contentType) {
        return contentType != null
                && contentType.toLowerCase().startsWith("application/json");
    }

    private boolean constantTimeEquals(String supplied) {
        if (supplied == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expectedKey,
                supplied.getBytes(StandardCharsets.UTF_8)
        );
    }

    private boolean allow(String remoteAddress) {
        String key = remoteAddress == null ? "unknown" : remoteAddress;
        long now = System.currentTimeMillis();
        Window window = windows.computeIfAbsent(key, ignored -> new Window(now));

        synchronized (window) {
            if (now - window.startedAt >= WINDOW_MS) {
                window.startedAt = now;
                window.count = 0;
            }
            window.count++;

            if (windows.size() > MAX_TRACKED_CLIENTS) {
                windows.entrySet().removeIf(
                        e -> now - e.getValue().startedAt >= WINDOW_MS
                );
            }
            return window.count <= requestsPerMinute;
        }
    }

    private void securityHeaders(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader(
                "Content-Security-Policy",
                "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"
        );
    }

    private void json(
            HttpServletResponse response,
            String code,
            String message) throws IOException {

        response.setContentType("application/json");
        response.getWriter().write(
                "{\"code\":\"" + code
                        + "\",\"message\":\"" + message
                        + "\",\"timestamp\":\"" + Instant.now() + "\"}"
        );
    }

    private static final class Window {
        private long startedAt;
        private int count;

        private Window(long startedAt) {
            this.startedAt = startedAt;
        }
    }
}

