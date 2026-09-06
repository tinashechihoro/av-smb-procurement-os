package com.avsmc.procurement.finance.entity;

import com.avsmc.procurement.shared.entity.OrganisationScoped;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "debtor_ledger")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DebtorLedger extends OrganisationScoped {
    @Column(name = "customer_id", nullable = false) private UUID customerId;
    @Column(name = "invoice_id") private UUID invoiceId;
    @Column(name = "payment_id") private UUID paymentId;
    @Column(name = "entry_type", nullable = false, length = 10) private String entryType;
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal amount;
    @Column(name = "balance_after", precision = 18, scale = 2) private BigDecimal balanceAfter;
    @Column(name = "entry_date", nullable = false) private LocalDate entryDate;
    @Column(columnDefinition = "TEXT") private String narration;
}
