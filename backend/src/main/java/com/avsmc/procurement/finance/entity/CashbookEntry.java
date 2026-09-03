package com.avsmc.procurement.finance.entity;

import com.avsmc.procurement.shared.entity.OrganisationScoped;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cashbook_entries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CashbookEntry extends OrganisationScoped {

    @Column(name = "cash_account_id", nullable = false)
    private UUID cashAccountId;

    @Column(name = "journal_id")
    private UUID journalId;

    @Column(name = "entry_type", nullable = false, length = 10)
    private String entryType;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(length = 200)
    private String counterparty;

    @Column(columnDefinition = "TEXT")
    private String narration;

    @Column(length = 100)
    private String reference;

    @Column(name = "source_type", length = 50)
    private String sourceType;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(name = "running_balance", precision = 18, scale = 2)
    private BigDecimal runningBalance;

    @Column(name = "reconciliation_status", nullable = false, length = 20)
    @Builder.Default
    private String reconciliationStatus = "UNRECONCILED";

    @Column(name = "reconciled_by")
    private UUID reconciledBy;

    @Column(name = "reconciled_at")
    private Instant reconciledAt;

    @Column(name = "entry_date", nullable = false)
    @Builder.Default
    private LocalDate entryDate = LocalDate.now();

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;
}
