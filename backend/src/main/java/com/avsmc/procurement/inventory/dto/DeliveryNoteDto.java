package com.avsmc.procurement.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data @Builder
public class DeliveryNoteDto {
    private UUID id;
    private UUID avOrderId;
    private String deliveryNumber;
    private String status;
    private LocalDate deliveryDate;
    private String deliveredBy;
    private String vehicleReg;
    private String notes;
}
