package com.avsmc.procurement.finance.controller;

import com.avsmc.procurement.finance.dto.*;
import com.avsmc.procurement.finance.service.FinanceService;
import jakarta.validation.Valid;
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

    @GetMapping("/chart-of-accounts")
    public ResponseEntity<List<ChartOfAccountDto>> listAccounts() {
        return ResponseEntity.ok(financeService.listAccounts());
    }

    @GetMapping("/cash-accounts")
    public ResponseEntity<List<CashAccountDto>> listCashAccounts() {
        return ResponseEntity.ok(financeService.listCashAccounts());
    }

    @GetMapping("/journals")
    public ResponseEntity<List<JournalDto>> listJournals() {
        return ResponseEntity.ok(financeService.listJournals());
    }

    @PostMapping("/journals")
    public ResponseEntity<JournalDto> createJournal(@Valid @RequestBody CreateJournalRequest request) {
        return ResponseEntity.ok(financeService.createManualJournal(request));
    }

    @PostMapping("/journals/{id}/post")
    public ResponseEntity<JournalDto> postJournal(@PathVariable UUID id) {
        return ResponseEntity.ok(financeService.postJournal(id));
    }

    @GetMapping("/invoices")
    public ResponseEntity<List<InvoiceDto>> listInvoices() {
        return ResponseEntity.ok(financeService.listInvoices());
    }

    @PostMapping("/invoices")
    public ResponseEntity<InvoiceDto> createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
        return ResponseEntity.ok(financeService.createInvoice(request));
    }

    @GetMapping("/payments")
    public ResponseEntity<List<PaymentDto>> listPayments() {
        return ResponseEntity.ok(financeService.listPayments());
    }

    @PostMapping("/payments")
    public ResponseEntity<PaymentDto> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.ok(financeService.createPayment(request));
    }

    @GetMapping("/cashbook")
    public ResponseEntity<List<CashbookEntryDto>> listCashbook() {
        return ResponseEntity.ok(financeService.listCashbookEntries());
    }

    @PostMapping("/cashbook")
    public ResponseEntity<CashbookEntryDto> createCashbookEntry(@Valid @RequestBody CreateCashbookEntryRequest request) {
        return ResponseEntity.ok(financeService.createCashbookEntry(request));
    }
}
