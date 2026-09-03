package com.avsmc.procurement.purchasing.service;

import com.avsmc.procurement.purchasing.dto.*;
import com.avsmc.procurement.purchasing.entity.*;
import com.avsmc.procurement.purchasing.repository.*;
import com.avsmc.procurement.security.SecurityUtils;
import com.avsmc.procurement.shared.exception.ResourceNotFoundException;
import com.avsmc.procurement.shared.util.DocumentNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchasingService {

    private final SupplierRepository supplierRepository;
    private final SupplierPurchaseOrderRepository poRepository;
    private final SecurityUtils securityUtils;
    private final DocumentNumberGenerator docNumberGenerator;

    @Transactional(readOnly = true)
    public List<SupplierDto> listSuppliers() {
        return supplierRepository.findByOrganisationId(securityUtils.currentOrgId())
                .stream().map(this::toSupplierDto).collect(Collectors.toList());
    }

    @Transactional
    public SupplierDto createSupplier(CreateSupplierRequest req) {
        Supplier s = new Supplier();
        s.setOrganisationId(securityUtils.currentOrgId());
        s.setName(req.getName());
        s.setContactPerson(req.getContactPerson());
        s.setEmail(req.getEmail());
        s.setPhone(req.getPhone());
        s.setAddress(req.getAddress());
        s.setTaxNumber(req.getTaxNumber());
        s.setPaymentTerms(req.getPaymentTerms() != null ? req.getPaymentTerms() : 30);
        s.setCurrency(req.getCurrency() != null ? req.getCurrency() : "USD");
        s.setNotes(req.getNotes());
        return toSupplierDto(supplierRepository.save(s));
    }

    @Transactional(readOnly = true)
    public List<SupplierPoDto> listSupplierPos() {
        return poRepository.findByOrganisationId(securityUtils.currentOrgId())
                .stream().map(this::toPoDto).collect(Collectors.toList());
    }

    @Transactional
    public SupplierPoDto createSupplierPo(CreateSupplierPoRequest req) {
        supplierRepository.findById(req.getSupplierId()).orElseThrow(() -> new ResourceNotFoundException("Supplier", req.getSupplierId()));
        SupplierPurchaseOrder po = new SupplierPurchaseOrder();
        po.setOrganisationId(securityUtils.currentOrgId());
        po.setSupplierId(req.getSupplierId());
        po.setAvOrderId(req.getAvOrderId());
        po.setPoNumber(docNumberGenerator.nextNumber(securityUtils.currentOrgId(), "SUPPLIER_PO"));
        po.setExpectedDelivery(req.getExpectedDelivery());
        po.setNotes(req.getNotes());

        List<SupplierPoItem> items = new ArrayList<>();
        int lineNum = 1;
        for (var item : req.getItems()) {
            SupplierPoItem pi = new SupplierPoItem();
            pi.setPoId(po.getId());
            pi.setAvOrderItemId(item.getAvOrderItemId());
            pi.setLineNumber(lineNum++);
            pi.setDescription(item.getDescription());
            pi.setQuantity(item.getQuantity());
            pi.setUnitCost(item.getUnitCost());
            pi.setFreightAlloc(item.getFreightAlloc() != null ? item.getFreightAlloc() : BigDecimal.ZERO);
            pi.setLineTotal(item.getQuantity().multiply(item.getUnitCost()).add(pi.getFreightAlloc()));
            items.add(pi);
        }
        po.setItems(items);
        BigDecimal sub = items.stream().map(i -> i.getQuantity().multiply(i.getUnitCost())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal freight = items.stream().map(SupplierPoItem::getFreightAlloc).reduce(BigDecimal.ZERO, BigDecimal::add);
        po.setSubtotal(sub);
        po.setFreightTotal(freight);
        po.setTotalAmount(sub.add(freight));
        return toPoDto(poRepository.save(po));
    }

    private SupplierDto toSupplierDto(Supplier s) {
        return SupplierDto.builder().id(s.getId()).name(s.getName())
                .contactPerson(s.getContactPerson()).email(s.getEmail())
                .phone(s.getPhone()).address(s.getAddress())
                .taxNumber(s.getTaxNumber()).paymentTerms(s.getPaymentTerms())
                .currency(s.getCurrency()).isActive(s.getIsActive())
                .rating(s.getRating()).notes(s.getNotes()).build();
    }

    private SupplierPoDto toPoDto(SupplierPurchaseOrder po) {
        return SupplierPoDto.builder().id(po.getId())
                .supplierId(po.getSupplierId()).avOrderId(po.getAvOrderId())
                .poNumber(po.getPoNumber()).status(po.getStatus())
                .orderDate(po.getOrderDate()).subtotal(po.getSubtotal())
                .freightTotal(po.getFreightTotal()).totalAmount(po.getTotalAmount())
                .currency(po.getCurrency()).expectedDelivery(po.getExpectedDelivery())
                .notes(po.getNotes())
                .items(po.getItems().stream().map(i -> SupplierPoItemDto.builder()
                        .id(i.getId()).lineNumber(i.getLineNumber())
                        .description(i.getDescription()).quantity(i.getQuantity())
                        .unitCost(i.getUnitCost()).freightAlloc(i.getFreightAlloc())
                        .lineTotal(i.getLineTotal()).receivedQty(i.getReceivedQty())
                        .build()).collect(Collectors.toList())).build();
    }
}
