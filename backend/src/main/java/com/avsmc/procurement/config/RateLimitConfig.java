package com.avsmc.procurement.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class RateLimitConfig {

    private static final int MAX_FAILED_LOGINS_PER_WINDOW = 10;
    private static final long WINDOW_MS = 900_000; // 15 minutes

    private final Map<String, AtomicInteger> failedLogins = new ConcurrentHashMap<>();
    private final Map<String, Long> lastFailureAt = new ConcurrentHashMap<>();

    @Bean
    public OncePerRequestFilter rateLimitFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                if (!isLoginRequest(request)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                String clientIp = getClientIp(request);
                long now = System.currentTimeMillis();

                // Expire stale windows
                lastFailureAt.entrySet().removeIf(e -> now - e.getValue() > WINDOW_MS);
                failedLogins.entrySet().removeIf(e -> !lastFailureAt.containsKey(e.getKey()));

                AtomicInteger attempts = failedLogins.get(clientIp);
                if (attempts != null && attempts.get() >= MAX_FAILED_LOGINS_PER_WINDOW) {
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"error\":\"Too many failed login attempts. Please try again in 15 minutes.\"}");
                    return;
                }

                // Count only FAILED logins (401/423 from the auth endpoint);
                // successful sign-ins must not consume the budget.
                StatusCapturingResponse wrapper = new StatusCapturingResponse(response);
                filterChain.doFilter(request, wrapper);

                int status = wrapper.getStatus();
                if (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.LOCKED.value()) {
                    failedLogins.computeIfAbsent(clientIp, k -> new AtomicInteger(0)).incrementAndGet();
                    lastFailureAt.put(clientIp, System.currentTimeMillis());
                } else {
                    failedLogins.remove(clientIp);
                    lastFailureAt.remove(clientIp);
                }
            }
        };
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return request.getRequestURI().endsWith("/auth/login")
                && "POST".equalsIgnoreCase(request.getMethod());
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

    private static final class StatusCapturingResponse extends HttpServletResponseWrapper {
        private StatusCapturingResponse(HttpServletResponse response) {
            super(response);
        }
    }
}
