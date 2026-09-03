package com.avsmc.procurement.procurement.controller;

import com.avsmc.procurement.procurement.dto.*;
import com.avsmc.procurement.procurement.service.ProcurementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class ProcurementController {

    private final ProcurementService procurementService;

    // ── Requisitions ──────────────────────────────────────────────────────

    @GetMapping("/requisitions")
    public ResponseEntity<List<RequisitionDto>> listRequisitions() {
        return ResponseEntity.ok(procurementService.listRequisitions());
    }

    @GetMapping("/requisitions/{id}")
    public ResponseEntity<RequisitionDto> getRequisition(@PathVariable UUID id) {
        return ResponseEntity.ok(procurementService.getRequisition(id));
    }

    @PostMapping("/requisitions")
    public ResponseEntity<RequisitionDto> createRequisition(@Valid @RequestBody CreateRequisitionRequest request) {
        return ResponseEntity.ok(procurementService.createRequisition(request));
    }

    @PostMapping("/requisitions/{id}/submit")
    public ResponseEntity<RequisitionDto> submitRequisition(@PathVariable UUID id) {
        return ResponseEntity.ok(procurementService.submitRequisition(id));
    }

    // ── Quotations ────────────────────────────────────────────────────────

    @GetMapping("/quotations")
    public ResponseEntity<List<QuotationDto>> listQuotations() {
        return ResponseEntity.ok(procurementService.listQuotations());
    }

    @PostMapping("/quotations")
    public ResponseEntity<QuotationDto> createQuotation(@Valid @RequestBody CreateQuotationRequest request) {
        return ResponseEntity.ok(procurementService.createQuotation(request));
    }

    @PostMapping("/quotations/{id}/approve")
    public ResponseEntity<QuotationDto> approveQuotation(@PathVariable UUID id) {
        return ResponseEntity.ok(procurementService.approveQuotation(id));
    }

    @PostMapping("/quotations/{id}/convert-to-order")
    public ResponseEntity<OrderDto> convertToOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(procurementService.convertToOrder(id));
    }

    // ── Orders ────────────────────────────────────────────────────────────

    @GetMapping("/orders")
    public ResponseEntity<List<OrderDto>> listOrders() {
        return ResponseEntity.ok(procurementService.listOrders());
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(procurementService.getOrder(id));
    }
}
