package com.avsmc.procurement.inventory.entity;

import com.avsmc.procurement.shared.entity.OrganisationScoped;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "stock_movements")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockMovement extends OrganisationScoped {
    @Column(name = "inventory_item_id", nullable = false) private UUID inventoryItemId;
    @Column(name = "movement_type", nullable = false, length = 30) private String movementType;
    @Column(nullable = false, precision = 12, scale = 3) private BigDecimal quantity;
    @Column(name = "from_location_id") private UUID fromLocationId;
    @Column(name = "to_location_id") private UUID toLocationId;
    @Column(name = "reference_type", length = 50) private String referenceType;
    @Column(name = "reference_id") private UUID referenceId;
    @Column(name = "performed_by", nullable = false) private UUID performedBy;
    @Column(columnDefinition = "TEXT") private String notes;
}
