package com.avsmc.procurement.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class UpdateUserRequest {
    @Email
    private String email;

    @Size(min = 1, max = 100)
    private String firstName;

    @Size(min = 1, max = 100)
    private String lastName;

    private String phone;

    private String avatarUrl;

    private Boolean isActive;

    private Boolean mfaEnabled;

    private List<UUID> roleIds;
}
