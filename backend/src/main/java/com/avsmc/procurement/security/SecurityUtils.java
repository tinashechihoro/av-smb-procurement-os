package com.avsmc.procurement.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SecurityUtils {

    public UserPrincipal currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        throw new IllegalStateException("No authenticated user in context");
    }

    public UUID currentUserId() {
        return currentUser().userId();
    }

    public UUID currentOrgId() {
        return currentUser().organisationId();
    }

    public String currentRole() {
        return currentUser().roleCode();
    }

    public boolean isAvUser() {
        return currentRole().startsWith("AV_");
    }

    public boolean isSmbUser() {
        return currentRole().startsWith("SMB_");
    }

    public boolean hasRole(String roleCode) {
        return currentRole().equals(roleCode);
    }

    public boolean isAvAdmin() {
        return hasRole("AV_SYS_ADMIN");
    }

    public boolean isAvManager() {
        return hasRole("AV_MANAGER") || isAvAdmin();
    }

    public boolean isSmbAdmin() {
        return hasRole("SMB_ADMIN");
    }

    public boolean isSmbManager() {
        return hasRole("SMB_MANAGER") || isSmbAdmin();
    }
}
