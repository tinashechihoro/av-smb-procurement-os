package com.avsmc.procurement.workshop.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data @Builder
public class RepairOperationDto {
    private UUID id;
    private UUID repairJobId;
    private String operationName;
    private String operationType;
    private String assignedTo;
    private BigDecimal bookedHours;
    private BigDecimal actualHours;
    private Integer sequenceOrder;
    private String status;
    private String notes;
    private Instant startedAt;
    private Instant completedAt;
}
