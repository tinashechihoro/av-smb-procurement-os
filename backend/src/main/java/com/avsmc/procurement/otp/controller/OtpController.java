package com.avsmc.procurement.otp.controller;

import com.avsmc.procurement.otp.entity.OtpCode.OtpPurpose;
import com.avsmc.procurement.otp.service.OtpService;
import com.avsmc.procurement.otp.service.OtpService.OtpGenerationResult;
import com.avsmc.procurement.otp.service.OtpService.OtpVerificationResult;
import com.avsmc.procurement.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/otp")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;
    private final SecurityUtils securityUtils;

    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateOtp(@Valid @RequestBody GenerateOtpRequest request) {
        UUID userId = securityUtils.currentUserId();
        OtpGenerationResult result = otpService.generateOtp(userId, request.getPurpose());

        if (!result.success()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", result.error() != null ? result.error() : "Failed to generate OTP"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "otpId", result.otpId().toString(),
                "expiresAt", result.expiresAt().toString(),
                "message", "OTP sent to your registered phone number"
        ));
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        OtpVerificationResult result;

        if (request.getOtpId() != null) {
            result = otpService.verifyOtp(request.getOtpId(), request.getCode());
        } else {
            UUID userId = securityUtils.currentUserId();
            result = otpService.verifyOtpForUser(userId, request.getPurpose(), request.getCode());
        }

        if (!result.success()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", result.error() != null ? result.error() : "OTP verification failed"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "otpId", result.otpId().toString(),
                "userId", result.userId().toString(),
                "purpose", result.purpose().name(),
                "message", "OTP verified successfully"
        ));
    }

    @PostMapping("/verify-for-action")
    public ResponseEntity<Map<String, Object>> verifyOtpForAction(@Valid @RequestBody VerifyForActionRequest request) {
        UUID userId = securityUtils.currentUserId();
        OtpVerificationResult result = otpService.verifyOtpForUser(userId, request.getPurpose(), request.getCode());

        if (!result.success()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", result.error() != null ? result.error() : "OTP verification failed"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "otpId", result.otpId().toString(),
                "verified", true,
                "message", "Action authorized with OTP"
        ));
    }

    @Data
    public static class GenerateOtpRequest {
        @NotNull
        private OtpPurpose purpose;
    }

    @Data
    public static class VerifyOtpRequest {
        private UUID otpId;
        @NotBlank
        private String code;
        private OtpPurpose purpose;
    }

    @Data
    public static class VerifyForActionRequest {
        @NotNull
        private OtpPurpose purpose;
        @NotBlank
        private String code;
    }
}
