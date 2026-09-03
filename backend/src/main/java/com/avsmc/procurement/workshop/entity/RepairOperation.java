package com.avsmc.procurement.workshop.entity;

import com.avsmc.procurement.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "repair_operations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RepairOperation extends BaseEntity {

    @Column(name = "repair_job_id", nullable = false)
    private UUID repairJobId;

    @Column(name = "operation_name", nullable = false, length = 200)
    private String operationName;

    @Column(name = "operation_type", nullable = false, length = 50)
    private String operationType;

    @Column(name = "assigned_to", length = 200)
    private String assignedTo;

    @Column(name = "booked_hours", precision = 8, scale = 2)
    private BigDecimal bookedHours;

    @Column(name = "actual_hours", precision = 8, scale = 2)
    private BigDecimal actualHours;

    @Column(name = "sequence_order", nullable = false)
    @Builder.Default
    private Integer sequenceOrder = 0;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
