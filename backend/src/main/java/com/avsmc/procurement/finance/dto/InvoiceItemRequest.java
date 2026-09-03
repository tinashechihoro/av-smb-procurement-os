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
public class InvoiceItemRequest {
    private UUID orderItemId;
    @NotBlank private String description;
    @NotNull private BigDecimal quantity;
    @NotNull private BigDecimal unitPrice;
}
