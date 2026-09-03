package com.avsmc.procurement.purchasing.entity;

import com.avsmc.procurement.shared.entity.OrganisationScoped;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "supplier_purchase_orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierPurchaseOrder extends OrganisationScoped {

    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", insertable = false, updatable = false)
    private Supplier supplier;

    @Column(name = "av_order_id")
    private UUID avOrderId;

    @Column(name = "po_number", nullable = false, unique = true, length = 30)
    private String poNumber;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "DRAFT";

    @Column(name = "order_date", nullable = false)
    @Builder.Default
    private LocalDate orderDate = LocalDate.now();

    @Column(nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "freight_total", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal freightTotal = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "expected_delivery")
    private LocalDate expectedDelivery;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "poId", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SupplierPoItem> items = new ArrayList<>();
}
