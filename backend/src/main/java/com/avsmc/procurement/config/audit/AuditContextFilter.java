package com.avsmc.procurement.config.audit;

import com.avsmc.procurement.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Publishes the authenticated principal into the per-request audit context so
 * that database audit triggers can attribute writes to the acting user.
 * Runs after the Spring Security filter chain.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class AuditContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
                AuditContext.set(new AuditContext.Actor(principal.userId(), principal.organisationId()));
            }
            filterChain.doFilter(request, response);
        } finally {
            AuditContext.clear();
        }
    }
}
