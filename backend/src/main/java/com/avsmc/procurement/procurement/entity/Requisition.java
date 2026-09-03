package com.avsmc.procurement.procurement.entity;

import com.avsmc.procurement.shared.entity.OrganisationScoped;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "requisitions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Requisition extends OrganisationScoped {

    @Column(name = "repair_job_id")
    private UUID repairJobId;

    @Column(name = "requisition_number", nullable = false, unique = true, length = 30)
    private String requisitionNumber;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "DRAFT";

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String priority = "NORMAL";

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Column(name = "total_estimate", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalEstimate = BigDecimal.ZERO;

    @Column(name = "amendment_number", nullable = false)
    @Builder.Default
    private Integer amendmentNumber = 0;

    @Column(name = "parent_requisition_id")
    private UUID parentRequisitionId;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @OneToMany(mappedBy = "requisitionId", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RequisitionItem> items = new ArrayList<>();
}
