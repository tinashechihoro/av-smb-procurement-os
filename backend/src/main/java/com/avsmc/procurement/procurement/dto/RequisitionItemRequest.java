package com.avsmc.procurement.procurement.dto;

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
public class RequisitionItemRequest {
    private String partNumber;
    @NotBlank private String description;
    private BigDecimal quantity;
    private String unitOfMeasure;
    private BigDecimal estimatedCost;
    private String notes;
}
