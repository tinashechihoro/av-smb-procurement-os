package com.avsmc.procurement.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateInventoryItemRequest {
    @NotBlank private String partNumber;
    @NotBlank private String description;
    private BigDecimal quantityOnHand;
    private BigDecimal reorderLevel;
    private BigDecimal unitCost;
    private BigDecimal sellingPrice;
    private String category;
    private UUID locationId;
}
