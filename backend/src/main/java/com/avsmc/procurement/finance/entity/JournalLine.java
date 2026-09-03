package com.avsmc.procurement.finance.entity;

import com.avsmc.procurement.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "journal_lines")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JournalLine extends BaseEntity {

    @Column(name = "journal_id", nullable = false)
    private UUID journalId;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(length = 500)
    private String description;

    @Column(name = "debit_amount", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal debitAmount = BigDecimal.ZERO;

    @Column(name = "credit_amount", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal creditAmount = BigDecimal.ZERO;
}
