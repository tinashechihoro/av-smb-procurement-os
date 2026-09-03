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
public class JournalDto {
    private UUID id;
    private String journalNumber;
    private String journalType;
    private String description;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private String status;
    private UUID postedBy;
    private Instant postedAt;
    private List<JournalLineDto> lines;
}
