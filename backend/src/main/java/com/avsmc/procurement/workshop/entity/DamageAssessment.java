package com.avsmc.procurement.workshop.entity;

import com.avsmc.procurement.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "damage_assessments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DamageAssessment extends BaseEntity {

    @Column(name = "repair_job_id", nullable = false)
    private UUID repairJobId;

    @Column(name = "assessed_by", nullable = false)
    private UUID assessedBy;

    @Column(nullable = false, length = 50)
    private String zone;

    @Column(nullable = false, length = 10)
    private String severity;

    @Column(columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parts_affected", columnDefinition = "jsonb")
    private Map<String, Object> partsAffected;

    @Column(name = "repair_method", length = 100)
    private String repairMethod;

    @Column(name = "estimated_cost", precision = 12, scale = 2)
    private BigDecimal estimatedCost;
}
