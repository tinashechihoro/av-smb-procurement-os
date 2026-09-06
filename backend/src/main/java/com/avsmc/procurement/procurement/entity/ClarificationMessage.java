package com.avsmc.procurement.procurement.entity;

import com.avsmc.procurement.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "clarification_messages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClarificationMessage extends BaseEntity {
    @Column(name = "thread_id", nullable = false) private UUID threadId;
    @Column(name = "sender_id", nullable = false) private UUID senderId;
    @Column(nullable = false, columnDefinition = "TEXT") private String message;
}
