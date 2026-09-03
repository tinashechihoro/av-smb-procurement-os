package com.avsmc.procurement.purchasing.entity;

import com.avsmc.procurement.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "supplier_po_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierPoItem extends BaseEntity {

    @Column(name = "po_id", nullable = false)
    private UUID poId;

    @Column(name = "av_order_item_id")
    private UUID avOrderItemId;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "freight_alloc", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal freightAlloc = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "received_qty", nullable = false, precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal receivedQty = BigDecimal.ZERO;
}
