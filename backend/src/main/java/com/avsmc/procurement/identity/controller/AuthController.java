package com.avsmc.procurement.identity.controller;

import com.avsmc.procurement.identity.dto.*;
import com.avsmc.procurement.identity.service.IdentityService;
import com.avsmc.procurement.security.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IdentityService identityService;
    private final PermissionService permissionService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(identityService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(identityService.refresh(request.getRefreshToken()));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me() {
        return ResponseEntity.ok(identityService.getCurrentUser());
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> listUsers() {
        permissionService.requirePermission("users.manage");
        return ResponseEntity.ok(identityService.listUsers());
    }

    @PostMapping("/users")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        permissionService.requirePermission("users.manage");
        return ResponseEntity.ok(identityService.createUser(request));
    }

    @GetMapping("/roles")
    public ResponseEntity<List<RoleDto>> listRoles() {
        permissionService.requirePermission("roles.manage");
        return ResponseEntity.ok(identityService.listRoles());
    }
}
