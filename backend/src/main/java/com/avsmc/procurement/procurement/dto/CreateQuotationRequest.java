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
public class CreateQuotationRequest {
    @NotNull private UUID requisitionId;
    private String title;
    private BigDecimal taxRate;
    private LocalDate validUntil;
    @NotNull private List<QuotationItemRequest> items;
    private String notes;
}
