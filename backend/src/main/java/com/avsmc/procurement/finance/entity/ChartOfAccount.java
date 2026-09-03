package com.avsmc.procurement.finance.entity;

import com.avsmc.procurement.shared.entity.OrganisationScoped;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "chart_of_accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChartOfAccount extends OrganisationScoped {

    @Column(name = "account_code", nullable = false, length = 20)
    private String accountCode;

    @Column(name = "account_name", nullable = false, length = 200)
    private String accountName;

    @Column(name = "account_type", nullable = false, length = 20)
    private String accountType;

    @Column(name = "sub_type", length = 50)
    private String subType;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "is_postable", nullable = false)
    @Builder.Default
    private Boolean isPostable = true;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "normal_balance", nullable = false, length = 10)
    private String normalBalance;

    @Column(name = "opening_balance", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(name = "current_balance", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal currentBalance = BigDecimal.ZERO;
}
