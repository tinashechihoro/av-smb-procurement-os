package com.avsmc.procurement.config;

import com.avsmc.procurement.security.SecurityUtils;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditConfig {

    private final EntityManager entityManager;
    private final SecurityUtils securityUtils;

    @Before("execution(* com.avsmc.procurement..controller..*(..))")
    public void setAuditContext() {
        try {
            var principal = securityUtils.currentUser();
            entityManager.createNativeQuery("SET LOCAL app.current_user_id = :userId")
                    .setParameter("userId", principal.userId().toString())
                    .executeUpdate();
            entityManager.createNativeQuery("SET LOCAL app.current_org_id = :orgId")
                    .setParameter("orgId", principal.organisationId().toString())
                    .executeUpdate();
        } catch (Exception ignored) {
            // No authenticated user (e.g., public endpoint)
        }
    }
}
