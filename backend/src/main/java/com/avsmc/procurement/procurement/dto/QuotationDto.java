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

@Data @Builder
public class QuotationDto {
    private UUID id;
    private UUID organisationId;
    private UUID requisitionId;
    private String quotationNumber;
    private String title;
    private String status;
    private Integer currentVersion;
    private BigDecimal subtotal;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String currency;
    private LocalDate validUntil;
    private String notes;
    private List<QuotationItemDto> items;
}
