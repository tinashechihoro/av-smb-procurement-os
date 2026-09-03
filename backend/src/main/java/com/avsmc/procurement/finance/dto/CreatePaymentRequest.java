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
public class CreatePaymentRequest {
    private UUID invoiceId;
    @NotBlank private String paymentType;
    @NotNull private BigDecimal amount;
    @NotBlank private String paymentMethod;
    private String reference;
    private UUID cashAccountId;
    private LocalDate paymentDate;
    private String notes;
}
