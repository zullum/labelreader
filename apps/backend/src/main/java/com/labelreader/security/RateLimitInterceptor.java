package com.labelreader.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String key = getClientKey(request);
        Bucket bucket = cache.computeIfAbsent(key, k -> createBucket(request));

        if (bucket.tryConsume(1)) {
            return true;
        } else {
            log.warn("Rate limit exceeded for client: {}", key);
            response.setStatus(429);
            response.setHeader("X-Rate-Limit-Retry-After-Seconds", "60");
            return false;
        }
    }

    private String getClientKey(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String authHeader = request.getHeader("Authorization");

        // Use user token if available, otherwise use IP
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return "user:" + authHeader.substring(7, Math.min(authHeader.length(), 20));
        }
        return "ip:" + ip;
    }

    private Bucket createBucket(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Stricter limits for authentication endpoints
        if (path.contains("/api/auth/")) {
            return Bucket.builder()
                    .addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1))))
                    .build();
        }

        // Standard limits for other endpoints
        return Bucket.builder()
                .addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1))))
                .build();
    }
}
