package com.avsmc.procurement.workshop.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateJobStatusRequest {
    @NotBlank private String status;
    private String notes;
}
