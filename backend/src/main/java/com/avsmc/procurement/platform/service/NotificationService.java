package com.avsmc.procurement.platform.service;

import com.avsmc.procurement.platform.entity.Notification;
import com.avsmc.procurement.platform.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Async
    @Transactional
    public void send(UUID orgId, UUID userId, String title, String message, String type, String entityType, UUID entityId) {
        Notification n = new Notification();
        n.setOrganisationId(orgId);
        n.setUserId(userId);
        n.setTitle(title);
        n.setMessage(message);
        n.setNotificationType(type);
        n.setEntityType(entityType);
        n.setEntityId(entityId);
        notificationRepository.save(n);
    }

    @Async
    @Transactional
    public void sendToOrg(UUID orgId, String title, String message, String type, String entityType, UUID entityId) {
        // Send to all users in the org — in production, query user list
        send(orgId, orgId, title, message, type, entityType, entityId);
    }
}
