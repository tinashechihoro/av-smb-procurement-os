package com.avsmc.procurement.procurement.entity;

import com.avsmc.procurement.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "clarification_threads")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClarificationThread extends BaseEntity {
    @Column(name = "requisition_id", nullable = false) private UUID requisitionId;
    @Column(name = "raised_by", nullable = false) private UUID raisedBy;
    @Column(nullable = false, length = 300) private String subject;
    @Column(nullable = false, length = 20) @Builder.Default private String status = "OPEN";
}
