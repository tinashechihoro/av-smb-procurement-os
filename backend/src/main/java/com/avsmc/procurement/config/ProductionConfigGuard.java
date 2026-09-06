package com.avsmc.procurement.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * Fails startup in the production profile if security-sensitive configuration
 * is missing or still set to a development default. Prevents an accidentally
 * unconfigured deployment from going live with known credentials.
 */
@Slf4j
@Configuration
@org.springframework.context.annotation.Profile("prod")
public class ProductionConfigGuard {

    private static final String DEV_JWT_SECRET = "dev-only-jwt-secret-avsmc-platform-min-32-bytes";
    private static final String DEV_DB_PASSWORD = "avsmc_s3cur3_2026";
    private static final String DEFAULT_SEED_ADMIN_PASSWORD = "Admin@123";

    private final String jwtSecret;
    private final String dbPassword;
    private final String seedAdminPassword;
    private final String corsOrigins;

    public ProductionConfigGuard(
            @Value("${app.jwt.secret:}") String jwtSecret,
            @Value("${spring.datasource.password:}") String dbPassword,
            @Value("${spring.flyway.placeholders.seedAdminPassword:}") String seedAdminPassword,
            @Value("${app.cors.allowed-origins:}") String corsOrigins) {
        this.jwtSecret = jwtSecret;
        this.dbPassword = dbPassword;
        this.seedAdminPassword = seedAdminPassword;
        this.corsOrigins = corsOrigins;
    }

    @PostConstruct
    void validate() {
        List<String> failures = new ArrayList<>();

        if (jwtSecret == null || jwtSecret.isBlank()) {
            failures.add("JWT_SECRET is not set. Generate one with: openssl rand -base64 64");
        } else if (DEV_JWT_SECRET.equals(jwtSecret)) {
            failures.add("JWT_SECRET is set to the development default. Set a production-specific secret.");
        } else if (jwtSecret.getBytes().length < 32) {
            failures.add("JWT_SECRET must be at least 32 bytes.");
        }

        if (dbPassword == null || dbPassword.isBlank()) {
            failures.add("DB_PASSWORD is not set.");
        } else if (DEV_DB_PASSWORD.equals(dbPassword)) {
            failures.add("DB_PASSWORD is set to the development default. Set a production-specific password.");
        }

        if (corsOrigins == null || corsOrigins.isBlank() || corsOrigins.contains("localhost")) {
            failures.add("CORS_ORIGINS must list only production origins (no localhost).");
        }

        if (!failures.isEmpty()) {
            throw new IllegalStateException(
                    "Production configuration validation failed:\n  - " + String.join("\n  - ", failures));
        }

        if (DEFAULT_SEED_ADMIN_PASSWORD.equals(seedAdminPassword)) {
            log.warn("SEED_ADMIN_PASSWORD was not overridden — the seeded admin accounts keep the default "
                    + "password. Set SEED_ADMIN_PASSWORD before first deployment or rotate the admin "
                    + "passwords immediately after go-live.");
        }
    }
}
