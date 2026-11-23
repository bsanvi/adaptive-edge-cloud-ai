package com.team.aecai.orchestrator.security;

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
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("secure")
@Order(-300)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60000L;

    private final int apiLimit;
    private final int loginLimit;

    private final ConcurrentHashMap<String,Window> apiWindows =
            new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String,Window> loginWindows =
            new ConcurrentHashMap<>();

    public RateLimitFilter(
            @Value("${security.rate-limit-per-minute:300}") int apiLimit,
            @Value("${security.login-rate-limit-per-minute:10}") int loginLimit) {
        this.apiLimit = apiLimit;
        this.loginLimit = loginLimit;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader(
                "Permissions-Policy",
                "camera=(), microphone=(), geolocation=()"
        );
        response.setHeader(
                "Content-Security-Policy",
                "default-src 'self'; " +
                "script-src 'self'; " +
                "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                "font-src 'self' https://fonts.gstatic.com data:; " +
                "img-src 'self' data: blob:; " +
                "connect-src 'self'; " +
                "frame-ancestors 'none'; " +
                "base-uri 'self'; " +
                "form-action 'self'"
        );

        String path = request.getRequestURI();

        String client = request.getRemoteAddr() == null
                ? "unknown"
                : request.getRemoteAddr();

        if (path.startsWith("/api/")
                && request.getContentLengthLong() > 12_582_912L) {
            response.setStatus(413);
            return;
        }

        if ("/login".equals(path)
                && "POST".equalsIgnoreCase(request.getMethod())
                && !allow(loginWindows, client, loginLimit)) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            return;
        }

        if (path.startsWith("/api/")
                && !allow(apiWindows, client, apiLimit)) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            return;
        }

        chain.doFilter(request, response);
    }

    private static boolean allow(
            ConcurrentHashMap<String,Window> windows,
            String client,
            int limit) {

        long now = System.currentTimeMillis();

        Window window =
                windows.computeIfAbsent(client, key -> new Window(now));

        synchronized (window) {
            if (now - window.started >= WINDOW_MS) {
                window.started = now;
                window.count = 0;
            }

            window.count++;
            return window.count <= limit;
        }
    }

    private static final class Window {
        long started;
        int count;

        Window(long started) {
            this.started = started;
        }
    }
}
