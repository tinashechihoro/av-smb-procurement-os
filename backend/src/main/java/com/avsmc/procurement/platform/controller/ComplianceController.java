package com.avsmc.procurement.platform.controller;

import com.avsmc.procurement.security.SecurityUtils;
import jakarta.persistence.EntityManager;
import com.avsmc.procurement.security.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/compliance")
@RequiredArgsConstructor
public class ComplianceController {

    private final EntityManager em;
    private final SecurityUtils securityUtils;
    private final PermissionService permissionService;

    @Value("${app.audit.retention-days:2555}")
    private int auditRetentionDays;

    @GetMapping("/data-export")
    public ResponseEntity<Map<String, Object>> exportUserData() {
        UUID userId = securityUtils.currentUserId();

        Map<String, Object> export = new LinkedHashMap<>();
        export.put("exportedAt", Instant.now());
        export.put("userId", userId);

        export.put("user", em.createNativeQuery(
                "SELECT id, email, first_name, last_name, phone, is_active, mfa_enabled, last_login_at, created_at FROM users WHERE id = :id")
                .setParameter("id", userId).getResultList());

        export.put("auditEntries", em.createNativeQuery(
                "SELECT action, entity_type, entity_id, created_at FROM audit.audit_log WHERE user_id = :id ORDER BY created_at DESC LIMIT 1000")
                .setParameter("id", userId).getResultList());

        export.put("notifications", em.createNativeQuery(
                "SELECT title, message, notification_type, is_read, created_at FROM notifications WHERE user_id = :id ORDER BY created_at DESC")
                .setParameter("id", userId).getResultList());

        return ResponseEntity.ok(export);
    }

    @GetMapping("/audit-retention")
    public ResponseEntity<Map<String, Object>> auditRetention() {
        permissionService.requirePermission("audit.view");

        UUID orgId = securityUtils.currentOrgId();

        long totalEntries = ((Number) em.createNativeQuery(
                "SELECT count(*) FROM audit.audit_log WHERE organisation_id = :orgId")
                .setParameter("orgId", orgId).getSingleResult()).longValue();

        long entriesOlderThanRetention = ((Number) em.createNativeQuery(
                "SELECT count(*) FROM audit.audit_log WHERE organisation_id = :orgId AND created_at < :cutoff")
                .setParameter("orgId", orgId)
                .setParameter("cutoff", Instant.now().minusSeconds(auditRetentionDays * 86400L))
                .getSingleResult()).longValue();

        return ResponseEntity.ok(Map.of(
                "retentionDays", auditRetentionDays,
                "totalEntries", totalEntries,
                "entriesEligibleForArchival", entriesOlderThanRetention,
                "cutoffDate", Instant.now().minusSeconds(auditRetentionDays * 86400L)
        ));
    }

    @GetMapping("/privacy-policy")
    public ResponseEntity<Map<String, Object>> privacyPolicy() {
        return ResponseEntity.ok(Map.of(
                "version", "1.0.0",
                "lastUpdated", "2026-09-03",
                "dataController", "AV Motors × SMB Procurement OS",
                "dataRetention", Map.of(
                        "auditLogs", auditRetentionDays + " days",
                        "personalData", "Until account deletion",
                        "transactionData", "7 years (financial regulation)",
                        "documents", "Until explicitly deleted"
                ),
                "userRights", List.of(
                        "Right to access personal data",
                        "Right to rectification",
                        "Right to erasure (where not legally required)",
                        "Right to data portability",
                        "Right to restrict processing"
                )
        ));
    }
}
