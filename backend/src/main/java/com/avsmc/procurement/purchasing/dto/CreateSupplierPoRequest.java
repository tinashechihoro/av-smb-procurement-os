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
public class CreateSupplierPoRequest {
    @NotNull private UUID supplierId;
    private UUID avOrderId;
    private LocalDate expectedDelivery;
    @NotNull private List<SupplierPoItemRequest> items;
    private String notes;
}
