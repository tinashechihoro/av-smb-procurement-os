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
        // Credentials first; the second factor only applies to accounts enrolled in it.
        var authResponse = identityService.login(request);

        User user = userRepository.findById(authResponse.getUserId())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user vanished: " + authResponse.getUserId()));

        // Only accounts with MFA switched on are challenged. Previously every
        // login returned requiresOtp=true unconditionally, including accounts
        // with no phone number — generateOtp then failed, its null otpId was
        // coerced to "", and the caller got HTTP 200 claiming an OTP had been
        // sent. No code existed to verify, so login could never complete.
        if (!Boolean.TRUE.equals(user.getMfaEnabled())) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "requiresOtp", false,
                    "accessToken", authResponse.getAccessToken(),
                    "refreshToken", authResponse.getRefreshToken(),
                    "user", authResponse
            ));
        }

        OtpGenerationResult otpResult = otpService.generateOtp(authResponse.getUserId(), OtpPurpose.LOGIN);

        // Fail closed: this account requires a second factor, so a challenge we
        // could not issue must never fall through to a session. Returning the
        // failure keeps the misconfiguration visible instead of silently
        // stranding the user on an OTP prompt that can never be satisfied.
        if (!otpResult.success() || otpResult.otpId() == null) {
            return ResponseEntity.status(503).body(Map.of(
                    "success", false,
                    "requiresOtp", true,
                    "error", otpResult.error() != null
                            ? otpResult.error()
                            : "Could not send the login code. Contact an administrator."
            ));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "requiresOtp", true,
                "otpId", otpResult.otpId().toString(),
                "message", "OTP sent to your registered phone number",
                // Flattened alongside the nested object: the client reads
                // userId/fullName at the top level.
                "userId", authResponse.getUserId().toString(),
                "email", authResponse.getEmail(),
                "fullName", authResponse.getFullName(),
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
