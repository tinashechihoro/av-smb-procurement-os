package com.avsmc.procurement.identity.entity;

import com.avsmc.procurement.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "approval_limits")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalLimit extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "approval_type", nullable = false, length = 50)
    private String approvalType;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "single_limit", nullable = false, precision = 18, scale = 2)
    private BigDecimal singleLimit = BigDecimal.ZERO;

    @Column(name = "cumulative_limit", precision = 18, scale = 2)
    private BigDecimal cumulativeLimit;

    @Column(name = "requires_second_approval", nullable = false)
    private Boolean requiresSecondApproval = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
