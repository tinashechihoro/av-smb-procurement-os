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

@Data
public class CreateInvoiceRequest {
    private UUID avOrderId;
    private UUID repairJobId;
    @NotBlank private String fromEntity;
    @NotBlank private String toEntity;
    private BigDecimal taxRate;
    private LocalDate dueDate;
    @NotNull private List<InvoiceItemRequest> items;
    private String notes;
}
