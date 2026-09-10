package com.avsmc.procurement.config.audit;

import java.util.UUID;

/**
 * Holds the current request's actor identity for the audit pipeline.
 * Populated by {@link AuditContextFilter}, consumed by
 * {@link AuditAwareDataSourcePostProcessor} when syncing PostgreSQL session
 * variables used by the audit triggers.
 */
public final class AuditContext {

    private static final ThreadLocal<Actor> CURRENT = new ThreadLocal<>();

    public record Actor(UUID userId, UUID organisationId) {}

    private AuditContext() {}

    public static void set(Actor actor) {
        CURRENT.set(actor);
    }

    public static Actor get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
