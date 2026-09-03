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
public class JournalLineRequest {
    @NotNull private UUID accountId;
    private String description;
    @NotNull private BigDecimal debitAmount;
    @NotNull private BigDecimal creditAmount;
}
