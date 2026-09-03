package com.avsmc.procurement.procurement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data @Builder
public class OrderDto {
    private UUID id;
    private UUID organisationId;
    private UUID quotationId;
    private UUID repairJobId;
    private String orderNumber;
    private String status;
    private LocalDate orderDate;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String currency;
    private LocalDate expectedDelivery;
    private String notes;
    private List<OrderItemDto> items;
}
