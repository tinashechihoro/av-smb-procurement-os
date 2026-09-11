package com.avsmc.procurement.identity.controller;

import com.avsmc.procurement.identity.dto.*;
import com.avsmc.procurement.identity.entity.User;
import com.avsmc.procurement.identity.repository.UserRepository;
import com.avsmc.procurement.identity.service.IdentityService;
import com.avsmc.procurement.otp.entity.OtpCode.OtpPurpose;
import com.avsmc.procurement.otp.service.OtpService;
import com.avsmc.procurement.otp.service.OtpService.OtpGenerationResult;
import com.avsmc.procurement.security.PermissionService;
import com.avsmc.procurement.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IdentityService identityService;
    private final PermissionService permissionService;
    private final OtpService otpService;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody AuthRequest request) {
        // First validate credentials, then require OTP
        var authResponse = identityService.login(request);
        
        // Generate OTP for login verification
        OtpGenerationResult otpResult = otpService.generateOtp(authResponse.getUserId(), OtpPurpose.LOGIN);
        
        return ResponseEntity.ok(Map.of(
                "requiresOtp", true,
                "otpId", otpResult.otpId() != null ? otpResult.otpId().toString() : "",
                "message", "OTP sent to your registered phone number",
                "user", Map.of(
                        "userId", authResponse.getUserId().toString(),
                        "email", authResponse.getEmail(),
                        "fullName", authResponse.getFullName()
                )
        ));
    }

    @PostMapping("/login/verify")
    public ResponseEntity<Map<String, Object>> verifyLoginOtp(@Valid @RequestBody VerifyLoginRequest request) {
        var verifyResult = otpService.verifyOtpForUser(
                UUID.fromString(request.getUserId()), 
                OtpPurpose.LOGIN, 
                request.getOtpCode()
        );
        
        if (!verifyResult.success()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", verifyResult.error() != null ? verifyResult.error() : "OTP verification failed"
            ));
        }
        
        // OTP verified, now issue the actual tokens
        var authResponse = identityService.loginWithOtp(UUID.fromString(request.getUserId()), verifyResult.otpId());
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "requiresOtp", false,
                "accessToken", authResponse.getAccessToken(),
                "refreshToken", authResponse.getRefreshToken(),
                "user", authResponse
        ));
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

    @PutMapping("/users/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        permissionService.requirePermission("users.manage");
        return ResponseEntity.ok(identityService.updateUser(id, request));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deactivateUser(@PathVariable UUID id) {
        permissionService.requirePermission("users.manage");
        identityService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/roles")
    public ResponseEntity<List<RoleDto>> listRoles() {
        permissionService.requirePermission("roles.manage");
        return ResponseEntity.ok(identityService.listRoles());
    }

    @Data
    public static class VerifyLoginRequest {
        @NotBlank
        private String userId;
        @NotBlank
        private String otpCode;
    }
}
