package com.avsmc.procurement.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data @Builder
public class InvoiceDto {
    private UUID id;
    private UUID avOrderId;
    private UUID repairJobId;
    private String invoiceNumber;
    private String invoiceType;
    private String status;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private String fromEntity;
    private String toEntity;
    private BigDecimal subtotal;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal amountPaid;
    private BigDecimal balanceDue;
    private String currency;
    private String notes;
    private List<InvoiceItemDto> items;
}
