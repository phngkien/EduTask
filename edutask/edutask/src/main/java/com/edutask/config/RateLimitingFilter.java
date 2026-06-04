package com.edutask.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class RateLimitingFilter implements Filter {

    private final Map<String, TokenBucket> ipBuckets = new ConcurrentHashMap<>();
    private final Map<String, TokenBucket> authIpBuckets = new ConcurrentHashMap<>();

    private static final int GENERAL_LIMIT = 100;
    private static final int AUTH_LIMIT = 10;
    private static final long TIME_WINDOW_MS = 60000; // 1 minute

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest && response instanceof HttpServletResponse httpResponse) {
            String ip = getClientIp(httpRequest);
            String path = httpRequest.getRequestURI();

            // Kiểm tra rate limit cho các API đăng nhập/đăng ký để chống brute-force
            if (path.startsWith("/api/auth/login") || path.startsWith("/api/auth/register")) {
                TokenBucket authBucket = authIpBuckets.computeIfAbsent(ip, k -> new TokenBucket(AUTH_LIMIT, TIME_WINDOW_MS));
                if (!authBucket.tryConsume()) {
                    log.warn("Rate limit exceeded for authentication from IP: {}", ip);
                    sendErrorResponse(httpResponse, HttpStatus.TOO_MANY_REQUESTS, 
                            "Bạn đã yêu cầu đăng nhập/đăng ký quá nhiều lần. Vui lòng thử lại sau 1 phút.");
                    return;
                }
            }

            // Giới hạn chung cho tất cả các API khác
            if (path.startsWith("/api/")) {
                TokenBucket generalBucket = ipBuckets.computeIfAbsent(ip, k -> new TokenBucket(GENERAL_LIMIT, TIME_WINDOW_MS));
                if (!generalBucket.tryConsume()) {
                    log.warn("Rate limit exceeded for general API from IP: {}", ip);
                    sendErrorResponse(httpResponse, HttpStatus.TOO_MANY_REQUESTS, 
                            "Tần suất gửi yêu cầu quá nhanh. Vui lòng làm chậm lại.");
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String json = String.format("{\"success\":false,\"message\":\"%s\",\"data\":null}", message);
        response.getWriter().write(json);
    }

    private static class TokenBucket {
        private final int capacity;
        private final long refillDurationMs;
        private int tokens;
        private long lastRefillTime;

        public TokenBucket(int capacity, long refillDurationMs) {
            this.capacity = capacity;
            this.refillDurationMs = refillDurationMs;
            this.tokens = capacity;
            this.lastRefillTime = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsedTime = now - lastRefillTime;
            if (elapsedTime >= refillDurationMs) {
                tokens = capacity;
                lastRefillTime = now;
            } else {
                int refillAmount = (int) (elapsedTime * capacity / refillDurationMs);
                if (refillAmount > 0) {
                    tokens = Math.min(capacity, tokens + refillAmount);
                    lastRefillTime = now - (elapsedTime % (refillDurationMs / capacity));
                }
            }
        }
    }
}
