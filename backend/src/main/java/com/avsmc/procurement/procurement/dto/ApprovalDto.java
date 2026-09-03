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
public class ApprovalDto {
    private UUID id;
    private String entityType;
    private UUID entityId;
    private String approvalType;
    private String status;
    private UUID requestedBy;
    private UUID approvedBy;
    private BigDecimal amount;
    private String comments;
    private Instant approvedAt;
}
