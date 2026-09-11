package com.avsmc.procurement.otp.service;

import com.avsmc.procurement.identity.entity.User;
import com.avsmc.procurement.identity.repository.UserRepository;
import com.avsmc.procurement.otp.entity.OtpCode;
import com.avsmc.procurement.otp.entity.OtpCode.OtpPurpose;
import com.avsmc.procurement.otp.repository.OtpRepository;
import com.avsmc.procurement.otp.sms.SendAiSmsClient;
import com.avsmc.procurement.otp.sms.SendAiSmsClient.SmsSendResult;
import com.avsmc.procurement.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpRepository otpRepository;
    private final UserRepository userRepository;
    private final SendAiSmsClient smsClient;
    private final SecurityUtils securityUtils;

    @Value("${otp.expiry-minutes:5}")
    private int expiryMinutes;

    @Value("${otp.max-attempts:3}")
    private int maxAttempts;

    @Value("${otp.test-mode:false}")
    private boolean testMode;

    @Value("${otp.test-code:123456}")
    private String testCode;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public OtpGenerationResult generateOtp(UUID userId, OtpPurpose purpose) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.getPhone() == null || user.getPhone().isBlank()) {
            return OtpGenerationResult.failed("User has no phone number configured");
        }

        // Invalidate any existing unused OTPs for this user/purpose
        otpRepository.findByUserIdAndPurposeAndIsUsedFalse(userId, purpose)
                .forEach(otp -> {
                    otp.setIsUsed(true);
                    otpRepository.save(otp);
                });

        String code = testMode ? testCode : generateSixDigitCode();
        Instant expiresAt = Instant.now().plusSeconds(expiryMinutes * 60L);

        HttpServletRequest request = securityUtils.getCurrentRequest();
        String ipAddress = request != null ? getClientIp(request) : null;
        String userAgent = request != null ? request.getHeader("User-Agent") : null;

        OtpCode otp = OtpCode.builder()
                .userId(userId)
                .code(code)
                .purpose(purpose)
                .phoneNumber(user.getPhone())
                .expiresAt(expiresAt)
                .maxAttempts(maxAttempts)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        SmsSendResult smsResult;
        if (testMode) {
            log.info("TEST MODE: OTP for user {} purpose {}: code={}", userId, purpose, code);
            smsResult = SmsSendResult.skipped("Test mode - no SMS sent");
        } else {
            smsResult = smsClient.sendOtp(user.getPhone(), code, purpose.name());
        }
        otp.setSmsMessageId(smsResult.messageId());
        otp.setSmsStatus(smsResult.success() ? "SENT" : smsResult.skipped() ? "SKIPPED" : "FAILED");

        otpRepository.save(otp);

        log.info("OTP generated for user {} purpose {}: id={}, smsStatus={}", 
                userId, purpose, otp.getId(), otp.getSmsStatus());

        return OtpGenerationResult.success(otp.getId(), expiresAt, smsResult.success() || smsResult.skipped());
    }

    @Transactional
    public OtpVerificationResult verifyOtp(UUID otpId, String code) {
        Optional<OtpCode> optOtp = otpRepository.findById(otpId);
        if (optOtp.isEmpty()) {
            return OtpVerificationResult.failed("OTP not found");
        }

        OtpCode otp = optOtp.get();

        if (otp.getIsUsed()) {
            return OtpVerificationResult.failed("OTP already used");
        }

        if (otp.isExpired()) {
            return OtpVerificationResult.failed("OTP has expired");
        }

        if (!otp.canAttempt()) {
            return OtpVerificationResult.failed("Maximum verification attempts exceeded");
        }

        if (!otp.getCode().equals(code)) {
            otpRepository.incrementFailedAttempts(otpId);
            int remaining = otp.getMaxAttempts() - otp.getFailedAttempts() - 1;
            return OtpVerificationResult.failed("Invalid OTP code. " + remaining + " attempts remaining.");
        }

        otpRepository.markAsVerified(otpId, Instant.now());
        otp.setIsUsed(true);
        otpRepository.save(otp);

        log.info("OTP verified: id={}, userId={}, purpose={}", otpId, otp.getUserId(), otp.getPurpose());

        return OtpVerificationResult.success(otpId, otp.getUserId(), otp.getPurpose());
    }

    @Transactional
    public OtpVerificationResult verifyOtpForUser(UUID userId, OtpPurpose purpose, String code) {
        Optional<OtpCode> optOtp = otpRepository.findFirstByUserIdAndPurposeAndIsUsedFalseOrderByCreatedAtDesc(userId, purpose);
        if (optOtp.isEmpty()) {
            return OtpVerificationResult.failed("No active OTP found. Please request a new one.");
        }
        return verifyOtp(optOtp.get().getId(), code);
    }

    public boolean hasVerifiedOtp(UUID userId, OtpPurpose purpose, long withinSeconds) {
        Instant cutoff = Instant.now().minusSeconds(withinSeconds);
        return otpRepository.findByUserIdAndPurposeAndIsUsedFalse(userId, purpose).stream()
                .anyMatch(otp -> otp.getVerifiedAt() != null 
                        && otp.getVerifiedAt().isAfter(cutoff)
                        && otp.getUserId().equals(userId));
    }

    private String generateSixDigitCode() {
        int code = RANDOM.nextInt(900000) + 100000;
        return String.valueOf(code);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public record OtpGenerationResult(boolean success, UUID otpId, Instant expiresAt, String error) {
        public static OtpGenerationResult success(UUID otpId, Instant expiresAt, boolean smsSent) {
            return new OtpGenerationResult(true, otpId, expiresAt, null);
        }
        public static OtpGenerationResult failed(String error) {
            return new OtpGenerationResult(false, null, null, error);
        }
    }

    public record OtpVerificationResult(boolean success, UUID otpId, UUID userId, OtpPurpose purpose, String error) {
        public static OtpVerificationResult success(UUID otpId, UUID userId, OtpPurpose purpose) {
            return new OtpVerificationResult(true, otpId, userId, purpose, null);
        }
        public static OtpVerificationResult failed(String error) {
            return new OtpVerificationResult(false, null, null, null, error);
        }
    }
}
