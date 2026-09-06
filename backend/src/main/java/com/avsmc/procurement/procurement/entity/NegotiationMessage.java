package com.avsmc.procurement.procurement.entity;

import com.avsmc.procurement.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "negotiation_messages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NegotiationMessage extends BaseEntity {
    @Column(name = "quotation_id", nullable = false) private UUID quotationId;
    @Column(name = "sender_id", nullable = false) private UUID senderId;
    @Column(nullable = false, columnDefinition = "TEXT") private String message;
    @Column(name = "proposed_amount", precision = 14, scale = 2) private BigDecimal proposedAmount;
    @Column(name = "is_av_message", nullable = false) @Builder.Default private Boolean isAvMessage = true;
}
