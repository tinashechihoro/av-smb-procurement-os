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
public class CreateRequisitionRequest {
    private UUID repairJobId;
    @NotBlank private String title;
    private String priority;
    @NotNull private List<RequisitionItemRequest> items;
    private String notes;
}
