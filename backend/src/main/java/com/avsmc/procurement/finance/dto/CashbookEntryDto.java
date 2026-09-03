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
public class CashbookEntryDto {
    private UUID id;
    private UUID cashAccountId;
    private String cashAccountName;
    private UUID journalId;
    private String entryType;
    private BigDecimal amount;
    private String counterparty;
    private String narration;
    private String reference;
    private BigDecimal runningBalance;
    private String reconciliationStatus;
    private LocalDate entryDate;
}
