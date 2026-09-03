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
public class PaymentDto {
    private UUID id;
    private UUID invoiceId;
    private String paymentNumber;
    private String paymentType;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String reference;
    private UUID cashAccountId;
    private String status;
    private LocalDate paymentDate;
    private String notes;
}
