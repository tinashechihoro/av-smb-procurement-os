package com.avsmc.procurement.finance.controller;

import com.avsmc.procurement.finance.dto.*;
import com.avsmc.procurement.finance.service.FinanceService;
import jakarta.validation.Valid;
import com.avsmc.procurement.security.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;
    private final PermissionService permissionService;

    @GetMapping("/chart-of-accounts")
    public ResponseEntity<List<ChartOfAccountDto>> listAccounts() {
        permissionService.requirePermission("gl.view");

        return ResponseEntity.ok(financeService.listAccounts());
    }

    @GetMapping("/cash-accounts")
    public ResponseEntity<List<CashAccountDto>> listCashAccounts() {
        permissionService.requirePermission("gl.view");

        return ResponseEntity.ok(financeService.listCashAccounts());
    }

    @GetMapping("/journals")
    public ResponseEntity<List<JournalDto>> listJournals() {
        permissionService.requirePermission("gl.view");

        return ResponseEntity.ok(financeService.listJournals());
    }

    @PostMapping("/journals")
    public ResponseEntity<JournalDto> createJournal(@Valid @RequestBody CreateJournalRequest request) {
        permissionService.requirePermission("gl.post_journal");

        return ResponseEntity.ok(financeService.createManualJournal(request));
    }

    @PostMapping("/journals/{id}/post")
    public ResponseEntity<JournalDto> postJournal(@PathVariable UUID id) {
        permissionService.requirePermission("gl.post_journal");

        return ResponseEntity.ok(financeService.postJournal(id));
    }

    @GetMapping("/invoices")
    public ResponseEntity<List<InvoiceDto>> listInvoices() {
        permissionService.requirePermission("invoices.view");

        return ResponseEntity.ok(financeService.listInvoices());
    }

    @PostMapping("/invoices")
    public ResponseEntity<InvoiceDto> createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
        permissionService.requirePermission("invoices.create");

        return ResponseEntity.ok(financeService.createInvoice(request));
    }

    @GetMapping("/payments")
    public ResponseEntity<List<PaymentDto>> listPayments() {
        permissionService.requirePermission("payments.view");

        return ResponseEntity.ok(financeService.listPayments());
    }

    @PostMapping("/payments")
    public ResponseEntity<PaymentDto> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        permissionService.requirePermission("payments.create");

        return ResponseEntity.ok(financeService.createPayment(request));
    }

    @GetMapping("/cashbook")
    public ResponseEntity<List<CashbookEntryDto>> listCashbook() {
        permissionService.requirePermission("cashbook.view");

        return ResponseEntity.ok(financeService.listCashbookEntries());
    }

    @PostMapping("/cashbook")
    public ResponseEntity<CashbookEntryDto> createCashbookEntry(@Valid @RequestBody CreateCashbookEntryRequest request) {
        permissionService.requirePermission("cashbook.post");

        return ResponseEntity.ok(financeService.createCashbookEntry(request));
    }
}
