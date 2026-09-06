package com.avsmc.procurement.workshop.entity;

import com.avsmc.procurement.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "repair_stage_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RepairStageHistory extends BaseEntity {
    @Column(name = "repair_job_id", nullable = false) private UUID repairJobId;
    @Column(name = "from_stage", length = 30) private String fromStage;
    @Column(name = "to_stage", nullable = false, length = 30) private String toStage;
    @Column(name = "changed_by", nullable = false) private UUID changedBy;
    @Column(columnDefinition = "TEXT") private String notes;
}
