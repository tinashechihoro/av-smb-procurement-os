package com.avsmc.procurement.inventory.entity;

import com.avsmc.procurement.shared.entity.OrganisationScoped;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "inventory_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryItem extends OrganisationScoped {

    @Column(name = "location_id")
    private UUID locationId;

    @Column(name = "part_number", nullable = false, length = 80)
    private String partNumber;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "quantity_on_hand", nullable = false, precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal quantityOnHand = BigDecimal.ZERO;

    @Column(name = "quantity_reserved", nullable = false, precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal quantityReserved = BigDecimal.ZERO;

    @Column(name = "reorder_level", precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal reorderLevel = BigDecimal.ZERO;

    @Column(name = "unit_cost", precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "selling_price", precision = 12, scale = 2)
    private BigDecimal sellingPrice;

    @Column(length = 50)
    private String category;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";
}
