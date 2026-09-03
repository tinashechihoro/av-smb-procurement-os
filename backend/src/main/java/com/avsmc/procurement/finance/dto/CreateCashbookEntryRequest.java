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
public class CreateCashbookEntryRequest {
    @NotNull private UUID cashAccountId;
    @NotBlank private String entryType;
    @NotNull private BigDecimal amount;
    private String counterparty;
    private String narration;
    private String reference;
    private LocalDate entryDate;
}
