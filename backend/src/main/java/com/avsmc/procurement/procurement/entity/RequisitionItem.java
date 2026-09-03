package com.avsmc.procurement.procurement.entity;

import com.avsmc.procurement.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "requisition_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RequisitionItem extends BaseEntity {

    @Column(name = "requisition_id", nullable = false)
    private UUID requisitionId;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "part_number", length = 80)
    private String partNumber;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit_of_measure", nullable = false, length = 20)
    @Builder.Default
    private String unitOfMeasure = "EA";

    @Column(name = "estimated_cost", precision = 12, scale = 2)
    private BigDecimal estimatedCost;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
