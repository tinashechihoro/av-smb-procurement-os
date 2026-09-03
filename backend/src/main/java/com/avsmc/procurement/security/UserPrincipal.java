package com.avsmc.procurement.security;

import java.util.UUID;

public record UserPrincipal(UUID userId, UUID organisationId, String roleCode) {
}
