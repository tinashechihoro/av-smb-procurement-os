package com.avsmc.procurement.purchasing.controller;

import com.avsmc.procurement.purchasing.dto.*;
import com.avsmc.procurement.purchasing.service.PurchasingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class PurchasingController {

    private final PurchasingService purchasingService;

    @GetMapping("/suppliers")
    public ResponseEntity<List<SupplierDto>> listSuppliers() {
        return ResponseEntity.ok(purchasingService.listSuppliers());
    }

    @PostMapping("/suppliers")
    public ResponseEntity<SupplierDto> createSupplier(@Valid @RequestBody CreateSupplierRequest request) {
        return ResponseEntity.ok(purchasingService.createSupplier(request));
    }

    @GetMapping("/supplier-pos")
    public ResponseEntity<List<SupplierPoDto>> listSupplierPos() {
        return ResponseEntity.ok(purchasingService.listSupplierPos());
    }

    @PostMapping("/supplier-pos")
    public ResponseEntity<SupplierPoDto> createSupplierPo(@Valid @RequestBody CreateSupplierPoRequest request) {
        return ResponseEntity.ok(purchasingService.createSupplierPo(request));
    }
}
