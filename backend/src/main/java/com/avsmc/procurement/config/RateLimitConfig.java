package com.avsmc.procurement.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class RateLimitConfig {

    private final Map<String, AtomicInteger> loginAttempts = new ConcurrentHashMap<>();
    private final Map<String, Long> loginAttemptTimestamps = new ConcurrentHashMap<>();

    @Bean
    public OncePerRequestFilter rateLimitFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                String path = request.getRequestURI();

                if (path.endsWith("/auth/login") && "POST".equalsIgnoreCase(request.getMethod())) {
                    String clientIp = getClientIp(request);
                    long now = System.currentTimeMillis();

                    loginAttemptTimestamps.entrySet().removeIf(e -> now - e.getValue() > 900_000);
                    loginAttempts.entrySet().removeIf(e -> !loginAttemptTimestamps.containsKey(e.getKey()));

                    AtomicInteger attempts = loginAttempts.computeIfAbsent(clientIp, k -> new AtomicInteger(0));
                    loginAttemptTimestamps.put(clientIp, now);

                    if (attempts.incrementAndGet() > 10) {
                        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Too many login attempts. Please try again in 15 minutes.\"}");
                        return;
                    }
                }

                filterChain.doFilter(request, response);
            }
        };
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip.split(",")[0].trim();
    }
}
