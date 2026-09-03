package com.avsmc.procurement.identity.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data @Builder
public class UserDto {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private Boolean isActive;
    private Boolean mfaEnabled;
    private Instant lastLoginAt;
    private UUID organisationId;
    private String organisationName;
    private List<RoleDto> roles;
    private Instant createdAt;
}
