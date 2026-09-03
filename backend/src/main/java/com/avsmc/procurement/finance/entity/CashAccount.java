package com.avsmc.procurement.finance.entity;

import com.avsmc.procurement.shared.entity.OrganisationScoped;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "cash_accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CashAccount extends OrganisationScoped {

    @Column(name = "account_name", nullable = false, length = 200)
    private String accountName;

    @Column(name = "account_type", nullable = false, length = 20)
    private String accountType;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "bank_name", length = 200)
    private String bankName;

    @Column(name = "branch_name", length = 200)
    private String branchName;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "gl_account_id")
    private UUID glAccountId;

    @Column(nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
