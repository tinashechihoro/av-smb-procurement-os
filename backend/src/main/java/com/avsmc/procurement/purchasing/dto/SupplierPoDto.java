package com.avsmc.procurement.purchasing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data @Builder
public class SupplierPoDto {
    private UUID id;
    private UUID supplierId;
    private String supplierName;
    private UUID avOrderId;
    private String poNumber;
    private String status;
    private LocalDate orderDate;
    private BigDecimal subtotal;
    private BigDecimal freightTotal;
    private BigDecimal totalAmount;
    private String currency;
    private LocalDate expectedDelivery;
    private String notes;
    private List<SupplierPoItemDto> items;
}
