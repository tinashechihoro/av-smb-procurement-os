package com.avsmc.procurement.identity.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data @Builder
public class RoleDto {
    private UUID id;
    private String name;
    private String code;
    private String description;
    private List<String> permissions;
}
