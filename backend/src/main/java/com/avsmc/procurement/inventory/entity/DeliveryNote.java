package com.avsmc.procurement.inventory.entity;

import com.avsmc.procurement.shared.entity.OrganisationScoped;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "delivery_notes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeliveryNote extends OrganisationScoped {

    @Column(name = "av_order_id", nullable = false)
    private UUID avOrderId;

    @Column(name = "delivery_number", nullable = false, unique = true, length = 30)
    private String deliveryNumber;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PREPARED";

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "delivered_by", length = 200)
    private String deliveredBy;

    @Column(name = "vehicle_reg", length = 30)
    private String vehicleReg;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
