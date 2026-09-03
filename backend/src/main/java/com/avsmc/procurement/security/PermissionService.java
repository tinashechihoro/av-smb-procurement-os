package com.avsmc.procurement.security;

import com.avsmc.procurement.shared.exception.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private static final Set<String> AV_ADMIN_PERMS = Set.of(
            "vehicles.view", "vehicles.manage", "jobs.view", "jobs.manage", "jobs.approve",
            "requisitions.view", "requisitions.create", "requisitions.submit", "requisitions.approve",
            "quotations.view", "quotations.create", "quotations.negotiate", "quotations.approve",
            "orders.view", "orders.create", "orders.approve", "orders.amend",
            "invoices.view", "reports.view", "audit.view",
            "users.manage", "roles.manage", "notifications.manage"
    );

    private static final Set<String> SMB_ADMIN_PERMS = Set.of(
            "suppliers.view", "suppliers.manage", "supplier_po.create", "supplier_po.approve",
            "inventory.view", "inventory.manage", "goods_receipt.create", "delivery.create",
            "invoices.view", "invoices.create", "invoices.approve",
            "payments.create", "payments.approve",
            "cashbook.view", "cashbook.post", "cashbook.reconcile",
            "gl.view", "gl.post_journal",
            "reports.view", "audit.view",
            "users.manage", "roles.manage", "notifications.manage"
    );

    public void requirePermission(String permission) {
        SecurityUtils securityUtils = new SecurityUtils();
        String role = securityUtils.currentRole();

        boolean hasPermission = switch (role) {
            case "AV_SYS_ADMIN" -> AV_ADMIN_PERMS.contains(permission);
            case "AV_MANAGER" -> AV_ADMIN_PERMS.contains(permission) ||
                    Set.of("requisitions.approve", "quotations.approve", "orders.approve").contains(permission);
            case "AV_ESTIMATOR" -> Set.of("vehicles.view", "vehicles.manage", "jobs.view", "jobs.manage",
                    "requisitions.view", "requisitions.create", "requisitions.submit").contains(permission);
            case "AV_TECHNICIAN" -> Set.of("vehicles.view", "jobs.view").contains(permission);
            case "AV_PROCUREMENT" -> Set.of("requisitions.view", "quotations.view", "quotations.negotiate",
                    "orders.view", "orders.create").contains(permission);
            case "AV_ACCOUNTS" -> Set.of("invoices.view", "payments.create", "reports.view").contains(permission);
            case "SMB_ADMIN" -> SMB_ADMIN_PERMS.contains(permission);
            case "SMB_MANAGER" -> SMB_ADMIN_PERMS.contains(permission);
            case "SMB_QUOTATION" -> Set.of("quotations.view", "quotations.create").contains(permission);
            case "SMB_PURCHASING" -> Set.of("suppliers.view", "supplier_po.create", "inventory.view").contains(permission);
            case "SMB_STORES" -> Set.of("inventory.view", "inventory.manage", "goods_receipt.create").contains(permission);
            case "SMB_ACCOUNTS" -> Set.of("invoices.view", "invoices.create", "payments.create", "payments.approve",
                    "cashbook.view", "cashbook.post", "cashbook.reconcile",
                    "gl.view", "gl.post_journal", "reports.view").contains(permission);
            default -> false;
        };

        if (!hasPermission) {
            throw new AccessDeniedException("Permission denied: " + permission);
        }
    }

    public void requireAvUser() {
        SecurityUtils securityUtils = new SecurityUtils();
        if (!securityUtils.isAvUser()) {
            throw new AccessDeniedException("AV workspace access required");
        }
    }

    public void requireSmbUser() {
        SecurityUtils securityUtils = new SecurityUtils();
        if (!securityUtils.isSmbUser()) {
            throw new AccessDeniedException("SMB workspace access required");
        }
    }

    public void requireManager() {
        SecurityUtils securityUtils = new SecurityUtils();
        if (!securityUtils.isAvManager() && !securityUtils.isSmbManager()) {
            throw new AccessDeniedException("Manager access required");
        }
    }
}
