package com.avsmc.procurement.procurement.entity;

import com.avsmc.procurement.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_amendments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderAmendment extends BaseEntity {
    @Column(name = "order_id", nullable = false) private UUID orderId;
    @Column(name = "amendment_number", nullable = false) @Builder.Default private Integer amendmentNumber = 1;
    @Column(nullable = false, columnDefinition = "TEXT") private String reason;
    @Column(nullable = false, length = 20) @Builder.Default private String status = "PENDING";
    @Column(name = "requested_by", nullable = false) private UUID requestedBy;
    @Column(name = "approved_by") private UUID approvedBy;
    @Column(name = "amount_change", precision = 14, scale = 2) private BigDecimal amountChange;
    @Column(name = "approved_at") private Instant approvedAt;
}
