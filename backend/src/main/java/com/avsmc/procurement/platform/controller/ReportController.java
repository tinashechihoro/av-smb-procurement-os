package com.avsmc.procurement.platform.controller;

import com.avsmc.procurement.security.SecurityUtils;
import jakarta.persistence.EntityManager;
import com.avsmc.procurement.security.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final EntityManager em;
    private final SecurityUtils securityUtils;
    private final PermissionService permissionService;

    @GetMapping("/trial-balance")
    public ResponseEntity<Map<String, Object>> trialBalance() {
        permissionService.requirePermission("reports.view");

        UUID orgId = securityUtils.currentOrgId();
        List<Object[]> rows = em.createNativeQuery(
                "SELECT account_code, account_name, account_type, normal_balance, current_balance FROM chart_of_accounts WHERE organisation_id = :orgId AND is_active = true ORDER BY account_code")
                .setParameter("orgId", orgId).getResultList();

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        List<Map<String, Object>> data = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("accountCode", r[0]);
            row.put("accountName", r[1]);
            row.put("accountType", r[2]);
            row.put("normalBalance", r[3]);
            row.put("balance", r[4]);
            data.add(row);
            java.math.BigDecimal bal = (java.math.BigDecimal) r[4];
            if ("DEBIT".equals(r[3])) totalDebits = totalDebits.add(bal);
            else totalCredits = totalCredits.add(bal);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reportTitle", "Trial Balance");
        result.put("organisationId", orgId);
        result.put("generatedAt", java.time.Instant.now());
        result.put("data", data);
        result.put("totalDebits", totalDebits);
        result.put("totalCredits", totalCredits);
        result.put("balanced", totalDebits.compareTo(totalCredits) == 0);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/debtor-ageing")
    public ResponseEntity<Map<String, Object>> debtorAgeing() {
        permissionService.requirePermission("reports.view");

        UUID orgId = securityUtils.currentOrgId();
        List<Object[]> rows = em.createNativeQuery(
                "SELECT i.invoice_number, i.to_entity, i.invoice_date, i.total_amount, i.amount_paid, " +
                "(i.total_amount - i.amount_paid) AS balance, " +
                "CURRENT_DATE - i.invoice_date AS days_outstanding " +
                "FROM invoices i WHERE i.organisation_id = :orgId AND i.total_amount > i.amount_paid ORDER BY i.invoice_date")
                .setParameter("orgId", orgId).getResultList();

        List<Map<String, Object>> data = rows.stream().map(r -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("invoiceNumber", r[0]);
            row.put("entity", r[1]);
            row.put("invoiceDate", r[2]);
            row.put("totalAmount", r[3]);
            row.put("amountPaid", r[4]);
            row.put("balance", r[5]);
            row.put("daysOutstanding", r[6]);
            return row;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("reportTitle", "Debtor Ageing", "data", data, "generatedAt", java.time.Instant.now()));
    }

    @GetMapping("/job-cost")
    public ResponseEntity<Map<String, Object>> jobCostReport() {
        permissionService.requirePermission("reports.view");

        UUID orgId = securityUtils.currentOrgId();
        List<Object[]> rows = em.createNativeQuery(
                "SELECT j.job_number, j.title, j.status, j.priority, j.estimated_total, j.actual_total, " +
                "v.registration, v.make || ' ' || v.model AS vehicle, " +
                "COALESCE(j.actual_total - j.estimated_total, 0) AS variance " +
                "FROM repair_jobs j LEFT JOIN vehicles v ON v.id = j.vehicle_id " +
                "WHERE j.organisation_id = :orgId ORDER BY j.created_at DESC")
                .setParameter("orgId", orgId).getResultList();

        List<Map<String, Object>> data = rows.stream().map(r -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("jobNumber", r[0]);
            row.put("title", r[1]);
            row.put("status", r[2]);
            row.put("priority", r[3]);
            row.put("estimatedTotal", r[4]);
            row.put("actualTotal", r[5]);
            row.put("vehicle", r[7]);
            row.put("registration", r[6]);
            row.put("variance", r[8]);
            return row;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("reportTitle", "Vehicle Job Cost Report", "data", data, "generatedAt", java.time.Instant.now()));
    }

    @GetMapping("/inventory-valuation")
    public ResponseEntity<Map<String, Object>> inventoryValuation() {
        permissionService.requirePermission("reports.view");

        UUID orgId = securityUtils.currentOrgId();
        List<Object[]> rows = em.createNativeQuery(
                "SELECT part_number, description, quantity_on_hand, quantity_reserved, " +
                "(quantity_on_hand - quantity_reserved) AS available, " +
                "COALESCE(unit_cost, 0) AS unit_cost, " +
                "COALESCE(quantity_on_hand * unit_cost, 0) AS total_value, " +
                "status, reorder_level " +
                "FROM inventory_items WHERE organisation_id = :orgId ORDER BY part_number")
                .setParameter("orgId", orgId).getResultList();

        List<Map<String, Object>> data = rows.stream().map(r -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("partNumber", r[0]);
            row.put("description", r[1]);
            row.put("onHand", r[2]);
            row.put("reserved", r[3]);
            row.put("available", r[4]);
            row.put("unitCost", r[5]);
            row.put("totalValue", r[6]);
            row.put("status", r[7]);
            row.put("reorderLevel", r[8]);
            return row;
        }).collect(Collectors.toList());

        BigDecimal totalValue = data.stream()
                .map(r -> (BigDecimal) r.get("totalValue"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ResponseEntity.ok(Map.of("reportTitle", "Inventory Valuation", "data", data, "totalValue", totalValue, "generatedAt", java.time.Instant.now()));
    }

    @GetMapping("/procurement-pipeline")
    public ResponseEntity<Map<String, Object>> procurementPipeline() {
        permissionService.requirePermission("reports.view");

        UUID orgId = securityUtils.currentOrgId();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reportTitle", "Procurement Pipeline");
        result.put("generatedAt", java.time.Instant.now());
        result.put("requisitions", em.createNativeQuery("SELECT status, count(*), COALESCE(SUM(total_estimate),0) FROM requisitions WHERE organisation_id = :orgId GROUP BY status")
                .setParameter("orgId", orgId).getResultList());
        result.put("quotations", em.createNativeQuery("SELECT status, count(*), COALESCE(SUM(total_amount),0) FROM quotations WHERE organisation_id = :orgId GROUP BY status")
                .setParameter("orgId", orgId).getResultList());
        result.put("orders", em.createNativeQuery("SELECT status, count(*), COALESCE(SUM(total_amount),0) FROM av_orders WHERE organisation_id = :orgId GROUP BY status")
                .setParameter("orgId", orgId).getResultList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/csv/{reportType}")
    public ResponseEntity<byte[]> exportCsv(@PathVariable String reportType) {
        permissionService.requirePermission("reports.view");

        UUID orgId = securityUtils.currentOrgId();
        StringBuilder csv = new StringBuilder();

        switch (reportType) {
            case "invoices" -> {
                csv.append("Invoice Number,From,To,Status,Total,Paid,Balance,Date\n");
                em.createNativeQuery("SELECT invoice_number, from_entity, to_entity, status, total_amount, amount_paid, (total_amount - amount_paid), invoice_date FROM invoices WHERE organisation_id = :orgId")
                        .setParameter("orgId", orgId).getResultList().forEach(r -> {
                            Object[] row = (Object[]) r;
                            csv.append(row[0]).append(",").append(row[1]).append(",").append(row[2]).append(",")
                                    .append(row[3]).append(",").append(row[4]).append(",").append(row[5]).append(",")
                                    .append(row[6]).append(",").append(row[7]).append("\n");
                        });
            }
            case "orders" -> {
                csv.append("Order Number,Status,Date,Subtotal,Tax,Total\n");
                em.createNativeQuery("SELECT order_number, status, order_date, subtotal, tax_amount, total_amount FROM av_orders WHERE organisation_id = :orgId")
                        .setParameter("orgId", orgId).getResultList().forEach(r -> {
                            Object[] row = (Object[]) r;
                            csv.append(row[0]).append(",").append(row[1]).append(",").append(row[2]).append(",")
                                    .append(row[3]).append(",").append(row[4]).append(",").append(row[5]).append("\n");
                        });
            }
            case "inventory" -> {
                csv.append("Part Number,Description,On Hand,Reserved,Available,Unit Cost,Status\n");
                em.createNativeQuery("SELECT part_number, description, quantity_on_hand, quantity_reserved, (quantity_on_hand - quantity_reserved), COALESCE(unit_cost,0), status FROM inventory_items WHERE organisation_id = :orgId")
                        .setParameter("orgId", orgId).getResultList().forEach(r -> {
                            Object[] row = (Object[]) r;
                            csv.append(row[0]).append(",\"").append(row[1]).append("\",").append(row[2]).append(",")
                                    .append(row[3]).append(",").append(row[4]).append(",").append(row[5]).append(",")
                                    .append(row[6]).append("\n");
                        });
            }
            case "audit" -> {
                csv.append("Timestamp,Action,Entity Type,Entity ID,User ID\n");
                em.createNativeQuery("SELECT created_at, action, entity_type, entity_id, user_id FROM audit.audit_log WHERE organisation_id = :orgId ORDER BY created_at DESC LIMIT 500")
                        .setParameter("orgId", orgId).getResultList().forEach(r -> {
                            Object[] row = (Object[]) r;
                            csv.append(row[0]).append(",").append(row[1]).append(",").append(row[2]).append(",")
                                    .append(row[3]).append(",").append(row[4]).append("\n");
                        });
            }
            default -> { return ResponseEntity.badRequest().build(); }
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + reportType + "-report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString().getBytes());
    }
}
