package com.avsmc.procurement.identity.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data @Builder
public class AuthResponse {
    private UUID userId;
    private String email;
    private String fullName;
    private UUID organisationId;
    private String organisationName;
    private String orgType;
    private String roleCode;
    private String roleName;
    private List<String> permissions;
    private String accessToken;
    private String refreshToken;
}
