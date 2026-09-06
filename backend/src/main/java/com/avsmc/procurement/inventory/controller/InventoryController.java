package com.avsmc.procurement.inventory.controller;

import com.avsmc.procurement.inventory.dto.*;
import com.avsmc.procurement.inventory.service.InventoryService;
import jakarta.validation.Valid;
import com.avsmc.procurement.security.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final PermissionService permissionService;

    @GetMapping("/inventory")
    public ResponseEntity<List<InventoryItemDto>> listInventory() {
        permissionService.requirePermission("inventory.view");

        return ResponseEntity.ok(inventoryService.listInventory());
    }

    @PostMapping("/inventory")
    public ResponseEntity<InventoryItemDto> createInventoryItem(@Valid @RequestBody CreateInventoryItemRequest request) {
        permissionService.requirePermission("inventory.manage");

        return ResponseEntity.ok(inventoryService.createInventoryItem(request));
    }

    @GetMapping("/goods-receipts")
    public ResponseEntity<List<GoodsReceiptDto>> listGoodsReceipts() {
        permissionService.requirePermission("inventory.view");

        return ResponseEntity.ok(inventoryService.listGoodsReceipts());
    }

    @GetMapping("/delivery-notes")
    public ResponseEntity<List<DeliveryNoteDto>> listDeliveries() {
        permissionService.requirePermission("inventory.view");

        return ResponseEntity.ok(inventoryService.listDeliveries());
    }

    @PostMapping("/delivery-notes")
    public ResponseEntity<DeliveryNoteDto> createDelivery(@Valid @RequestBody CreateDeliveryRequest request) {
        permissionService.requirePermission("delivery.create");

        return ResponseEntity.ok(inventoryService.createDelivery(request));
    }
}
