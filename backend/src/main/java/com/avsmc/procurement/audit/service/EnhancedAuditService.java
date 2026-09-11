package com.avsmc.procurement.audit.service;

import com.avsmc.procurement.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedAuditService {

    private final SecurityUtils securityUtils;

    public AuditContext captureContext() {
        AuditContext ctx = new AuditContext();
        
        try {
            ctx.setUserId(securityUtils.currentUserId());
            ctx.setOrgId(securityUtils.currentOrgId());
        } catch (Exception e) {
            // No authenticated user
        }

        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            ctx.setIpAddress(securityUtils.getClientIp());
            ctx.setUserAgent(securityUtils.getUserAgent());
            ctx.setSessionId(request.getRequestedSessionId() != null 
                    ? UUID.nameUUIDFromBytes(request.getRequestedSessionId().getBytes()) 
                    : null);
        }

        return ctx;
    }

    public AuditContext captureContextWithLocation(GpsLocation location) {
        AuditContext ctx = captureContext();
        
        if (location != null) {
            ctx.setGpsLatitude(location.latitude());
            ctx.setGpsLongitude(location.longitude());
            ctx.setGpsAccuracy(location.accuracy());
            ctx.setLocationSource(location.accuracy() != null && location.accuracy() < 100 ? "GPS" : "IP");
        } else {
            ctx.setLocationSource("IP");
        }

        return ctx;
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    @lombok.Data
    public static class AuditContext {
        private UUID userId;
        private UUID orgId;
        private String ipAddress;
        private String userAgent;
        private UUID sessionId;
        private Double gpsLatitude;
        private Double gpsLongitude;
        private Double gpsAccuracy;
        private String locationSource;
        private String phone;
        private String deviceFingerprint;
    }

    public record GpsLocation(Double latitude, Double longitude, Double accuracy) {}
}
