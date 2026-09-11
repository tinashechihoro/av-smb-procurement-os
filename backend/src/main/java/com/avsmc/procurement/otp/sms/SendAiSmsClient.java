package com.avsmc.procurement.otp.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@Slf4j
public class SendAiSmsClient {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String baseUrl;

    public SendAiSmsClient(
            @Value("${sms.sendai.api-key:}") String apiKey,
            @Value("${sms.sendai.base-url:https://app.sendai.co.zw}") String baseUrl) {
        this.restTemplate = new RestTemplate();
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    public SmsSendResult sendOtp(String phoneNumber, String otpCode, String purpose) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("SendAI API key not configured, OTP not sent: {} for {}", otpCode, phoneNumber);
            return SmsSendResult.skipped("API key not configured");
        }

        String message = buildOtpMessage(otpCode, purpose);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, String> body = Map.of(
                    "to", normalizePhone(phoneNumber),
                    "message", message
            );

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/api/sms/send",
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("OTP SMS sent to {} for {}: response={}", phoneNumber, purpose, response.getBody());
                return SmsSendResult.success("sent");
            } else {
                log.error("SendAI SMS failed: status={}, body={}", response.getStatusCode(), response.getBody());
                return SmsSendResult.failed("HTTP " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("SendAI SMS error for {}: {}", phoneNumber, e.getMessage());
            return SmsSendResult.failed(e.getMessage());
        }
    }

    private String buildOtpMessage(String otpCode, String purpose) {
        String action = switch (purpose.toUpperCase()) {
            case "LOGIN" -> "login verification";
            case "APPROVAL" -> "approval authorization";
            case "SIGNATURE" -> "document signature";
            case "PASSWORD_RESET" -> "password reset";
            case "MFA" -> "multi-factor authentication";
            default -> "verification";
        };
        return String.format("AV Motors: Your %s code is %s. Valid for 5 minutes. Do not share this code.", action, otpCode);
    }

    private String normalizePhone(String phone) {
        if (phone == null) return "";
        String cleaned = phone.replaceAll("[^0-9+]", "");
        if (cleaned.startsWith("0")) {
            cleaned = "+263" + cleaned.substring(1);
        }
        if (!cleaned.startsWith("+")) {
            cleaned = "+" + cleaned;
        }
        return cleaned;
    }

    public record SmsSendResult(boolean success, String messageId, String error, boolean skipped) {
        public static SmsSendResult success(String messageId) {
            return new SmsSendResult(true, messageId, null, false);
        }
        public static SmsSendResult failed(String error) {
            return new SmsSendResult(false, null, error, false);
        }
        public static SmsSendResult skipped(String reason) {
            return new SmsSendResult(false, null, reason, true);
        }
    }
}
