package com.avsmc.procurement.workshop.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data @Builder
public class VehicleDto {
    private UUID id;
    private UUID organisationId;
    private UUID customerId;
    private UUID insurerId;
    private String registration;
    private String vin;
    private String make;
    private String model;
    private Integer year;
    private String color;
    private String engineNumber;
    private Integer mileage;
    private String fuelType;
    private String vehicleType;
    private String status;
    private String notes;
}
