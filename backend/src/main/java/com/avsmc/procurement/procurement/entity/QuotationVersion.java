package com.avsmc.procurement.procurement.entity;

import com.avsmc.procurement.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "quotation_versions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QuotationVersion extends BaseEntity {
    @Column(name = "quotation_id", nullable = false) private UUID quotationId;
    @Column(name = "version_number", nullable = false) private Integer versionNumber;
    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2) @Builder.Default private BigDecimal subtotal = BigDecimal.ZERO;
    @Column(name = "tax_amount", nullable = false, precision = 14, scale = 2) @Builder.Default private BigDecimal taxAmount = BigDecimal.ZERO;
    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2) @Builder.Default private BigDecimal totalAmount = BigDecimal.ZERO;
    @Column(name = "change_summary", columnDefinition = "TEXT") private String changeSummary;
    @Column(name = "created_by") private UUID createdBy;
}
