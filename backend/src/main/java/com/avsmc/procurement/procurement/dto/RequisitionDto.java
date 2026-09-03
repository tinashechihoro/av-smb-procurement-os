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

@Data @Builder
public class RequisitionDto {
    private UUID id;
    private UUID organisationId;
    private UUID repairJobId;
    private String requisitionNumber;
    private String title;
    private String status;
    private String priority;
    private UUID requestedBy;
    private String requestedByName;
    private BigDecimal totalEstimate;
    private Integer amendmentNumber;
    private String notes;
    private Instant submittedAt;
    private Instant approvedAt;
    private List<RequisitionItemDto> items;
}
