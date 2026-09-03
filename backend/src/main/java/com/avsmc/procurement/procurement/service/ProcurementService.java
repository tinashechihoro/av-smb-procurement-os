package com.avsmc.procurement.procurement.service;

import com.avsmc.procurement.procurement.dto.*;
import com.avsmc.procurement.procurement.entity.*;
import com.avsmc.procurement.procurement.repository.*;
import com.avsmc.procurement.security.SecurityUtils;
import com.avsmc.procurement.shared.exception.BusinessRuleException;
import com.avsmc.procurement.shared.exception.ResourceNotFoundException;
import com.avsmc.procurement.shared.util.DocumentNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProcurementService {

    private final RequisitionRepository requisitionRepository;
    private final QuotationRepository quotationRepository;
    private final AvOrderRepository orderRepository;
    private final SecurityUtils securityUtils;
    private final DocumentNumberGenerator docNumberGenerator;

    @Transactional(readOnly = true)
    public List<RequisitionDto> listRequisitions() {
        return requisitionRepository.findByOrganisationId(securityUtils.currentOrgId())
                .stream().map(this::toReqDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RequisitionDto getRequisition(UUID id) {
        return toReqDto(requisitionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Requisition", id)));
    }

    @Transactional
    public RequisitionDto createRequisition(CreateRequisitionRequest req) {
        Requisition r = new Requisition();
        r.setOrganisationId(securityUtils.currentOrgId());
        r.setRepairJobId(req.getRepairJobId());
        r.setRequisitionNumber(docNumberGenerator.nextNumber(securityUtils.currentOrgId(), "REQUISITION"));
        r.setTitle(req.getTitle());
        r.setPriority(req.getPriority() != null ? req.getPriority() : "NORMAL");
        r.setRequestedBy(securityUtils.currentUserId());
        r.setNotes(req.getNotes());

        List<RequisitionItem> items = new ArrayList<>();
        int lineNum = 1;
        BigDecimal total = BigDecimal.ZERO;
        for (var item : req.getItems()) {
            RequisitionItem ri = new RequisitionItem();
            ri.setRequisitionId(r.getId());
            ri.setLineNumber(lineNum++);
            ri.setPartNumber(item.getPartNumber());
            ri.setDescription(item.getDescription());
            ri.setQuantity(item.getQuantity() != null ? item.getQuantity() : BigDecimal.ONE);
            ri.setUnitOfMeasure(item.getUnitOfMeasure() != null ? item.getUnitOfMeasure() : "EA");
            ri.setEstimatedCost(item.getEstimatedCost());
            ri.setNotes(item.getNotes());
            items.add(ri);
            if (item.getEstimatedCost() != null) total = total.add(item.getEstimatedCost().multiply(ri.getQuantity()));
        }
        r.setItems(items);
        r.setTotalEstimate(total);
        return toReqDto(requisitionRepository.save(r));
    }

    @Transactional
    public RequisitionDto submitRequisition(UUID id) {
        Requisition r = requisitionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Requisition", id));
        if (!"DRAFT".equals(r.getStatus())) throw new BusinessRuleException("Only draft requisitions can be submitted");
        r.setStatus("SUBMITTED");
        r.setSubmittedAt(Instant.now());
        return toReqDto(requisitionRepository.save(r));
    }

    @Transactional(readOnly = true)
    public List<QuotationDto> listQuotations() {
        return quotationRepository.findByOrganisationId(securityUtils.currentOrgId())
                .stream().map(this::toQuotationDto).collect(Collectors.toList());
    }

    @Transactional
    public QuotationDto createQuotation(CreateQuotationRequest req) {
        requisitionRepository.findById(req.getRequisitionId()).orElseThrow(() -> new ResourceNotFoundException("Requisition", req.getRequisitionId()));
        Quotation q = new Quotation();
        q.setOrganisationId(securityUtils.currentOrgId());
        q.setRequisitionId(req.getRequisitionId());
        q.setQuotationNumber(docNumberGenerator.nextNumber(securityUtils.currentOrgId(), "QUOTATION"));
        q.setTitle(req.getTitle());
        q.setTaxRate(req.getTaxRate() != null ? req.getTaxRate() : BigDecimal.ZERO);
        q.setValidUntil(req.getValidUntil());
        q.setPreparedBy(securityUtils.currentUserId());
        q.setNotes(req.getNotes());

        List<QuotationItem> items = new ArrayList<>();
        int lineNum = 1;
        for (var item : req.getItems()) {
            QuotationItem qi = new QuotationItem();
            qi.setQuotationId(q.getId());
            qi.setRequisitionItemId(item.getRequisitionItemId());
            qi.setLineNumber(lineNum++);
            qi.setPartNumber(item.getPartNumber());
            qi.setDescription(item.getDescription());
            qi.setQuantity(item.getQuantity() != null ? item.getQuantity() : BigDecimal.ONE);
            qi.setUnitPrice(item.getUnitPrice());
            qi.setDiscountPct(item.getDiscountPct() != null ? item.getDiscountPct() : BigDecimal.ZERO);
            BigDecimal lt = qi.getQuantity().multiply(qi.getUnitPrice()).multiply(BigDecimal.ONE.subtract(qi.getDiscountPct().divide(BigDecimal.valueOf(100))));
            qi.setLineTotal(lt);
            qi.setNotes(item.getNotes());
            items.add(qi);
        }
        q.setItems(items);
        BigDecimal sub = items.stream().map(QuotationItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tax = sub.multiply(q.getTaxRate()).divide(BigDecimal.valueOf(100));
        q.setSubtotal(sub);
        q.setTaxAmount(tax);
        q.setTotalAmount(sub.add(tax));
        return toQuotationDto(quotationRepository.save(q));
    }

    @Transactional
    public QuotationDto approveQuotation(UUID id) {
        Quotation q = quotationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Quotation", id));
        q.setStatus("APPROVED");
        return toQuotationDto(quotationRepository.save(q));
    }

    @Transactional
    public OrderDto convertToOrder(UUID quotationId) {
        Quotation q = quotationRepository.findById(quotationId).orElseThrow(() -> new ResourceNotFoundException("Quotation", quotationId));
        if (!"APPROVED".equals(q.getStatus())) throw new BusinessRuleException("Only approved quotations can be converted to orders");
        AvOrder o = new AvOrder();
        o.setOrganisationId(securityUtils.currentOrgId());
        o.setQuotationId(q.getId());
        o.setOrderNumber(docNumberGenerator.nextNumber(securityUtils.currentOrgId(), "ORDER"));
        o.setStatus("CONFIRMED");
        o.setSubtotal(q.getSubtotal());
        o.setTaxAmount(q.getTaxAmount());
        o.setTotalAmount(q.getTotalAmount());
        o.setCurrency(q.getCurrency());

        List<AvOrderItem> orderItems = new ArrayList<>();
        for (var qi : q.getItems()) {
            AvOrderItem oi = new AvOrderItem();
            oi.setOrderId(o.getId());
            oi.setQuotationItemId(qi.getId());
            oi.setLineNumber(qi.getLineNumber());
            oi.setPartNumber(qi.getPartNumber());
            oi.setDescription(qi.getDescription());
            oi.setQuantity(qi.getQuantity());
            oi.setUnitPrice(qi.getUnitPrice());
            oi.setLineTotal(qi.getLineTotal());
            orderItems.add(oi);
        }
        o.setItems(orderItems);
        q.setStatus("CONVERTED_TO_ORDER");
        quotationRepository.save(q);
        return toOrderDto(orderRepository.save(o));
    }

    @Transactional(readOnly = true)
    public List<OrderDto> listOrders() {
        return orderRepository.findByOrganisationId(securityUtils.currentOrgId()).stream().map(this::toOrderDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDto getOrder(UUID id) {
        return toOrderDto(orderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order", id)));
    }

    private RequisitionDto toReqDto(Requisition r) {
        return RequisitionDto.builder().id(r.getId()).organisationId(r.getOrganisationId())
                .repairJobId(r.getRepairJobId()).requisitionNumber(r.getRequisitionNumber())
                .title(r.getTitle()).status(r.getStatus()).priority(r.getPriority())
                .requestedBy(r.getRequestedBy()).totalEstimate(r.getTotalEstimate())
                .amendmentNumber(r.getAmendmentNumber()).notes(r.getNotes())
                .submittedAt(r.getSubmittedAt()).approvedAt(r.getApprovedAt())
                .items(r.getItems().stream().map(i -> RequisitionItemDto.builder().id(i.getId())
                        .lineNumber(i.getLineNumber()).partNumber(i.getPartNumber())
                        .description(i.getDescription()).quantity(i.getQuantity())
                        .unitOfMeasure(i.getUnitOfMeasure()).estimatedCost(i.getEstimatedCost())
                        .notes(i.getNotes()).build()).collect(Collectors.toList())).build();
    }

    private QuotationDto toQuotationDto(Quotation q) {
        return QuotationDto.builder().id(q.getId()).organisationId(q.getOrganisationId())
                .requisitionId(q.getRequisitionId()).quotationNumber(q.getQuotationNumber())
                .title(q.getTitle()).status(q.getStatus()).currentVersion(q.getCurrentVersion())
                .subtotal(q.getSubtotal()).taxRate(q.getTaxRate()).taxAmount(q.getTaxAmount())
                .totalAmount(q.getTotalAmount()).currency(q.getCurrency()).validUntil(q.getValidUntil())
                .notes(q.getNotes())
                .items(q.getItems().stream().map(qi -> QuotationItemDto.builder().id(qi.getId())
                        .lineNumber(qi.getLineNumber()).partNumber(qi.getPartNumber())
                        .description(qi.getDescription()).quantity(qi.getQuantity())
                        .unitPrice(qi.getUnitPrice()).discountPct(qi.getDiscountPct())
                        .lineTotal(qi.getLineTotal()).notes(qi.getNotes()).build()).collect(Collectors.toList())).build();
    }

    private OrderDto toOrderDto(AvOrder o) {
        return OrderDto.builder().id(o.getId()).organisationId(o.getOrganisationId())
                .quotationId(o.getQuotationId()).repairJobId(o.getRepairJobId())
                .orderNumber(o.getOrderNumber()).status(o.getStatus()).orderDate(o.getOrderDate())
                .subtotal(o.getSubtotal()).taxAmount(o.getTaxAmount()).totalAmount(o.getTotalAmount())
                .currency(o.getCurrency()).expectedDelivery(o.getExpectedDelivery()).notes(o.getNotes())
                .items(o.getItems().stream().map(oi -> OrderItemDto.builder().id(oi.getId())
                        .lineNumber(oi.getLineNumber()).partNumber(oi.getPartNumber())
                        .description(oi.getDescription()).quantity(oi.getQuantity())
                        .unitPrice(oi.getUnitPrice()).lineTotal(oi.getLineTotal())
                        .deliveredQty(oi.getDeliveredQty()).build()).collect(Collectors.toList())).build();
    }
}
