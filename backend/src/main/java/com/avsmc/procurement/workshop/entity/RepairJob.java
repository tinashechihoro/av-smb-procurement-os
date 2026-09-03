package com.avsmc.procurement.workshop.entity;

import com.avsmc.procurement.shared.entity.OrganisationScoped;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "repair_jobs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RepairJob extends OrganisationScoped {

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", insertable = false, updatable = false)
    private Vehicle vehicle;

    @Column(name = "job_number", nullable = false, unique = true, length = 30)
    private String jobNumber;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "job_type", nullable = false, length = 30)
    @Builder.Default
    private String jobType = "COLLISION";

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "ASSESSMENT";

    @Column(name = "repair_stage", nullable = false, length = 30)
    @Builder.Default
    private String repairStage = "INITIAL_ASSESSMENT";

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String priority = "NORMAL";

    @Column(name = "assigned_technician", length = 200)
    private String assignedTechnician;

    @Column(name = "bay_number", length = 20)
    private String bayNumber;

    @Column(name = "booked_hours", precision = 8, scale = 2)
    private BigDecimal bookedHours;

    @Column(name = "actual_hours", precision = 8, scale = 2)
    private BigDecimal actualHours;

    @Column(name = "labour_rate", precision = 10, scale = 2)
    private BigDecimal labourRate;

    @Column(name = "insurer_claim_number", length = 80)
    private String insurerClaimNumber;

    @Column(name = "insurer_authorised", nullable = false)
    @Builder.Default
    private Boolean insurerAuthorised = false;

    @Column(name = "excess_amount", precision = 12, scale = 2)
    private BigDecimal excessAmount;

    @Column(name = "supplement_amount", precision = 12, scale = 2)
    private BigDecimal supplementAmount;

    @Column(name = "estimated_total", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal estimatedTotal = BigDecimal.ZERO;

    @Column(name = "actual_total", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal actualTotal = BigDecimal.ZERO;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "handed_over_at")
    private Instant handedOverAt;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
