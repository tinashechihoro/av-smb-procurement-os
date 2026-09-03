package com.avsmc.procurement.procurement.dto;

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
public class QuotationItemRequest {
    private UUID requisitionItemId;
    private String partNumber;
    @NotBlank private String description;
    private BigDecimal quantity;
    @NotNull private BigDecimal unitPrice;
    private BigDecimal discountPct;
    private String notes;
}
