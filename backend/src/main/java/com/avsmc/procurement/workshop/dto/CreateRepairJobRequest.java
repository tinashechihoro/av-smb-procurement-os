package com.avsmc.procurement.workshop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateRepairJobRequest {
    @NotNull private UUID vehicleId;
    @NotBlank private String title;
    private String description;
    private String jobType;
    private String priority;
    private String assignedTechnician;
    private String bayNumber;
    private BigDecimal bookedHours;
    private BigDecimal labourRate;
    private String insurerClaimNumber;
    private BigDecimal excessAmount;
    private String notes;
}
