package com.avsmc.procurement.workshop.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data @Builder
public class DamageAssessmentDto {
    private UUID id;
    private UUID repairJobId;
    private UUID assessedBy;
    private String zone;
    private String severity;
    private String description;
    private Map<String, Object> partsAffected;
    private String repairMethod;
    private BigDecimal estimatedCost;
}
