package com.avsmc.procurement.workshop.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data @Builder
public class RepairJobDto {
    private UUID id;
    private UUID organisationId;
    private UUID vehicleId;
    private String vehicleRegistration;
    private String vehicleDescription;
    private String jobNumber;
    private String title;
    private String description;
    private String jobType;
    private String status;
    private String repairStage;
    private String priority;
    private String assignedTechnician;
    private String bayNumber;
    private BigDecimal bookedHours;
    private BigDecimal actualHours;
    private BigDecimal labourRate;
    private String insurerClaimNumber;
    private Boolean insurerAuthorised;
    private BigDecimal excessAmount;
    private BigDecimal supplementAmount;
    private BigDecimal estimatedTotal;
    private BigDecimal actualTotal;
    private Instant startedAt;
    private Instant completedAt;
    private String notes;
    private List<DamageAssessmentDto> damageAssessments;
    private List<RepairOperationDto> operations;
}
