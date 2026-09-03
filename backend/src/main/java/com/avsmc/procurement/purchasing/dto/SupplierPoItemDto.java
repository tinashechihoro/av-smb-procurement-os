package com.avsmc.procurement.purchasing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data @Builder
public class SupplierPoItemDto {
    private UUID id;
    private Integer lineNumber;
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal freightAlloc;
    private BigDecimal lineTotal;
    private BigDecimal receivedQty;
}
