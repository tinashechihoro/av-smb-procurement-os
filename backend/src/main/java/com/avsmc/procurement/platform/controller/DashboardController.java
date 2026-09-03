package com.avsmc.procurement.platform.controller;

import com.avsmc.procurement.security.SecurityUtils;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final EntityManager em;
    private final SecurityUtils securityUtils;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        UUID orgId = securityUtils.currentOrgId();
        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("vehicles", count("vehicles", orgId));
        stats.put("repairJobs", count("repair_jobs", orgId));
        stats.put("activeJobs", countByStatus("repair_jobs", orgId, List.of("ASSESSMENT","ESTIMATING","IN_REPAIR","PAINTING","REASSEMBLY","QC")));
        stats.put("requisitions", count("requisitions", orgId));
        stats.put("draftRequisitions", countByStatus("requisitions", orgId, List.of("DRAFT")));
        stats.put("pendingRequisitions", countByStatus("requisitions", orgId, List.of("SUBMITTED","UNDER_REVIEW","CLARIFICATION")));
        stats.put("quotations", count("quotations", orgId));
        stats.put("pendingQuotations", countByStatus("quotations", orgId, List.of("DRAFT","SUBMITTED","UNDER_NEGOTIATION")));
        stats.put("orders", count("av_orders", orgId));
        stats.put("activeOrders", countByStatus("av_orders", orgId, List.of("CONFIRMED","PARTIAL_DELIVERY")));
        stats.put("suppliers", count("suppliers", orgId));
        stats.put("inventoryItems", count("inventory_items", orgId));
        stats.put("lowStockItems", em.createNativeQuery("SELECT count(*) FROM inventory_items WHERE organisation_id = :orgId AND quantity_on_hand - quantity_reserved <= reorder_level AND quantity_on_hand > 0")
                .setParameter("orgId", orgId).getSingleResult());
        stats.put("goodsReceipts", count("goods_receipts", orgId));
        stats.put("deliveries", count("delivery_notes", orgId));
        stats.put("invoices", count("invoices", orgId));
        stats.put("payments", count("payments", orgId));
        stats.put("journals", count("journals", orgId));
        stats.put("cashbookEntries", count("cashbook_entries", orgId));
        stats.put("notifications", em.createNativeQuery("SELECT count(*) FROM notifications WHERE user_id = :userId AND is_read = false")
                .setParameter("userId", securityUtils.currentUserId()).getSingleResult());

        // Financial summaries
        stats.put("totalInvoiceValue", em.createNativeQuery("SELECT COALESCE(SUM(total_amount),0) FROM invoices WHERE organisation_id = :orgId")
                .setParameter("orgId", orgId).getSingleResult());
        stats.put("totalPaid", em.createNativeQuery("SELECT COALESCE(SUM(amount_paid),0) FROM invoices WHERE organisation_id = :orgId")
                .setParameter("orgId", orgId).getSingleResult());
        stats.put("totalPayments", em.createNativeQuery("SELECT COALESCE(SUM(amount),0) FROM payments WHERE organisation_id = :orgId AND status = 'COMPLETED'")
                .setParameter("orgId", orgId).getSingleResult());
        stats.put("inventoryValue", em.createNativeQuery("SELECT COALESCE(SUM(quantity_on_hand * unit_cost),0) FROM inventory_items WHERE organisation_id = :orgId AND unit_cost IS NOT NULL")
                .setParameter("orgId", orgId).getSingleResult());

        // Job cost summary
        stats.put("totalJobEstimated", em.createNativeQuery("SELECT COALESCE(SUM(estimated_total),0) FROM repair_jobs WHERE organisation_id = :orgId")
                .setParameter("orgId", orgId).getSingleResult());
        stats.put("totalJobActual", em.createNativeQuery("SELECT COALESCE(SUM(actual_total),0) FROM repair_jobs WHERE organisation_id = :orgId")
                .setParameter("orgId", orgId).getSingleResult());

        // Status breakdowns
        stats.put("jobStatusBreakdown", em.createNativeQuery("SELECT status, count(*) FROM repair_jobs WHERE organisation_id = :orgId GROUP BY status ORDER BY count(*) DESC")
                .setParameter("orgId", orgId).getResultList());
        stats.put("requisitionStatusBreakdown", em.createNativeQuery("SELECT status, count(*) FROM requisitions WHERE organisation_id = :orgId GROUP BY status ORDER BY count(*) DESC")
                .setParameter("orgId", orgId).getResultList());
        stats.put("orderStatusBreakdown", em.createNativeQuery("SELECT status, count(*) FROM av_orders WHERE organisation_id = :orgId GROUP BY status ORDER BY count(*) DESC")
                .setParameter("orgId", orgId).getResultList());
        stats.put("invoiceStatusBreakdown", em.createNativeQuery("SELECT status, count(*) FROM invoices WHERE organisation_id = :orgId GROUP BY status ORDER BY count(*) DESC")
                .setParameter("orgId", orgId).getResultList());

        // Recent activity
        stats.put("recentAuditEvents", em.createNativeQuery("SELECT action, entity_type, entity_id, created_at FROM audit.audit_log WHERE organisation_id = :orgId ORDER BY created_at DESC LIMIT 20")
                .setParameter("orgId", orgId).getResultList());

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/finance-summary")
    public ResponseEntity<Map<String, Object>> getFinanceSummary() {
        UUID orgId = securityUtils.currentOrgId();
        Map<String, Object> summary = new LinkedHashMap<>();

        // Cash account balances
        summary.put("cashAccounts", em.createNativeQuery("SELECT id, account_name, account_type, balance, currency FROM cash_accounts WHERE organisation_id = :orgId AND is_active = true")
                .setParameter("orgId", orgId).getResultList());
        summary.put("totalCashBalance", em.createNativeQuery("SELECT COALESCE(SUM(balance),0) FROM cash_accounts WHERE organisation_id = :orgId AND is_active = true")
                .setParameter("orgId", orgId).getSingleResult());

        // Debtor ageing
        summary.put("debtorAgeing", em.createNativeQuery(
                "SELECT CASE WHEN CURRENT_DATE - invoice_date < 30 THEN '0-30' WHEN CURRENT_DATE - invoice_date < 60 THEN '31-60' WHEN CURRENT_DATE - invoice_date < 90 THEN '61-90' ELSE '90+' END AS bucket, " +
                "count(*) AS count, COALESCE(SUM(total_amount - amount_paid),0) AS amount " +
                "FROM invoices WHERE organisation_id = :orgId AND total_amount > amount_paid GROUP BY bucket ORDER BY bucket")
                .setParameter("orgId", orgId).getResultList());

        // Trial balance
        summary.put("trialBalance", em.createNativeQuery(
                "SELECT account_code, account_name, account_type, current_balance, normal_balance FROM chart_of_accounts WHERE organisation_id = :orgId AND is_active = true ORDER BY account_code")
                .setParameter("orgId", orgId).getResultList());

        // Monthly revenue
        summary.put("monthlyRevenue", em.createNativeQuery(
                "SELECT DATE_TRUNC('month', invoice_date) AS month, COALESCE(SUM(total_amount),0) AS revenue " +
                "FROM invoices WHERE organisation_id = :orgId AND invoice_date >= CURRENT_DATE - INTERVAL '6 months' GROUP BY month ORDER BY month")
                .setParameter("orgId", orgId).getResultList());

        return ResponseEntity.ok(summary);
    }

    private long count(String table, UUID orgId) {
        return ((Number) em.createNativeQuery("SELECT count(*) FROM " + table + " WHERE organisation_id = :orgId")
                .setParameter("orgId", orgId).getSingleResult()).longValue();
    }

    private long countByStatus(String table, UUID orgId, List<String> statuses) {
        if (statuses.isEmpty()) return 0;
        StringBuilder sql = new StringBuilder("SELECT count(*) FROM " + table + " WHERE organisation_id = :orgId AND status IN (");
        for (int i = 0; i < statuses.size(); i++) sql.append(":s").append(i).append(i < statuses.size() - 1 ? "," : "");
        sql.append(")");
        var q = em.createNativeQuery(sql.toString()).setParameter("orgId", orgId);
        for (int i = 0; i < statuses.size(); i++) q.setParameter("s" + i, statuses.get(i));
        return ((Number) q.getSingleResult()).longValue();
    }
}
