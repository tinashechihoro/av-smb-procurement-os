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
public class CashAccountDto {
    private UUID id;
    private String accountName;
    private String accountType;
    private String accountNumber;
    private String bankName;
    private String currency;
    private BigDecimal balance;
}
