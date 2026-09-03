package com.avsmc.procurement.procurement.entity;

import com.avsmc.procurement.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "av_order_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AvOrderItem extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "quotation_item_id")
    private UUID quotationItemId;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "part_number", length = 80)
    private String partNumber;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "delivered_qty", nullable = false, precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal deliveredQty = BigDecimal.ZERO;
}
