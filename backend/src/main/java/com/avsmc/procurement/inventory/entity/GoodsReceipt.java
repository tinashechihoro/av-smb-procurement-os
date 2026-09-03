package com.avsmc.procurement.inventory.entity;

import com.avsmc.procurement.shared.entity.OrganisationScoped;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "goods_receipts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GoodsReceipt extends OrganisationScoped {

    @Column(name = "supplier_po_id")
    private UUID supplierPoId;

    @Column(name = "av_order_id")
    private UUID avOrderId;

    @Column(name = "receipt_number", nullable = false, unique = true, length = 30)
    private String receiptNumber;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT";

    @Column(name = "received_by", nullable = false)
    private UUID receivedBy;

    @Column(name = "received_at", nullable = false)
    @Builder.Default
    private Instant receivedAt = Instant.now();

    @Column(columnDefinition = "TEXT")
    private String notes;
}
