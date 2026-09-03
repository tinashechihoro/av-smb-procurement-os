package com.avsmc.procurement.platform.controller;

import com.avsmc.procurement.platform.entity.Notification;
import com.avsmc.procurement.platform.repository.NotificationRepository;
import com.avsmc.procurement.security.SecurityUtils;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class PlatformController {

    private final NotificationRepository notificationRepository;
    private final SecurityUtils securityUtils;
    private final EntityManager entityManager;

    @GetMapping("/audit")
    public ResponseEntity<List<Map<String, Object>>> getAuditTrail(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID userId,
            @RequestParam(defaultValue = "50") int limit) {

        StringBuilder sql = new StringBuilder(
                "SELECT id, organisation_id, user_id, action, entity_type, entity_id, " +
                "before_data, after_data, ip_address, created_at " +
                "FROM audit.audit_log WHERE organisation_id = :orgId");

        if (action != null) sql.append(" AND action = :action");
        if (entityType != null) sql.append(" AND entity_type = :entityType");
        if (userId != null) sql.append(" AND user_id = :userId");

        sql.append(" ORDER BY created_at DESC LIMIT :limit");

        var query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("orgId", securityUtils.currentOrgId());
        if (action != null) query.setParameter("action", action);
        if (entityType != null) query.setParameter("entityType", entityType);
        if (userId != null) query.setParameter("userId", userId);
        query.setParameter("limit", limit);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        List<Map<String, Object>> result = rows.stream().map(row -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", row[0]);
            map.put("organisationId", row[1]);
            map.put("userId", row[2]);
            map.put("action", row[3]);
            map.put("entityType", row[4]);
            map.put("entityId", row[5]);
            map.put("beforeData", row[6]);
            map.put("afterData", row[7]);
            map.put("ipAddress", row[8]);
            map.put("createdAt", row[9]);
            return map;
        }).toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<Notification>> getNotifications() {
        return ResponseEntity.ok(
                notificationRepository.findByUserIdOrderByCreatedAtDesc(securityUtils.currentUserId()));
    }

    @GetMapping("/notifications/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        long count = notificationRepository.countByUserIdAndIsReadFalse(securityUtils.currentUserId());
        return ResponseEntity.ok(Map.of("unread", count));
    }

    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
        return ResponseEntity.ok().build();
    }
}
