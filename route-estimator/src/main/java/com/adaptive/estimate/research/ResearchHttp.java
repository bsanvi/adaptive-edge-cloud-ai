package com.adaptive.estimate.research;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.io.*;
import java.time.Instant;
import java.util.Map;

@Order(-100)
@RestControllerAdvice(basePackageClasses = ResearchHttp.class)
public class ResearchHttp {
    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    ResponseEntity<?> invalid(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("code", "VALIDATION_ERROR", "message",
                e instanceof IllegalArgumentException ? String.valueOf(e.getMessage()) : "Malformed JSON", "timestamp", Instant.now()));
    }
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> upstream(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("code", "REQUEST_FAILED", "message",
                e.getReason() == null ? "Request failed" : e.getReason(), "timestamp", Instant.now()));
    }
    @Component
    public static class BodyLimit extends OncePerRequestFilter {
        @Override protected boolean shouldNotFilter(HttpServletRequest r) {
            return !r.getRequestURI().startsWith("/api/v2/") || !r.getMethod().equals("POST");
        }
        @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            int max = 12 * 1024 * 1024;
            byte[] bytes = request.getContentLengthLong() > max ? null : request.getInputStream().readNBytes(max + 1);
            if (bytes == null || bytes.length > max) {
                response.setStatus(413); response.setContentType("application/json");
                response.getWriter().write("{\"code\":\"PAYLOAD_TOO_LARGE\",\"message\":\"Maximum request body is 12 MiB\"}"); return;
            }
            chain.doFilter(new HttpServletRequestWrapper(request) {
                @Override public ServletInputStream getInputStream() {
                    var input = new ByteArrayInputStream(bytes);
                    return new ServletInputStream() {
                        public int read() { return input.read(); }
                        public int read(byte[] b, int off, int len) { return input.read(b, off, len); }
                        public boolean isFinished() { return input.available() == 0; }
                        public boolean isReady() { return true; }
                        public void setReadListener(ReadListener listener) { throw new UnsupportedOperationException(); }
                    };
                }
            }, response);
        }
    }
}
