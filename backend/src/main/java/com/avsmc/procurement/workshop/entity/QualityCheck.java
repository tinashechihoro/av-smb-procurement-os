package com.avsmc.procurement.workshop.entity;

import com.avsmc.procurement.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "quality_checks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QualityCheck extends BaseEntity {

    @Column(name = "repair_job_id", nullable = false)
    private UUID repairJobId;

    @Column(name = "operation_id")
    private UUID operationId;

    @Column(name = "checked_by", nullable = false)
    private UUID checkedBy;

    @Column(name = "check_type", nullable = false, length = 50)
    private String checkType;

    @Column(nullable = false)
    @Builder.Default
    private Boolean passed = false;

    @Column(columnDefinition = "TEXT")
    private String comments;
}
