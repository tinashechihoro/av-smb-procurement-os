package com.avsmc.procurement.purchasing.controller;

import com.avsmc.procurement.purchasing.dto.*;
import com.avsmc.procurement.purchasing.service.PurchasingService;
import jakarta.validation.Valid;
import com.avsmc.procurement.security.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class PurchasingController {

    private final PurchasingService purchasingService;
    private final PermissionService permissionService;

    @GetMapping("/suppliers")
    public ResponseEntity<List<SupplierDto>> listSuppliers() {
        permissionService.requirePermission("suppliers.view");

        return ResponseEntity.ok(purchasingService.listSuppliers());
    }

    @PostMapping("/suppliers")
    public ResponseEntity<SupplierDto> createSupplier(@Valid @RequestBody CreateSupplierRequest request) {
        permissionService.requirePermission("suppliers.manage");

        return ResponseEntity.ok(purchasingService.createSupplier(request));
    }

    @GetMapping("/supplier-pos")
    public ResponseEntity<List<SupplierPoDto>> listSupplierPos() {
        permissionService.requirePermission("supplier_po.view");

        return ResponseEntity.ok(purchasingService.listSupplierPos());
    }

    @PostMapping("/supplier-pos")
    public ResponseEntity<SupplierPoDto> createSupplierPo(@Valid @RequestBody CreateSupplierPoRequest request) {
        permissionService.requirePermission("supplier_po.create");

        return ResponseEntity.ok(purchasingService.createSupplierPo(request));
    }
}
