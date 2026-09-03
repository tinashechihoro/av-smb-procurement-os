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
public class InventoryItemDto {
    private UUID id;
    private UUID organisationId;
    private UUID locationId;
    private String partNumber;
    private String description;
    private BigDecimal quantityOnHand;
    private BigDecimal quantityReserved;
    private BigDecimal quantityAvailable;
    private BigDecimal reorderLevel;
    private BigDecimal unitCost;
    private BigDecimal sellingPrice;
    private String category;
    private String status;
}
