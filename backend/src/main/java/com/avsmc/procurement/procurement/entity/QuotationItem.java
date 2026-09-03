package com.avsmc.procurement.procurement.entity;

import com.avsmc.procurement.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "quotation_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QuotationItem extends BaseEntity {

    @Column(name = "quotation_id", nullable = false)
    private UUID quotationId;

    @Column(name = "version_id")
    private UUID versionId;

    @Column(name = "requisition_item_id")
    private UUID requisitionItemId;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "part_number", length = 80)
    private String partNumber;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "discount_pct", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPct = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal lineTotal = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
