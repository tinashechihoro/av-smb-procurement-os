package com.avsmc.procurement.inventory.service;

import com.avsmc.procurement.inventory.dto.*;
import com.avsmc.procurement.inventory.entity.*;
import com.avsmc.procurement.inventory.repository.*;
import com.avsmc.procurement.security.SecurityUtils;
import com.avsmc.procurement.shared.util.DocumentNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryItemRepository inventoryRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final DeliveryNoteRepository deliveryNoteRepository;
    private final SecurityUtils securityUtils;
    private final DocumentNumberGenerator docNumberGenerator;

    @Transactional(readOnly = true)
    public List<InventoryItemDto> listInventory() {
        return inventoryRepository.findByOrganisationId(securityUtils.currentOrgId())
                .stream().map(this::toItemDto).collect(Collectors.toList());
    }

    @Transactional
    public InventoryItemDto createInventoryItem(CreateInventoryItemRequest req) {
        InventoryItem item = new InventoryItem();
        item.setOrganisationId(securityUtils.currentOrgId());
        item.setPartNumber(req.getPartNumber());
        item.setDescription(req.getDescription());
        item.setQuantityOnHand(req.getQuantityOnHand() != null ? req.getQuantityOnHand() : BigDecimal.ZERO);
        item.setReorderLevel(req.getReorderLevel() != null ? req.getReorderLevel() : BigDecimal.ZERO);
        item.setUnitCost(req.getUnitCost());
        item.setSellingPrice(req.getSellingPrice());
        item.setCategory(req.getCategory());
        item.setLocationId(req.getLocationId());
        return toItemDto(inventoryRepository.save(item));
    }

    @Transactional(readOnly = true)
    public List<GoodsReceiptDto> listGoodsReceipts() {
        return goodsReceiptRepository.findByOrganisationId(securityUtils.currentOrgId())
                .stream().map(this::toReceiptDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DeliveryNoteDto> listDeliveries() {
        return deliveryNoteRepository.findByOrganisationId(securityUtils.currentOrgId())
                .stream().map(this::toDeliveryDto).collect(Collectors.toList());
    }

    @Transactional
    public DeliveryNoteDto createDelivery(CreateDeliveryRequest req) {
        DeliveryNote d = new DeliveryNote();
        d.setOrganisationId(securityUtils.currentOrgId());
        d.setAvOrderId(req.getAvOrderId());
        d.setDeliveryNumber(docNumberGenerator.nextNumber(securityUtils.currentOrgId(), "DELIVERY"));
        d.setDeliveryDate(req.getDeliveryDate());
        d.setDeliveredBy(req.getDeliveredBy());
        d.setVehicleReg(req.getVehicleReg());
        d.setNotes(req.getNotes());
        return toDeliveryDto(deliveryNoteRepository.save(d));
    }

    private InventoryItemDto toItemDto(InventoryItem i) {
        return InventoryItemDto.builder().id(i.getId()).organisationId(i.getOrganisationId())
                .locationId(i.getLocationId()).partNumber(i.getPartNumber())
                .description(i.getDescription()).quantityOnHand(i.getQuantityOnHand())
                .quantityReserved(i.getQuantityReserved())
                .quantityAvailable(i.getQuantityOnHand().subtract(i.getQuantityReserved()))
                .reorderLevel(i.getReorderLevel()).unitCost(i.getUnitCost())
                .sellingPrice(i.getSellingPrice()).category(i.getCategory()).status(i.getStatus()).build();
    }

    private GoodsReceiptDto toReceiptDto(GoodsReceipt r) {
        return GoodsReceiptDto.builder().id(r.getId()).supplierPoId(r.getSupplierPoId())
                .avOrderId(r.getAvOrderId()).receiptNumber(r.getReceiptNumber())
                .status(r.getStatus()).receivedBy(r.getReceivedBy())
                .receivedAt(r.getReceivedAt()).notes(r.getNotes()).build();
    }

    private DeliveryNoteDto toDeliveryDto(DeliveryNote d) {
        return DeliveryNoteDto.builder().id(d.getId()).avOrderId(d.getAvOrderId())
                .deliveryNumber(d.getDeliveryNumber()).status(d.getStatus())
                .deliveryDate(d.getDeliveryDate()).deliveredBy(d.getDeliveredBy())
                .vehicleReg(d.getVehicleReg()).notes(d.getNotes()).build();
    }
}
