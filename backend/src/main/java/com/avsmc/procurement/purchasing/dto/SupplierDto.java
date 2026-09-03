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
public class SupplierDto {
    private UUID id;
    private String name;
    private String contactPerson;
    private String email;
    private String phone;
    private String address;
    private String taxNumber;
    private Integer paymentTerms;
    private String currency;
    private Boolean isActive;
    private BigDecimal rating;
    private String notes;
}
