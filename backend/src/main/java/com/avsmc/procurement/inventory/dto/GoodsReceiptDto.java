package com.avsmc.procurement.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data @Builder
public class GoodsReceiptDto {
    private UUID id;
    private UUID supplierPoId;
    private UUID avOrderId;
    private String receiptNumber;
    private String status;
    private UUID receivedBy;
    private Instant receivedAt;
    private String notes;
}
