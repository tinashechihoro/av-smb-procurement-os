package com.avsmc.procurement.workshop.entity;

import com.avsmc.procurement.shared.entity.OrganisationScoped;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Customer extends OrganisationScoped {
    @Column(nullable = false, length = 200) private String name;
    @Column(length = 255) private String email;
    @Column(length = 30) private String phone;
    @Column(columnDefinition = "TEXT") private String address;
    @Column(name = "tax_number", length = 50) private String taxNumber;
    @Column(name = "is_active", nullable = false) @Builder.Default private Boolean isActive = true;
}
