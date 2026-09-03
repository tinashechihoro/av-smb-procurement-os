package com.avsmc.procurement.identity.entity;

import com.avsmc.procurement.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "organisations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Organisation extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 20, unique = true)
    private String code;

    @Column(name = "org_type", nullable = false, length = 20)
    private String orgType;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "fiscal_year_start")
    private java.time.LocalDate fiscalYearStart;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
