package com.avsmc.procurement.workshop.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateVehicleRequest {
    @NotBlank private String registration;
    @NotBlank private String make;
    @NotBlank private String model;
    private String vin;
    private Integer year;
    private String color;
    private String engineNumber;
    private Integer mileage;
    private String fuelType;
    private String vehicleType;
    private UUID customerId;
    private UUID insurerId;
    private String notes;
}
