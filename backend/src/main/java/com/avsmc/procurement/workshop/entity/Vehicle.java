package com.avsmc.procurement.workshop.entity;

import com.avsmc.procurement.shared.entity.OrganisationScoped;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "vehicles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Vehicle extends OrganisationScoped {

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "insurer_id")
    private UUID insurerId;

    @Column(nullable = false, length = 30)
    private String registration;

    @Column(length = 30)
    private String vin;

    @Column(nullable = false, length = 100)
    private String make;

    @Column(nullable = false, length = 100)
    private String model;

    @Column
    private Integer year;

    @Column(length = 50)
    private String color;

    @Column(name = "engine_number", length = 50)
    private String engineNumber;

    @Column
    private Integer mileage;

    @Column(name = "fuel_type", length = 20)
    private String fuelType;

    @Column(name = "vehicle_type", length = 30)
    private String vehicleType;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "IN_WORKSHOP";

    @Column(columnDefinition = "TEXT")
    private String notes;
}
