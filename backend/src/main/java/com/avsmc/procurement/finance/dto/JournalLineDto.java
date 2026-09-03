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
public class JournalLineDto {
    private UUID id;
    private Integer lineNumber;
    private UUID accountId;
    private String accountCode;
    private String accountName;
    private String description;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
}
