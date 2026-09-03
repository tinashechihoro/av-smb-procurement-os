package com.avsmc.procurement.purchasing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class SupplierPoItemRequest {
    private UUID avOrderItemId;
    @NotBlank private String description;
    @NotNull private BigDecimal quantity;
    @NotNull private BigDecimal unitCost;
    private BigDecimal freightAlloc;
}
